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

use grammers_client::grammers_tl_types as tl;
use grammers_client::types::Peer;
use grammers_session::defs::{PeerAuth, PeerId, PeerRef};
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

/// Bans or restricts a member, which is grammers' `set_banned_rights`.
///
/// The requested flags keep the layer's "banned" polarity, while the builder's methods are spelled
/// the other way round, so each flag is negated on the way in.
fn set_banned_rights(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SetBannedRightsPayload = parse_payload(payload)?;
    let chat = native.runtime.block_on(resolve_peer(native, &data.chat))?;
    let user = native.runtime.block_on(resolve_peer(native, &data.user))?;
    let rights = tl::types::ChatBannedRights::from(&data.rights);
    native.runtime.block_on(async {
        let mut builder = native.client.set_banned_rights(chat, user);
        if data.load_current {
            builder = builder.load_current().await.map_err(invocation_error)?;
        }
        builder
            .view_messages(!rights.view_messages)
            .send_messages(!rights.send_messages)
            .send_media(!rights.send_media)
            .send_stickers(!rights.send_stickers)
            .send_gifs(!rights.send_gifs)
            .send_games(!rights.send_games)
            .send_inline(!rights.send_inline)
            .embed_link_previews(!rights.embed_links)
            .send_polls(!rights.send_polls)
            .change_info(!rights.change_info)
            .invite_users(!rights.invite_users)
            .pin_messages(!rights.pin_messages)
            .until(rights.until_date)
            .await
            .map_err(invocation_error)
    })?;
    json_string(json!({ "ok": true }))
}

/// Grants or revokes admin rights, which is grammers' `set_admin_rights`.
fn set_admin_rights(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SetAdminRightsPayload = parse_payload(payload)?;
    let chat = native.runtime.block_on(resolve_peer(native, &data.chat))?;
    let user = native.runtime.block_on(resolve_peer(native, &data.user))?;
    let rights = tl::types::ChatAdminRights::from(&data.rights);
    let rank = data.rank.clone().unwrap_or_default();
    native.runtime.block_on(async {
        let mut builder = native.client.set_admin_rights(chat, user);
        if data.load_current {
            builder = builder.load_current().await.map_err(invocation_error)?;
        }
        builder
            .anonymous(rights.anonymous)
            .manage_call(rights.manage_call)
            .change_info(rights.change_info)
            .post_messages(rights.post_messages)
            .edit_messages(rights.edit_messages)
            .delete_messages(rights.delete_messages)
            .ban_users(rights.ban_users)
            .invite_users(rights.invite_users)
            .pin_messages(rights.pin_messages)
            .add_admins(rights.add_admins)
            .rank(rank)
            .await
            .map_err(invocation_error)
    })?;
    json_string(json!({ "ok": true }))
}

/// Joins a private chat from its invite link.
///
/// This is grammers' `accept_invite_link`, rebuilt on `messages.ImportChatInvite` because the
/// method itself is behind the optional `parse_invite_link` feature this crate does not enable.
fn accept_invite_link(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: InviteLinkPayload = parse_payload(payload)?;
    let hash = invite_hash(&data.invite_link).ok_or_else(|| "INVITE_HASH_INVALID".to_owned())?;
    let updates = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::messages::ImportChatInvite { hash }),
        )
        .map_err(invocation_error)?;
    match updates_to_chat(None, updates) {
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
        return Ok(PeerId::user(id));
    }
    if (-999_999_999_999..=-1).contains(&id) {
        return Ok(PeerId::chat(-id));
    }
    // `PeerId::channel` takes the bare id, which is the encoded id minus the `-100` marker.
    if (-1_997_852_516_352..=-1_000_000_000_001).contains(&id)
        || (-2_002_147_483_649..=-4_000_000_000_000).contains(&id)
    {
        return Ok(PeerId::channel(-id - 1_000_000_000_000));
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
fn updates_to_chat(id: Option<i64>, updates: tl::enums::Updates) -> Option<Peer> {
    let chats = match updates {
        tl::enums::Updates::Combined(updates) => updates.chats,
        tl::enums::Updates::Updates(updates) => updates.chats,
        _ => return None,
    };
    let chat = match id {
        Some(id) => chats.into_iter().find(|chat| chat.id() == id),
        None => chats.into_iter().next(),
    };
    chat.map(Peer::from_raw)
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
        assert_eq!(peer_id_from_bot_api_id(7).expect("a user"), PeerId::user(7));
        assert_eq!(
            peer_id_from_bot_api_id(-5).expect("a small group"),
            PeerId::chat(5)
        );
        assert_eq!(
            peer_id_from_bot_api_id(-1_000_000_000_042).expect("a channel"),
            PeerId::channel(42)
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
}
