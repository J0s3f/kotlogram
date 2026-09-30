//! The grammers-backed client, its registry, and peer resolution.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Arc, Mutex, OnceLock};

use grammers_client::client::UpdateStream;
use grammers_client::client::{LoginToken, PasswordToken};
use grammers_client::peer::Peer;
use grammers_client::Client;
use grammers_mtsender::UpdatesConfiguration;
use grammers_mtsender::{SenderPool, SenderPoolHandle};
use grammers_session::storages::SqliteSession;
use grammers_session::types::PeerRef;
use jni::sys::jlong;
use tokio::runtime::Runtime;

use crate::error::{error, invocation_error};
use crate::payload::PeerTarget;

/// One live Telegram session: its Tokio runtime, its grammers client and its registries.
pub(crate) struct NativeClient {
    pub(crate) runtime: Runtime,
    pub(crate) client: Client,
    pub(crate) sender: SenderPoolHandle,
    pub(crate) updates: Mutex<UpdateStream>,
    pub(crate) session: Arc<SqliteSession>,
    pub(crate) api_hash: String,
    pub(crate) authentication: Mutex<AuthenticationState>,
    pub(crate) peers: Mutex<HashMap<i64, Peer>>,
    pub(crate) next_peer_handle: AtomicI64,
}

/// The step of the user login flow that has been reached so far.
pub(crate) enum AuthenticationState {
    Idle,
    LoginCode(LoginToken),
    Password(PasswordToken),
}

type Clients = Mutex<HashMap<jlong, Arc<NativeClient>>>;

fn clients() -> &'static Clients {
    static CLIENTS: OnceLock<Clients> = OnceLock::new();
    CLIENTS.get_or_init(|| Mutex::new(HashMap::new()))
}

/// Looks up a live client by the handle Kotlin holds.
pub(crate) fn get_client(handle: jlong) -> Result<Arc<NativeClient>, String> {
    clients()
        .lock()
        .map_err(|_| "client registry is poisoned".to_owned())?
        .get(&handle)
        .cloned()
        .ok_or_else(|| format!("unknown client handle: {handle}"))
}

/// Starts a runtime and a grammers session, and registers the result. Returns the handle as a
/// string because that is what the JNI export hands back to Kotlin.
pub(crate) fn create_client(
    api_id: i32,
    api_hash: String,
    session_path: PathBuf,
) -> Result<String, String> {
    let runtime = Runtime::new().map_err(error)?;
    let (client, sender, updates, session, runner) = runtime.block_on(async move {
        let session = Arc::new(SqliteSession::open(session_path).await.map_err(error)?);
        let pool = SenderPool::new(Arc::clone(&session), api_id);
        let client = Client::new(pool.handle.clone());
        let updates = client
            .stream_updates(pool.updates, UpdatesConfiguration::default())
            .await
            .map_err(|error| error.to_string())?;
        Ok::<_, String>((client, pool.handle.thin, updates, session, pool.runner))
    })?;
    runtime.spawn(runner.run());

    let native = Arc::new(NativeClient {
        runtime,
        client,
        sender,
        updates: Mutex::new(updates),
        session,
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
    Ok(handle.to_string())
}

/// Drops a client from the registry and stops its sender pool. Closing an unknown handle succeeds.
pub(crate) fn close_client(handle: jlong) -> String {
    let result = clients()
        .lock()
        .map_err(|_| "client registry is poisoned".to_owned())
        .map(|mut map| map.remove(&handle));
    match result {
        Ok(Some(client)) => {
            client.sender.quit();
            "ok".to_owned()
        }
        Ok(None) => "ok".to_owned(),
        Err(message) => error(message),
    }
}

/// Remembers [peer] and returns the handle Kotlin passes back in later payloads.
pub(crate) fn register_peer(native: &NativeClient, peer: &Peer) -> Result<i64, String> {
    let native_handle = native.next_peer_handle.fetch_add(1, Ordering::Relaxed);
    native
        .peers
        .lock()
        .map_err(|_| "peer registry is poisoned".to_owned())?
        .insert(native_handle, peer.clone());
    Ok(native_handle)
}

/// Resolves a payload peer selector into a [PeerRef], which is what the grammers client methods
/// take.
///
/// The peer is resolved to its full form first and then converted, because the conversion asks
/// grammers whether the peer is usable on its own (it carries an access hash) and falls back to the
/// session cache when it does not.
pub(crate) async fn resolve_peer(
    native: &NativeClient,
    target: &PeerTarget,
) -> Result<PeerRef, String> {
    let peer = resolve_peer_projected(native, target).await?;
    peer.to_ref()
        .await
        .map_err(|error| error.to_string())?
        .ok_or_else(|| "the resolved peer is not usable on its own".to_owned())
}

/// Resolves a payload peer selector into the full peer, for projecting.
pub(crate) async fn resolve_peer_projected(
    native: &NativeClient,
    target: &PeerTarget,
) -> Result<Peer, String> {
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
