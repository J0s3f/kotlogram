//! Chat membership, moderation and rights operations.
//!
//! The membership half builds on grammers' participant iterator: the same `iter_participants` call
//! answers a filtered list and the chat's total member count. The moderation half wraps the
//! `set_banned_rights` and `set_admin_rights` builders and `get_permissions`, and the join half
//! adds the private-invite path next to the public `join_chat`.
//!
//! grammers gates `accept_invite_link` and `parse_invite_link` behind the optional
//! `parse_invite_link` feature, which this crate does not enable (its `Cargo.toml` pins the
//! defaults), so both are built here on the same public surface the feature's implementation uses:
//! a URL parse and `messages.ImportChatInvite`. They cannot drift from grammers' semantics because
//! the parsing is a pure function pinned by the tests below.

use grammers_client::peer::Peer;
use grammers_client::tl;
use grammers_session::types::{PeerAuth, PeerId, PeerKind, PeerRef};
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::participant::{participant_dto, participant_permissions_dto};
use crate::dto::peer::peer_dto;
use crate::dto::permissions::{ChatPermissionsDto, ChatRestrictionsDto};
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

/// How many participants a single listing may return, which is also grammers' own cap.
const MAX_PARTICIPANT_LIMIT: usize = 200;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ParticipantPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    limit: Option<usize>,
    /// One of the names [`participant_filter`] knows; absent means the layer's `recent`.
    filter: Option<String>,
    /// The query the `search`, `banned`, `kicked`, `contacts` and `mentions` filters take.
    query: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ParticipantPairPayload {
    chat: PeerTarget,
    user: PeerTarget,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SetBannedRightsPayload {
    chat: PeerTarget,
    user: PeerTarget,
    #[serde(default)]
    rights: ChatRestrictionsDto,
    /// When set, the rights already in place are loaded first and the requested flags applied on
    /// top, which is grammers' `BannedRightsBuilder::load_current`.
    #[serde(default)]
    load_current: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SetAdminRightsPayload {
    chat: PeerTarget,
    user: PeerTarget,
    #[serde(default)]
    rights: ChatPermissionsDto,
    /// The custom admin badge; empty is grammers' default localized badge.
    rank: Option<String>,
    #[serde(default)]
    load_current: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct InviteLinkPayload {
    invite_link: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ResolvePeerPayload {
    /// A Bot API dialog identifier: positive for a user, negative for a small group, and
    /// `-100…` for a channel. This is the identifier `PeerDto::id` reports.
    id: i64,
    /// The peer's access hash, when the caller has it. Without one Telegram only answers for a
    /// peer the session already knows.
    access_hash: Option<i64>,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "getParticipants",
    "kickParticipant",
    "inviteToChannel",
    "joinChat",
    "leaveChat",
    "getPermissions",
    "setBannedRights",
    "setAdminRights",
    "acceptInviteLink",
    "parseInviteLink",
    "resolvePeer",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "getParticipants" => get_participants,
        "kickParticipant" => kick_participant,
        "inviteToChannel" => invite_to_channel,
        "joinChat" => join_chat,
        "leaveChat" => leave_chat,
        "getPermissions" => get_permissions,
        "setBannedRights" => set_banned_rights,
        "setAdminRights" => set_admin_rights,
        "acceptInviteLink" => accept_invite_link,
        "parseInviteLink" => parse_invite_link,
        "resolvePeer" => resolve_peer_by_id,
        _ => return None,
    })
}

/// Lists a chat's participants, optionally filtered, together with the chat's total member count.
///
/// The count is asked for before the listing, because grammers' iterator learns it from the same
/// first request the listing makes; asking afterwards would be too late to include it.
fn get_participants(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ParticipantPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let limit = data.limit.unwrap_or(100).clamp(1, MAX_PARTICIPANT_LIMIT);
    let (participants, total) = native.runtime.block_on(async {
        let mut iterator = native.client.iter_participants(peer);
        if let Some(filter) = data.filter.as_deref() {
            iterator = iterator.filter(participant_filter(filter, data.query.as_deref())?);
        }
        let total = iterator.total().await.map_err(invocation_error)?;
        let mut result = Vec::new();
        while result.len() < limit {
            let Some(participant) = iterator.next().await.map_err(invocation_error)? else {
                break;
            };
            result.push(participant_dto(&participant));
        }
        Ok::<_, String>((result, total))
    })?;
    json_string(participants_result(participants, total))
}

fn kick_participant(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ParticipantPairPayload = parse_payload(payload)?;
    let chat = native.runtime.block_on(resolve_peer(native, &data.chat))?;
    let user = native.runtime.block_on(resolve_peer(native, &data.user))?;
    native
        .runtime
        .block_on(native.client.kick_participant(chat, user))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Adds a member to a chat, choosing the request the peer kind requires.
///
/// grammers has no invite method, so the layer's requests are written directly: a channel or
/// supergroup takes `channels.InviteToChannel`, a basic group takes `messages.AddChatUser`. The
/// resolved peers already carry the access hashes both requests need.
fn invite_to_channel(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ParticipantPairPayload = parse_payload(payload)?;
    let chat = native.runtime.block_on(resolve_peer(native, &data.chat))?;
    let user = native.runtime.block_on(resolve_peer(native, &data.user))?;
    native.runtime.block_on(async {
        match chat.id.kind() {
            PeerKind::Channel => {
                native
                    .client
                    .invoke(&tl::functions::channels::InviteToChannel {
                        channel: chat.into(),
                        users: vec![user.into()],
                    })
                    .await
                    .map_err(invocation_error)?;
            }
            PeerKind::Chat => {
                native
                    .client
                    .invoke(&tl::functions::messages::AddChatUser {
                        chat_id: chat.into(),
                        user_id: user.into(),
                        fwd_limit: 0,
                    })
                    .await
                    .map_err(invocation_error)?;
            }
            _ => return Err("PEER_ID_INVALID".to_owned()),
        }
        Ok(())
    })?;
    json_string(json!({ "ok": true }))
}

fn join_chat(native: &NativeClient, payload: &str) -> Result<String, String> {
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

fn leave_chat(native: &NativeClient, payload: &str) -> Result<String, String> {
    let target: PeerTarget = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &target))?;
    native
        .runtime
        .block_on(native.client.delete_dialog(peer))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Reports one member's role in a chat, which is grammers' `get_permissions`.
fn get_permissions(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ParticipantPairPayload = parse_payload(payload)?;
    let chat = native.runtime.block_on(resolve_peer(native, &data.chat))?;
    let user = native.runtime.block_on(resolve_peer(native, &data.user))?;
    let permissions = native
        .runtime
        .block_on(native.client.get_permissions(chat, user))
        .map_err(invocation_error)?;
    json_string(participant_permissions_dto(&permissions))
}

/// Bans or restricts a member, writing the layer's `chatBannedRights` directly.
///
/// grammers' `set_banned_rights` builder has no setter for the newer media, topic and reaction
/// flags, so the request is sent raw. The peer-kind branching is grammers': a channel megagroup or
/// broadcast takes `channels.EditBanned`; a basic group can only be banned by removing the member
/// (`messages.DeleteChatUser`), which the layer allows only when viewing is forbidden.
///
/// The requested flags keep the layer's "banned" polarity, which is exactly what the raw struct
/// carries, so no negation happens here.
fn set_banned_rights(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SetBannedRightsPayload = parse_payload(payload)?;
    let chat = native.runtime.block_on(resolve_peer(native, &data.chat))?;
    let user = native.runtime.block_on(resolve_peer(native, &data.user))?;
    let requested = tl::types::ChatBannedRights::from(&data.rights);
    native.runtime.block_on(async {
        let kind = chat.id.kind();
        let rights = if data.load_current && kind == PeerKind::Channel {
            match load_channel_banned(native, chat, user).await? {
                Some(current) => merge_banned(current, requested),
                None => requested,
            }
        } else {
            // grammers' `load_current` seeds the builder from the participant for a channel only;
            // a basic group starts from zeroed rights, which the request then fully specifies.
            requested
        };

        match kind {
            PeerKind::Channel => {
                native
                    .client
                    .invoke(&tl::functions::channels::EditBanned {
                        channel: chat.into(),
                        participant: user.into(),
                        banned_rights: tl::enums::ChatBannedRights::Rights(rights),
                    })
                    .await
                    .map_err(invocation_error)?;
            }
            PeerKind::Chat => {
                if !rights.view_messages {
                    return Err(
                        "CHAT_INVALID: a basic group can only be banned by forbidding \
                                view_messages"
                            .to_owned(),
                    );
                }
                native
                    .client
                    .invoke(&tl::functions::messages::DeleteChatUser {
                        chat_id: chat.into(),
                        user_id: user.into(),
                        revoke_history: false,
                    })
                    .await
                    .map_err(invocation_error)?;
            }
            _ => return Err("PEER_ID_INVALID".to_owned()),
        }
        Ok::<(), String>(())
    })?;
    json_string(json!({ "ok": true }))
}

/// Grants or revokes admin rights, writing the layer's `chatAdminRights` directly.
///
/// grammers' `set_admin_rights` builder has no setter for the newer topic, story and rank flags, so
/// the request is sent raw. The peer-kind branching is grammers': a channel megagroup or broadcast
/// takes `channels.EditAdmin`; a basic group's admin flag is the single boolean of
/// `messages.EditChatAdmin`, set when any right grammers knows is granted.
fn set_admin_rights(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SetAdminRightsPayload = parse_payload(payload)?;
    let chat = native.runtime.block_on(resolve_peer(native, &data.chat))?;
    let user = native.runtime.block_on(resolve_peer(native, &data.user))?;
    let requested = tl::types::ChatAdminRights::from(&data.rights);
    let rank = data.rank.clone().unwrap_or_default();
    native.runtime.block_on(async {
        let kind = chat.id.kind();
        let rights = if data.load_current && kind == PeerKind::Channel {
            match load_channel_admin(native, chat, user).await? {
                Some(current) => merge_admin(current, requested),
                None => requested,
            }
        } else {
            // grammers' `load_current` additionally seeds a basic group from the participant's
            // role, but the request spells out every flag, so the seed cannot change the outcome.
            requested
        };

        match kind {
            PeerKind::Channel => {
                native
                    .client
                    .invoke(&tl::functions::channels::EditAdmin {
                        channel: chat.into(),
                        user_id: user.into(),
                        admin_rights: tl::enums::ChatAdminRights::Rights(rights),
                        rank: Some(rank),
                    })
                    .await
                    .map_err(invocation_error)?;
            }
            PeerKind::Chat => {
                let promote = basic_group_promote(&rights);
                native
                    .client
                    .invoke(&tl::functions::messages::EditChatAdmin {
                        chat_id: chat.into(),
                        user_id: user.into(),
                        is_admin: promote,
                    })
                    .await
                    .map_err(invocation_error)?;
            }
            _ => return Err("PEER_ID_INVALID".to_owned()),
        }
        Ok::<(), String>(())
    })?;
    json_string(json!({ "ok": true }))
}

/// Fetches the admin rights a channel participant currently holds, mirroring the channel half of
/// grammers' `AdminRightsBuilder::load_current`.
async fn load_channel_admin(
    native: &NativeClient,
    channel: PeerRef,
    participant: PeerRef,
) -> Result<Option<tl::types::ChatAdminRights>, String> {
    let tl::enums::channels::ChannelParticipant::Participant(user) = native
        .client
        .invoke(&tl::functions::channels::GetParticipant {
            channel: channel.into(),
            participant: participant.into(),
        })
        .await
        .map_err(invocation_error)?;
    Ok(match user.participant {
        tl::enums::ChannelParticipant::Creator(creator) => Some(creator.admin_rights.into()),
        tl::enums::ChannelParticipant::Admin(admin) => Some(admin.admin_rights.into()),
        _ => None,
    })
}

/// Fetches the ban a channel participant currently holds, mirroring the channel half of grammers'
/// `BannedRightsBuilder::load_current`.
async fn load_channel_banned(
    native: &NativeClient,
    channel: PeerRef,
    participant: PeerRef,
) -> Result<Option<tl::types::ChatBannedRights>, String> {
    let tl::enums::channels::ChannelParticipant::Participant(user) = native
        .client
        .invoke(&tl::functions::channels::GetParticipant {
            channel: channel.into(),
            participant: participant.into(),
        })
        .await
        .map_err(invocation_error)?;
    Ok(match user.participant {
        tl::enums::ChannelParticipant::Banned(banned) => Some(banned.banned_rights.into()),
        _ => None,
    })
}

/// Layers a requested grant over the rights Telegram currently holds.
///
/// The request spells out every flag, so the loaded rights are fully superseded; starting from them
/// mirrors grammers' builder, which seeds its state from the participant before its setters run.
fn merge_admin(
    current: tl::types::ChatAdminRights,
    requested: tl::types::ChatAdminRights,
) -> tl::types::ChatAdminRights {
    let mut merged = current;
    merged.change_info = requested.change_info;
    merged.post_messages = requested.post_messages;
    merged.edit_messages = requested.edit_messages;
    merged.delete_messages = requested.delete_messages;
    merged.ban_users = requested.ban_users;
    merged.invite_users = requested.invite_users;
    merged.pin_messages = requested.pin_messages;
    merged.add_admins = requested.add_admins;
    merged.anonymous = requested.anonymous;
    merged.manage_call = requested.manage_call;
    merged.other = requested.other;
    merged.manage_topics = requested.manage_topics;
    merged.post_stories = requested.post_stories;
    merged.edit_stories = requested.edit_stories;
    merged.delete_stories = requested.delete_stories;
    merged.manage_direct_messages = requested.manage_direct_messages;
    merged.manage_ranks = requested.manage_ranks;
    merged.manage_linked_peers = requested.manage_linked_peers;
    merged.manage_welcome_messages = requested.manage_welcome_messages;
    merged
}

/// Layers a requested ban over the rights Telegram currently holds; see [`merge_admin`].
fn merge_banned(
    current: tl::types::ChatBannedRights,
    requested: tl::types::ChatBannedRights,
) -> tl::types::ChatBannedRights {
    let mut merged = current;
    merged.view_messages = requested.view_messages;
    merged.send_messages = requested.send_messages;
    merged.send_media = requested.send_media;
    merged.send_stickers = requested.send_stickers;
    merged.send_gifs = requested.send_gifs;
    merged.send_games = requested.send_games;
    merged.send_inline = requested.send_inline;
    merged.embed_links = requested.embed_links;
    merged.send_polls = requested.send_polls;
    merged.change_info = requested.change_info;
    merged.invite_users = requested.invite_users;
    merged.pin_messages = requested.pin_messages;
    merged.manage_topics = requested.manage_topics;
    merged.send_photos = requested.send_photos;
    merged.send_videos = requested.send_videos;
    merged.send_roundvideos = requested.send_roundvideos;
    merged.send_audios = requested.send_audios;
    merged.send_voices = requested.send_voices;
    merged.send_docs = requested.send_docs;
    merged.send_plain = requested.send_plain;
    merged.edit_rank = requested.edit_rank;
    merged.send_reactions = requested.send_reactions;
    merged.manage_linked_peers = requested.manage_linked_peers;
    merged.until_date = requested.until_date;
    merged
}

/// The single "is admin" flag a basic group takes, computed from the rights exactly as grammers'
/// `AdminRightsBuilder` does: true when any of the rights the layer exposes for a basic group is
/// granted. The newer rights have no meaning in a basic group and do not promote on their own.
fn basic_group_promote(rights: &tl::types::ChatAdminRights) -> bool {
    rights.anonymous
        || rights.change_info
        || rights.post_messages
        || rights.edit_messages
        || rights.delete_messages
        || rights.ban_users
        || rights.invite_users
        || rights.pin_messages
        || rights.add_admins
        || rights.manage_call
}

/// Joins a private chat from its invite link.
///
/// This is grammers' `accept_invite_link`, rebuilt on `messages.ImportChatInvite` because the
/// method itself is behind the optional `parse_invite_link` feature this crate does not enable.
fn accept_invite_link(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: InviteLinkPayload = parse_payload(payload)?;
    let hash = invite_hash(&data.invite_link).ok_or_else(|| "INVITE_HASH_INVALID".to_owned())?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::messages::ImportChatInvite { hash }),
        )
        .map_err(invocation_error)?;
    // `ImportChatInvite` answers `ChatInviteJoinResult`, whose `Ok` carries the updates bundle.
    let updates = match result {
        tl::enums::messages::ChatInviteJoinResult::Ok(result) => result.updates,
        _ => return Ok(json_string(json!({ "joined": false }))?),
    };
    match updates_to_chat(native, None, updates) {
        Some(peer) => json_string(peer_dto(native, &peer)?),
        None => json_string(json!({ "joined": false })),
    }
}

/// Extracts the hash of a private invite link, which is grammers' `parse_invite_link`.
///
/// Only `https` and `http` links on Telegram's own hosts are accepted, and only the two private
/// shapes: `t.me/+hash` and `t.me/joinchat/hash`. A public `t.me/username` link carries no hash
/// and answers `None`.
fn invite_hash(link: &str) -> Option<String> {
    let (scheme, rest) = link.split_once("://")?;
    if !scheme.eq_ignore_ascii_case("https") && !scheme.eq_ignore_ascii_case("http") {
        return None;
    }
    // Any query or fragment belongs to the page, not to the path that carries the hash.
    let rest = rest.split(['?', '#']).next().unwrap_or(rest);
    let (authority, path) = match rest.split_once('/') {
        Some((authority, path)) => (authority, path),
        None => (rest, ""),
    };
    // Credentials and a port are valid URL parts but never name the host. A host is
    // case-insensitive, like `url::Url`'s `host_str`, which grammers' own parser compares.
    let host = authority.rsplit('@').next().unwrap_or(authority);
    let host = host.split(':').next().unwrap_or(host).to_ascii_lowercase();
    const HOSTS: &[&str] = &[
        "t.me",
        "telegram.me",
        "telegram.dog",
        "tg.dev",
        "telesco.pe",
    ];
    if !HOSTS.contains(&host.as_str()) {
        return None;
    }

    let segments: Vec<&str> = path.split('/').collect();
    if segments.len() == 1 {
        return segments[0]
            .strip_prefix('+')
            .map(|hash| hash.replace('+', ""));
    }
    if segments.len() > 1 {
        if segments[0].starts_with("joinchat") {
            return Some(segments[1].to_owned());
        }
        if segments[0].starts_with('+') {
            return Some(segments[0].replace('+', ""));
        }
    }
    None
}

/// Answers the hash of a private invite link, wrapped so a caller can also read back "no hash".
fn parse_invite_link(_native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: InviteLinkPayload = parse_payload(payload)?;
    json_string(json!({ "hash": invite_hash(&data.invite_link) }))
}

/// Resolves a Bot API dialog id back into a peer, which is grammers' `resolve_peer`.
fn resolve_peer_by_id(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ResolvePeerPayload = parse_payload(payload)?;
    let id = peer_id_from_bot_api_id(data.id)?;
    let auth = data
        .access_hash
        .map(PeerAuth::from_hash)
        .unwrap_or_default();
    let peer = native
        .runtime
        .block_on(native.client.resolve_peer(PeerRef { id, auth }))
        .map_err(invocation_error)?;
    json_string(peer_dto(native, &peer)?)
}

/// Turns a Bot API dialog id into the layer's [`PeerId`].
///
/// The encoding is the one `PeerId::bot_api_dialog_id` writes: a positive user id, a negative
/// small-group id, and `-100…` for a channel or megagroup. Anything outside those ranges is not a
/// peer id at all.
fn peer_id_from_bot_api_id(id: i64) -> Result<PeerId, String> {
    if (1..=0xff_ffff_ffff).contains(&id) {
        return Ok(PeerId::user(id).ok_or_else(|| format!("not a bot API peer id: {id}"))?);
    }
    if (-999_999_999_999..=-1).contains(&id) {
        return Ok(PeerId::chat(-id).ok_or_else(|| format!("not a bot API peer id: {id}"))?);
    }
    // `PeerId::channel` takes the bare id, which is the encoded id minus the `-100` marker.
    if (-1_997_852_516_352..=-1_000_000_000_001).contains(&id)
        || (-2_002_147_483_649..=-4_000_000_000_000).contains(&id)
    {
        return Ok(PeerId::channel(-id - 1_000_000_000_000)
            .ok_or_else(|| format!("not a bot API peer id: {id}"))?);
    }
    Err(format!("not a bot API peer id: {id}"))
}

/// Picks the participant filter a listing asked for, attaching its query where the layer takes one.
fn participant_filter(
    filter: &str,
    query: Option<&str>,
) -> Result<tl::enums::ChannelParticipantsFilter, String> {
    let q = || query.unwrap_or("").to_owned();
    Ok(match filter {
        "recent" => tl::enums::ChannelParticipantsFilter::ChannelParticipantsRecent,
        "admins" => tl::enums::ChannelParticipantsFilter::ChannelParticipantsAdmins,
        "bots" => tl::enums::ChannelParticipantsFilter::ChannelParticipantsBots,
        "search" => tl::enums::ChannelParticipantsFilter::ChannelParticipantsSearch(
            tl::types::ChannelParticipantsSearch { q: q() },
        ),
        "banned" => tl::enums::ChannelParticipantsFilter::ChannelParticipantsBanned(
            tl::types::ChannelParticipantsBanned { q: q() },
        ),
        "kicked" => tl::enums::ChannelParticipantsFilter::ChannelParticipantsKicked(
            tl::types::ChannelParticipantsKicked { q: q() },
        ),
        "contacts" => tl::enums::ChannelParticipantsFilter::ChannelParticipantsContacts(
            tl::types::ChannelParticipantsContacts { q: q() },
        ),
        "mentions" => tl::enums::ChannelParticipantsFilter::ChannelParticipantsMentions(
            tl::types::ChannelParticipantsMentions {
                q: query.map(ToOwned::to_owned),
                top_msg_id: None,
            },
        ),
        other => return Err(format!("unsupported participant filter: {other}")),
    })
}

/// The `getParticipants` document: the listing plus the chat's total member count.
fn participants_result(
    participants: Vec<crate::dto::participant::ParticipantDto>,
    total: usize,
) -> serde_json::Value {
    json!({ "participants": participants, "total": total })
}

/// Finds the chat an update bundle reports, mirroring grammers' own `updates_to_chat`.
fn updates_to_chat(
    native: &NativeClient,
    id: Option<i64>,
    updates: tl::enums::Updates,
) -> Option<Peer> {
    let chats = match updates {
        tl::enums::Updates::Combined(updates) => updates.chats,
        tl::enums::Updates::Updates(updates) => updates.chats,
        _ => return None,
    };
    let chat = match id {
        Some(id) => chats.into_iter().find(|chat| chat.id() == id),
        None => chats.into_iter().next(),
    };
    chat.map(|chat| Peer::from_raw(&native.client, chat))
}

#[cfg(test)]
mod tests {
    //! Tests for the pure parts of the chat operations: invite-link parsing, the participant
    //! filter mapping, the Bot API peer-id decoding and the documents the handlers wrap.
    //!
    //! The handlers themselves need a live Telegram session, so what is asserted here is
    //! everything between the payload and the grammers call.

    use super::*;

    #[test]
    fn a_private_invite_link_yields_its_hash() {
        let cases = [
            ("https://t.me/+AbCdEf", Some("AbCdEf")),
            ("http://t.me/+AbCdEf", Some("AbCdEf")),
            ("https://telegram.me/+AbCdEf", Some("AbCdEf")),
            ("https://telegram.dog/+AbCdEf", Some("AbCdEf")),
            ("https://tg.dev/+AbCdEf", Some("AbCdEf")),
            ("https://telesco.pe/+AbCdEf", Some("AbCdEf")),
            ("https://t.me/joinchat/AbCdEf", Some("AbCdEf")),
            ("https://t.me/joinchat/AbCdEf/", Some("AbCdEf")),
            ("https://t.me/+AbCdEf?single", Some("AbCdEf")),
            ("https://t.me/+AbCdEf#fragment", Some("AbCdEf")),
            ("https://t.me:+443/+AbCdEf", Some("AbCdEf")),
        ];
        for (link, expected) in cases {
            assert_eq!(
                invite_hash(link).as_deref(),
                expected,
                "unexpected hash for {link}"
            );
        }
    }

    #[test]
    fn a_public_or_foreign_invite_url_yields_no_hash() {
        let cases = [
            "https://t.me/somechannel",
            "https://example.com/+AbCdEf",
            "https://t.me/",
            "https://t.me",
            "tg://resolve?domain=somechannel",
            "ftp://t.me/+AbCdEf",
            "joinchat/AbCdEf",
            "not a url",
        ];
        for link in cases {
            assert_eq!(invite_hash(link), None, "{link} must carry no hash");
        }
    }

    #[test]
    fn a_host_compares_case_insensitively() {
        // `url::Url` lowercases the host, which grammers' own parser compares to its host list;
        // the path segments stay case-sensitive, exactly as grammers' parser treats them.
        assert_eq!(
            invite_hash("https://T.ME/+AbCdEf").as_deref(),
            Some("AbCdEf")
        );
        assert_eq!(
            invite_hash("HTTPS://TELEGRAM.ME/joinchat/AbCdEf").as_deref(),
            Some("AbCdEf")
        );
    }

    #[test]
    fn every_participant_filter_maps_to_its_layer_variant() {
        assert_eq!(
            participant_filter("recent", None).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsRecent
        );
        assert_eq!(
            participant_filter("admins", None).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsAdmins
        );
        assert_eq!(
            participant_filter("bots", None).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsBots
        );
        assert_eq!(
            participant_filter("search", Some("ada")).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsSearch(
                tl::types::ChannelParticipantsSearch {
                    q: "ada".to_owned()
                }
            )
        );
        assert_eq!(
            participant_filter("banned", Some("ada")).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsBanned(
                tl::types::ChannelParticipantsBanned {
                    q: "ada".to_owned()
                }
            )
        );
        assert_eq!(
            participant_filter("kicked", Some("ada")).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsKicked(
                tl::types::ChannelParticipantsKicked {
                    q: "ada".to_owned()
                }
            )
        );
        assert_eq!(
            participant_filter("contacts", Some("ada")).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsContacts(
                tl::types::ChannelParticipantsContacts {
                    q: "ada".to_owned()
                }
            )
        );
        assert_eq!(
            participant_filter("mentions", Some("ada")).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsMentions(
                tl::types::ChannelParticipantsMentions {
                    q: Some("ada".to_owned()),
                    top_msg_id: None,
                }
            )
        );
    }

    #[test]
    fn a_query_is_optional_and_an_unknown_filter_is_rejected() {
        assert_eq!(
            participant_filter("search", None).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsSearch(
                tl::types::ChannelParticipantsSearch { q: String::new() }
            )
        );
        assert_eq!(
            participant_filter("mentions", None).expect("a filter"),
            tl::enums::ChannelParticipantsFilter::ChannelParticipantsMentions(
                tl::types::ChannelParticipantsMentions {
                    q: None,
                    top_msg_id: None,
                }
            )
        );
        let error = participant_filter("everyone", None).expect_err("no such filter");
        assert_eq!(error, "unsupported participant filter: everyone");
    }

    #[test]
    fn bot_api_peer_ids_decode_into_the_layer_kinds() {
        assert_eq!(
            peer_id_from_bot_api_id(7).expect("a user"),
            PeerId::user(7).unwrap()
        );
        assert_eq!(
            peer_id_from_bot_api_id(-5).expect("a small group"),
            PeerId::chat(5).unwrap()
        );
        assert_eq!(
            peer_id_from_bot_api_id(-1_000_000_000_042).expect("a channel"),
            PeerId::channel(42).unwrap()
        );
    }

    #[test]
    fn a_value_outside_every_peer_range_is_rejected() {
        for id in [0i64, -1_000_000_000_000, -1_997_852_516_353, i64::MIN] {
            let error = peer_id_from_bot_api_id(id).expect_err("not a peer id");
            assert_eq!(error, format!("not a bot API peer id: {id}"));
        }
    }

    #[test]
    fn a_participants_document_carries_the_list_and_the_total() {
        assert_eq!(
            participants_result(Vec::new(), 4_825),
            json!({ "participants": [], "total": 4_825 })
        );
    }

    #[test]
    fn a_fully_spelled_request_supersedes_the_loaded_admin_rights() {
        let current = tl::types::ChatAdminRights::from(&ChatPermissionsDto {
            anonymous: true,
            manage_topics: true,
            other: true,
            ..ChatPermissionsDto::default()
        });
        let requested = tl::types::ChatAdminRights::from(&ChatPermissionsDto {
            change_info: true,
            manage_linked_peers: true,
            ..ChatPermissionsDto::default()
        });

        let merged = merge_admin(current, requested);
        assert!(merged.change_info && merged.manage_linked_peers);
        assert!(!merged.anonymous && !merged.manage_topics && !merged.other);
    }

    #[test]
    fn a_fully_spelled_request_supersedes_the_loaded_ban() {
        let current = tl::types::ChatBannedRights::from(&ChatRestrictionsDto {
            view_messages: true,
            send_media: true,
            ..ChatRestrictionsDto::default()
        });
        let requested = tl::types::ChatBannedRights::from(&ChatRestrictionsDto {
            send_messages: true,
            send_roundvideos: true,
            until_date: 1_700_000_000_000,
            ..ChatRestrictionsDto::default()
        });

        let merged = merge_banned(current, requested);
        assert!(merged.send_messages && merged.send_roundvideos);
        assert!(!merged.view_messages && !merged.send_media);
        assert_eq!(merged.until_date, 1_700_000_000);
    }

    #[test]
    fn a_basic_group_promotes_on_any_right_grammers_knows() {
        assert!(!basic_group_promote(&tl::types::ChatAdminRights::from(
            &ChatPermissionsDto::default()
        )));
        assert!(basic_group_promote(&tl::types::ChatAdminRights::from(
            &ChatPermissionsDto {
                manage_call: true,
                ..ChatPermissionsDto::default()
            }
        )));
        // The newer rights have no meaning in a basic group, so they do not promote on their own.
        assert!(!basic_group_promote(&tl::types::ChatAdminRights::from(
            &ChatPermissionsDto {
                manage_topics: true,
                post_stories: true,
                ..ChatPermissionsDto::default()
            }
        )));
    }
}
