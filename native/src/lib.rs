use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Arc, Mutex, OnceLock};

use grammers_client::types::{InputReactions, Message as ClientMessage, Peer, Role};
use grammers_client::{Client, InputMessage, InvocationError, SignInError};
use grammers_mtsender::SenderPool;
use grammers_session::storages::SqliteSession;
use jni::objects::{JClass, JString};
use jni::sys::jlong;
use jni::JNIEnv;
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use serde_json::json;
use tokio::runtime::Runtime;

struct NativeClient {
    runtime: Runtime,
    client: Client,
    api_hash: String,
    authentication: Mutex<AuthenticationState>,
    peers: Mutex<HashMap<i64, Peer>>,
    next_peer_handle: AtomicI64,
}

enum AuthenticationState {
    Idle,
    LoginCode(grammers_client::types::LoginToken),
    Password(grammers_client::types::PasswordToken),
}

type Clients = Mutex<HashMap<jlong, Arc<NativeClient>>>;

fn clients() -> &'static Clients {
    static CLIENTS: OnceLock<Clients> = OnceLock::new();
    CLIENTS.get_or_init(|| Mutex::new(HashMap::new()))
}

fn java_string(env: &mut JNIEnv<'_>, value: String) -> jni::sys::jstring {
    env.new_string(value)
        .map_or(std::ptr::null_mut(), |value| value.into_raw())
}

fn read_string(env: &mut JNIEnv<'_>, value: JString<'_>) -> Result<String, String> {
    env.get_string(&value)
        .map(|value| value.to_string_lossy().into_owned())
        .map_err(|error| format!("invalid Java string: {error}"))
}

fn error(value: impl std::fmt::Display) -> String {
    format!("kotlogramme error: {value}")
}

fn get_client(handle: jlong) -> Result<Arc<NativeClient>, String> {
    clients()
        .lock()
        .map_err(|_| "client registry is poisoned".to_owned())?
        .get(&handle)
        .cloned()
        .ok_or_else(|| format!("unknown client handle: {handle}"))
}

fn parse_payload<T: DeserializeOwned>(payload: &str) -> Result<T, String> {
    serde_json::from_str(payload).map_err(|error| format!("invalid request payload: {error}"))
}

fn json_string(value: impl Serialize) -> Result<String, String> {
    serde_json::to_string(&value).map_err(error)
}

fn invocation_error(error: InvocationError) -> String {
    error.to_string()
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct UserDto {
    id: i64,
    username: Option<String>,
    first_name: Option<String>,
    last_name: Option<String>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct PeerDto {
    native_handle: i64,
    id: i64,
    kind: &'static str,
    username: Option<String>,
    name: Option<String>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct MessageDto {
    id: i32,
    text: String,
    outgoing: bool,
    reply_to_message_id: Option<i32>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct DialogDto {
    peer: PeerDto,
    last_message: Option<MessageDto>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ParticipantDto {
    user: UserDto,
    role: &'static str,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct PeerTarget {
    peer_handle: Option<i64>,
    username: Option<String>,
}

#[derive(Deserialize)]
struct PhonePayload {
    phone: String,
}

#[derive(Deserialize)]
struct CodePayload {
    code: String,
}

#[derive(Deserialize)]
struct PasswordPayload {
    password: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SendMessagePayload {
    #[serde(flatten)]
    peer: PeerTarget,
    text: String,
    reply_to_message_id: Option<i32>,
    silent: Option<bool>,
    link_preview: Option<bool>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct EditMessagePayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
    text: String,
    link_preview: Option<bool>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct MessageIdsPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_ids: Vec<i32>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct HistoryPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    limit: Option<usize>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SearchMessagesPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    query: String,
    limit: Option<usize>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ForwardMessagesPayload {
    destination: PeerTarget,
    source: PeerTarget,
    message_ids: Vec<i32>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct MessageIdPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ReactionPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
    emoji: Option<String>,
    remove: Option<bool>,
    big: Option<bool>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ParticipantPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    limit: Option<usize>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct KickParticipantPayload {
    chat: PeerTarget,
    user: PeerTarget,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct LimitPayload {
    limit: Option<usize>,
}

fn user_dto(user: &grammers_client::types::User) -> UserDto {
    UserDto {
        id: user.bare_id(),
        username: user.username().map(ToOwned::to_owned),
        first_name: user.first_name().map(ToOwned::to_owned),
        last_name: user.last_name().map(ToOwned::to_owned),
    }
}

fn message_dto(message: &ClientMessage) -> MessageDto {
    MessageDto {
        id: message.id(),
        text: message.text().to_owned(),
        outgoing: message.outgoing(),
        reply_to_message_id: message.reply_to_message_id(),
    }
}

fn role_name(role: &Role) -> &'static str {
    match role {
        Role::User(_) => "member",
        Role::Creator(_) => "creator",
        Role::Admin(_) => "admin",
        Role::Banned(_) => "banned",
        Role::Left(_) => "left",
        _ => "unknown",
    }
}

fn peer_dto(native: &NativeClient, peer: &Peer) -> Result<PeerDto, String> {
    let native_handle = native.next_peer_handle.fetch_add(1, Ordering::Relaxed);
    native
        .peers
        .lock()
        .map_err(|_| "peer registry is poisoned".to_owned())?
        .insert(native_handle, peer.clone());

    let kind = match peer {
        Peer::User(_) => "user",
        Peer::Group(_) => "group",
        Peer::Channel(_) => "channel",
    };
    Ok(PeerDto {
        native_handle,
        id: peer.id().bot_api_dialog_id(),
        kind,
        username: peer.username().map(ToOwned::to_owned),
        name: peer.name().map(ToOwned::to_owned),
    })
}

async fn resolve_peer(native: &NativeClient, target: &PeerTarget) -> Result<Peer, String> {
    if let Some(handle) = target.peer_handle {
        return native
            .peers
            .lock()
            .map_err(|_| "peer registry is poisoned".to_owned())?
            .get(&handle)
            .cloned()
            .ok_or_else(|| format!("unknown peer handle: {handle}"));
    }

    let username = target
        .username
        .as_deref()
        .ok_or_else(|| "a peerHandle or username is required".to_owned())?
        .trim_start_matches('@');
    native
        .client
        .resolve_username(username)
        .await
        .map_err(invocation_error)?
        .ok_or_else(|| format!("username not found: {username}"))
}

fn request(native: &NativeClient, operation: &str, payload: &str) -> Result<String, String> {
    match operation {
        "requestLoginCode" => {
            let PhonePayload { phone } = parse_payload(payload)?;
            let token = native
                .runtime
                .block_on(native.client.request_login_code(&phone, &native.api_hash))
                .map_err(invocation_error)?;
            *native
                .authentication
                .lock()
                .map_err(|_| "authentication state is poisoned".to_owned())? =
                AuthenticationState::LoginCode(token);
            json_string(json!({ "phone": phone }))
        }
        "signIn" => {
            let CodePayload { code } = parse_payload(payload)?;
            let token = match std::mem::replace(
                &mut *native
                    .authentication
                    .lock()
                    .map_err(|_| "authentication state is poisoned".to_owned())?,
                AuthenticationState::Idle,
            ) {
                AuthenticationState::LoginCode(token) => token,
                _ => return Err("requestLoginCode must be called before signIn".to_owned()),
            };

            match native
                .runtime
                .block_on(native.client.sign_in(&token, &code))
            {
                Ok(user) => json_string(json!({ "status": "authorized", "user": user_dto(&user) })),
                Err(SignInError::PasswordRequired(password_token)) => {
                    let hint = password_token.hint().map(ToOwned::to_owned);
                    *native
                        .authentication
                        .lock()
                        .map_err(|_| "authentication state is poisoned".to_owned())? =
                        AuthenticationState::Password(password_token);
                    json_string(json!({ "status": "passwordRequired", "hint": hint }))
                }
                Err(SignInError::InvalidCode) => {
                    *native
                        .authentication
                        .lock()
                        .map_err(|_| "authentication state is poisoned".to_owned())? =
                        AuthenticationState::LoginCode(token);
                    Err("invalid login code".to_owned())
                }
                Err(error) => Err(error.to_string()),
            }
        }
        "checkPassword" => {
            let PasswordPayload { password } = parse_payload(payload)?;
            let token = match std::mem::replace(
                &mut *native
                    .authentication
                    .lock()
                    .map_err(|_| "authentication state is poisoned".to_owned())?,
                AuthenticationState::Idle,
            ) {
                AuthenticationState::Password(token) => token,
                _ => {
                    return Err(
                        "signIn must report passwordRequired before checkPassword".to_owned()
                    )
                }
            };
            let user = native
                .runtime
                .block_on(native.client.check_password(token, password.as_bytes()))
                .map_err(|error| error.to_string())?;
            json_string(user_dto(&user))
        }
        "getMe" => {
            let user = native
                .runtime
                .block_on(native.client.get_me())
                .map_err(invocation_error)?;
            json_string(user_dto(&user))
        }
        "resolveUsername" => {
            let target: PeerTarget = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &target))?;
            json_string(peer_dto(native, &peer)?)
        }
        "sendMessage" => {
            let data: SendMessagePayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            let message = InputMessage::new()
                .text(data.text)
                .reply_to(data.reply_to_message_id)
                .silent(data.silent.unwrap_or(false))
                .link_preview(data.link_preview.unwrap_or(true));
            let message = native
                .runtime
                .block_on(native.client.send_message(peer, message))
                .map_err(invocation_error)?;
            json_string(message_dto(&message))
        }
        "editMessage" => {
            let data: EditMessagePayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            let message = InputMessage::new()
                .text(data.text)
                .link_preview(data.link_preview.unwrap_or(true));
            native
                .runtime
                .block_on(native.client.edit_message(peer, data.message_id, message))
                .map_err(invocation_error)?;
            json_string(json!({ "ok": true }))
        }
        "deleteMessages" => {
            let data: MessageIdsPayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            let deleted = native
                .runtime
                .block_on(native.client.delete_messages(peer, &data.message_ids))
                .map_err(invocation_error)?;
            json_string(json!({ "deleted": deleted }))
        }
        "markAsRead" => {
            let target: PeerTarget = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &target))?;
            native
                .runtime
                .block_on(native.client.mark_as_read(peer))
                .map_err(invocation_error)?;
            json_string(json!({ "ok": true }))
        }
        "getHistory" => {
            let data: HistoryPayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            let limit = data.limit.unwrap_or(50).clamp(1, 100);
            let messages = native.runtime.block_on(async {
                let mut iterator = native.client.iter_messages(peer).limit(limit);
                let mut result = Vec::new();
                while let Some(message) = iterator.next().await.map_err(invocation_error)? {
                    result.push(message_dto(&message));
                }
                Ok::<_, String>(result)
            })?;
            json_string(messages)
        }
        "getMessages" => {
            let data: MessageIdsPayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            if data.message_ids.len() > 100 {
                return Err("at most 100 message IDs can be requested at once".to_owned());
            }
            let messages = native
                .runtime
                .block_on(native.client.get_messages_by_id(peer, &data.message_ids))
                .map_err(invocation_error)?
                .iter()
                .map(|message| message.as_ref().map(message_dto))
                .collect::<Vec<_>>();
            json_string(messages)
        }
        "searchMessages" => {
            let data: SearchMessagesPayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            let limit = data.limit.unwrap_or(50).clamp(1, 100);
            let messages = native.runtime.block_on(async {
                let mut iterator = native
                    .client
                    .search_messages(peer)
                    .query(&data.query)
                    .limit(limit);
                let mut result = Vec::new();
                while let Some(message) = iterator.next().await.map_err(invocation_error)? {
                    result.push(message_dto(&message));
                }
                Ok::<_, String>(result)
            })?;
            json_string(messages)
        }
        "forwardMessages" => {
            let data: ForwardMessagesPayload = parse_payload(payload)?;
            if data.message_ids.len() > 100 {
                return Err("at most 100 messages can be forwarded at once".to_owned());
            }
            let destination = native
                .runtime
                .block_on(resolve_peer(native, &data.destination))?;
            let source = native
                .runtime
                .block_on(resolve_peer(native, &data.source))?;
            let messages = native
                .runtime
                .block_on(
                    native
                        .client
                        .forward_messages(destination, &data.message_ids, source),
                )
                .map_err(invocation_error)?
                .iter()
                .map(|message| message.as_ref().map(message_dto))
                .collect::<Vec<_>>();
            json_string(messages)
        }
        "getPinnedMessage" => {
            let target: PeerTarget = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &target))?;
            let message = native
                .runtime
                .block_on(native.client.get_pinned_message(peer))
                .map_err(invocation_error)?;
            json_string(message.as_ref().map(message_dto))
        }
        "pinMessage" => {
            let data: MessageIdPayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            native
                .runtime
                .block_on(native.client.pin_message(peer, data.message_id))
                .map_err(invocation_error)?;
            json_string(json!({ "ok": true }))
        }
        "unpinMessage" => {
            let data: MessageIdPayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            native
                .runtime
                .block_on(native.client.unpin_message(peer, data.message_id))
                .map_err(invocation_error)?;
            json_string(json!({ "ok": true }))
        }
        "unpinAllMessages" => {
            let target: PeerTarget = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &target))?;
            native
                .runtime
                .block_on(native.client.unpin_all_messages(peer))
                .map_err(invocation_error)?;
            json_string(json!({ "ok": true }))
        }
        "sendReaction" => {
            let data: ReactionPayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            let reactions = if data.remove.unwrap_or(false) {
                InputReactions::remove()
            } else {
                let emoji = data
                    .emoji
                    .filter(|emoji| !emoji.is_empty())
                    .ok_or_else(|| "emoji is required unless remove is true".to_owned())?;
                let reactions = InputReactions::emoticon(emoji).add_to_recent();
                if data.big.unwrap_or(false) {
                    reactions.big()
                } else {
                    reactions
                }
            };
            native
                .runtime
                .block_on(
                    native
                        .client
                        .send_reactions(peer, data.message_id, reactions),
                )
                .map_err(invocation_error)?;
            json_string(json!({ "ok": true }))
        }
        "getParticipants" => {
            let data: ParticipantPayload = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
            let limit = data.limit.unwrap_or(100).clamp(1, 200);
            let participants = native.runtime.block_on(async {
                let mut iterator = native.client.iter_participants(peer);
                let mut result = Vec::new();
                while result.len() < limit {
                    let Some(participant) = iterator.next().await.map_err(invocation_error)? else {
                        break;
                    };
                    result.push(ParticipantDto {
                        user: user_dto(&participant.user),
                        role: role_name(&participant.role),
                    });
                }
                Ok::<_, String>(result)
            })?;
            json_string(participants)
        }
        "kickParticipant" => {
            let data: KickParticipantPayload = parse_payload(payload)?;
            let chat = native.runtime.block_on(resolve_peer(native, &data.chat))?;
            let user = native.runtime.block_on(resolve_peer(native, &data.user))?;
            native
                .runtime
                .block_on(native.client.kick_participant(chat, user))
                .map_err(invocation_error)?;
            json_string(json!({ "ok": true }))
        }
        "getDialogs" => {
            let LimitPayload { limit } = parse_payload(payload)?;
            let limit = limit.unwrap_or(50).clamp(1, 100);
            let dialogs = native.runtime.block_on(async {
                let mut iterator = native.client.iter_dialogs().limit(limit);
                let mut result = Vec::new();
                while let Some(dialog) = iterator.next().await.map_err(invocation_error)? {
                    result.push(DialogDto {
                        peer: peer_dto(native, &dialog.peer)?,
                        last_message: dialog.last_message.as_ref().map(message_dto),
                    });
                }
                Ok::<_, String>(result)
            })?;
            json_string(dialogs)
        }
        "joinChat" => {
            let target: PeerTarget = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &target))?;
            let joined = native
                .runtime
                .block_on(native.client.join_chat(peer))
                .map_err(invocation_error)?;
            match joined {
                Some(peer) => json_string(peer_dto(native, &peer)?),
                None => json_string(json!({ "joined": false })),
            }
        }
        "leaveChat" => {
            let target: PeerTarget = parse_payload(payload)?;
            let peer = native.runtime.block_on(resolve_peer(native, &target))?;
            native
                .runtime
                .block_on(native.client.delete_dialog(peer))
                .map_err(invocation_error)?;
            json_string(json!({ "ok": true }))
        }
        other => Err(format!("unsupported operation: {other}")),
    }
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_create(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    api_id: i32,
    api_hash: JString<'_>,
    session_path: JString<'_>,
) -> jni::sys::jstring {
    let result = (|| {
        let api_hash = read_string(&mut env, api_hash)?;
        let path = PathBuf::from(read_string(&mut env, session_path)?);
        let runtime = Runtime::new().map_err(error)?;
        let (client, runner) = runtime.block_on(async move {
            let session = Arc::new(SqliteSession::open(path).map_err(error)?);
            let pool = SenderPool::new(session, api_id);
            let client = Client::new(&pool);
            Ok::<_, String>((client, pool.runner))
        })?;
        runtime.spawn(runner.run());

        let native = Arc::new(NativeClient {
            runtime,
            client,
            api_hash,
            authentication: Mutex::new(AuthenticationState::Idle),
            peers: Mutex::new(HashMap::new()),
            next_peer_handle: AtomicI64::new(1),
        });
        let handle = Arc::as_ptr(&native) as jlong;
        clients()
            .lock()
            .map_err(|_| "client registry is poisoned".to_owned())?
            .insert(handle, native);
        Ok::<_, String>(handle.to_string())
    })();

    java_string(&mut env, result.unwrap_or_else(error))
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_close(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
) -> jni::sys::jstring {
    let result = clients()
        .lock()
        .map_err(|_| "client registry is poisoned".to_owned())
        .map(|mut map| map.remove(&handle));
    let message = match result {
        Ok(Some(_)) | Ok(None) => "ok".to_owned(),
        Err(message) => error(message),
    };
    java_string(&mut env, message)
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_isAuthorized(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
) -> jni::sys::jstring {
    let result = get_client(handle).and_then(|native| {
        native
            .runtime
            .block_on(native.client.is_authorized())
            .map(|authorized| authorized.to_string())
            .map_err(invocation_error)
    });
    java_string(&mut env, result.unwrap_or_else(error))
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_signInBot(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
    token: JString<'_>,
    api_hash: JString<'_>,
) -> jni::sys::jstring {
    let result = (|| {
        let token = read_string(&mut env, token)?;
        let api_hash = read_string(&mut env, api_hash)?;
        let native = get_client(handle)?;
        let user = native
            .runtime
            .block_on(native.client.bot_sign_in(&token, &api_hash))
            .map_err(invocation_error)?;
        json_string(user_dto(&user))
    })();
    java_string(&mut env, result.unwrap_or_else(error))
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_sendMessage(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
    username: JString<'_>,
    text: JString<'_>,
) -> jni::sys::jstring {
    let result = (|| {
        let username = read_string(&mut env, username)?;
        let text = read_string(&mut env, text)?;
        let native = get_client(handle)?;
        let peer = native.runtime.block_on(resolve_peer(
            &native,
            &PeerTarget {
                peer_handle: None,
                username: Some(username),
            },
        ))?;
        let message = native
            .runtime
            .block_on(native.client.send_message(peer, text))
            .map_err(invocation_error)?;
        json_string(message_dto(&message))
    })();
    java_string(&mut env, result.unwrap_or_else(error))
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_request(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
    operation: JString<'_>,
    payload: JString<'_>,
) -> jni::sys::jstring {
    let result = (|| {
        let operation = read_string(&mut env, operation)?;
        let payload = read_string(&mut env, payload)?;
        let native = get_client(handle)?;
        request(&native, &operation, &payload)
    })();
    java_string(&mut env, result.unwrap_or_else(error))
}
