//! Typed chatlist-sharing operations built on grammers' TL layer.
//!
//! grammers exposes no chatlist surface of its own, so this module follows the
//! [`super::stickers`] precedent: each operation builds one `tl::functions::chatlists::*` request,
//! invokes it through the client and projects the result through [`crate::dto::chatlists`]. The
//! payload shapes are the whole contract.
//!
//! Folder creation and editing already live in [`super::folders`] over `messages.updateDialogFilter`;
//! this module is the sharing and sync half of the same feature — exporting a folder as a joinable
//! invite, the invite's lifecycle, and the update-sync calls for a folder someone else joined.
//!
//! An `InputChatlist` names a folder by its dialog-filter id and nothing else: the layer's only
//! constructor is `inputChatlistDialogFilter`, so every payload here carries a bare `filterId`,
//! matching how [`super::folders`]' `messagesUpdateDialogFilter` names a filter. The two answers to
//! `chatlistsCheckChatlistInvite` are kept apart rather than flattened — see
//! [`crate::dto::chatlists::ChatlistInviteDto`] — because a joined slug reports a filter id while an
//! unjoined one reports a title and a roster.
//!
//! `chatlistsEditExportedInvite` writes only the fields it is given, the same flag discipline
//! [`super::account`]'s notification settings follow: an absent `title` or `peers` leaves that part
//! of the invite as it was.

use grammers_client::tl;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::chatlists::{
    chatlist_invite_dto, chatlist_updates_dto, exported_invite_dto, exported_invites_dto,
    leave_suggestions_dto, updates_ack_dto,
};
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

/// Payload of an operation that names a folder by its dialog-filter id.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ChatlistPayload {
    /// The dialog-filter id the folder is addressed by; the layer has no other constructor for an
    /// `InputChatlist`.
    filter_id: i32,
}

/// Payload of `chatlistsExportChatlistInvite`: the folder to export, the invite's label and the
/// peers it should cover.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ExportInvitePayload {
    filter_id: i32,
    /// The invite's label, a plain string the layer stores and reports back unchanged.
    title: String,
    /// The peers the invite should cover, each resolved to its input form.
    #[serde(default)]
    peers: Vec<PeerTarget>,
}

/// Payload of `chatlistsDeleteExportedInvite`: the folder and the slug to drop.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct DeleteInvitePayload {
    filter_id: i32,
    /// The slug `chatlistsGetExportedInvites` reported for the invite.
    slug: String,
}

/// Payload of `chatlistsEditExportedInvite`: the folder, the slug, and the parts to change.
///
/// An absent `title` or `peers` leaves that part of the invite alone, which is the layer's flag
/// discipline; a present empty list clears the invite's peers.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct EditInvitePayload {
    filter_id: i32,
    slug: String,
    #[serde(default)]
    title: Option<String>,
    #[serde(default)]
    peers: Option<Vec<PeerTarget>>,
}

/// Payload of `chatlistsCheckChatlistInvite`: the slug to look up.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct CheckInvitePayload {
    slug: String,
}

/// Payload of `chatlistsJoinChatlistInvite`: the slug and the peers to join.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct JoinInvitePayload {
    slug: String,
    /// The peers to join, which for an unjoined slug is the invite's whole roster and for a joined
    /// one is the missing peers `chatlistsCheckChatlistInvite` reported.
    #[serde(default)]
    peers: Vec<PeerTarget>,
}

/// Payload of `chatlistsJoinChatlistUpdates` and `chatlistsLeaveChatlist`: a folder and the peers
/// the call acts on.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ChatlistPeersPayload {
    filter_id: i32,
    #[serde(default)]
    peers: Vec<PeerTarget>,
}

#[cfg_attr(not(test), allow(dead_code))]
/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "chatlistsExportChatlistInvite",
    "chatlistsDeleteExportedInvite",
    "chatlistsEditExportedInvite",
    "chatlistsGetExportedInvites",
    "chatlistsCheckChatlistInvite",
    "chatlistsJoinChatlistInvite",
    "chatlistsGetChatlistUpdates",
    "chatlistsJoinChatlistUpdates",
    "chatlistsHideChatlistUpdates",
    "chatlistsGetLeaveChatlistSuggestions",
    "chatlistsLeaveChatlist",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "chatlistsExportChatlistInvite" => export_chatlist_invite,
        "chatlistsDeleteExportedInvite" => delete_exported_invite,
        "chatlistsEditExportedInvite" => edit_exported_invite,
        "chatlistsGetExportedInvites" => get_exported_invites,
        "chatlistsCheckChatlistInvite" => check_chatlist_invite,
        "chatlistsJoinChatlistInvite" => join_chatlist_invite,
        "chatlistsGetChatlistUpdates" => get_chatlist_updates,
        "chatlistsJoinChatlistUpdates" => join_chatlist_updates,
        "chatlistsHideChatlistUpdates" => hide_chatlist_updates,
        "chatlistsGetLeaveChatlistSuggestions" => get_leave_chatlist_suggestions,
        "chatlistsLeaveChatlist" => leave_chatlist,
        _ => return None,
    })
}

/// Exports a folder as a joinable invite, answering the invite's label, URL and peers.
fn export_chatlist_invite(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ExportInvitePayload = parse_payload(payload)?;
    let peers = peers_input(native, &data.peers)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::ExportChatlistInvite {
                    chatlist: input_chatlist(data.filter_id),
                    title: data.title,
                    peers,
                }),
        )
        .map_err(invocation_error)?;
    let tl::enums::chatlists::ExportedChatlistInvite::Invite(invite) = result;
    let tl::enums::ExportedChatlistInvite::Invite(invite) = invite.invite;
    json_string(exported_invite_dto(&invite)?)
}

/// Drops one exported invite, which the layer answers as a bare boolean.
fn delete_exported_invite(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: DeleteInvitePayload = parse_payload(payload)?;
    native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::DeleteExportedInvite {
                    chatlist: input_chatlist(data.filter_id),
                    slug: data.slug,
                }),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Changes an exported invite's label or peer list, answering the invite as it now stands.
///
/// Only the fields the payload names are sent; the layer's flags leave the rest as they were.
fn edit_exported_invite(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: EditInvitePayload = parse_payload(payload)?;
    let peers = data
        .peers
        .as_ref()
        .map(|peers| peers_input(native, peers))
        .transpose()?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::EditExportedInvite {
                    chatlist: input_chatlist(data.filter_id),
                    slug: data.slug,
                    title: data.title,
                    peers,
                }),
        )
        .map_err(invocation_error)?;
    let tl::enums::ExportedChatlistInvite::Invite(invite) = result;
    json_string(exported_invite_dto(&invite)?)
}

/// Lists a folder's exported invites.
fn get_exported_invites(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ChatlistPayload = parse_payload(payload)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::GetExportedInvites {
                    chatlist: input_chatlist(data.filter_id),
                }),
        )
        .map_err(invocation_error)?;
    let tl::enums::chatlists::ExportedInvites::Invites(invites) = result;
    json_string(exported_invites_dto(native, &invites)?)
}

/// Looks up a slug, keeping the layer's joined and unjoined answers apart.
fn check_chatlist_invite(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: CheckInvitePayload = parse_payload(payload)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::CheckChatlistInvite { slug: data.slug }),
        )
        .map_err(invocation_error)?;
    json_string(chatlist_invite_dto(native, &result)?)
}

/// Joins the folder a slug names, answering an acknowledgement and the chats it touched.
fn join_chatlist_invite(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: JoinInvitePayload = parse_payload(payload)?;
    let peers = peers_input(native, &data.peers)?;
    let updates = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::JoinChatlistInvite {
                    slug: data.slug,
                    peers,
                }),
        )
        .map_err(invocation_error)?;
    json_string(updates_ack_dto(native, &updates)?)
}

/// Lists the peers a joined folder holds that this account is not part of yet.
fn get_chatlist_updates(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ChatlistPayload = parse_payload(payload)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::GetChatlistUpdates {
                    chatlist: input_chatlist(data.filter_id),
                }),
        )
        .map_err(invocation_error)?;
    let tl::enums::chatlists::ChatlistUpdates::Updates(updates) = result;
    json_string(chatlist_updates_dto(native, &updates)?)
}

/// Joins the missing peers of a folder, answering an acknowledgement and the chats it touched.
fn join_chatlist_updates(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ChatlistPeersPayload = parse_payload(payload)?;
    let peers = peers_input(native, &data.peers)?;
    let updates = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::JoinChatlistUpdates {
                    chatlist: input_chatlist(data.filter_id),
                    peers,
                }),
        )
        .map_err(invocation_error)?;
    json_string(updates_ack_dto(native, &updates)?)
}

/// Hides a folder's pending updates, which the layer answers as a bare boolean.
fn hide_chatlist_updates(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ChatlistPayload = parse_payload(payload)?;
    native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::HideChatlistUpdates {
                    chatlist: input_chatlist(data.filter_id),
                }),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Reports the peers a caller could leave from a folder.
fn get_leave_chatlist_suggestions(
    native: &NativeClient,
    payload: &str,
) -> Result<String, String> {
    let data: ChatlistPayload = parse_payload(payload)?;
    let suggestions = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::GetLeaveChatlistSuggestions {
                    chatlist: input_chatlist(data.filter_id),
                }),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "peers": leave_suggestions_dto(&suggestions) }))
}

/// Leaves a folder's peers, answering an acknowledgement and the chats it touched.
fn leave_chatlist(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ChatlistPeersPayload = parse_payload(payload)?;
    let peers = peers_input(native, &data.peers)?;
    let updates = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::chatlists::LeaveChatlist {
                    chatlist: input_chatlist(data.filter_id),
                    peers,
                }),
        )
        .map_err(invocation_error)?;
    json_string(updates_ack_dto(native, &updates)?)
}

/// Builds the layer's one `InputChatlist` constructor, which names a folder by its filter id.
fn input_chatlist(filter_id: i32) -> tl::enums::InputChatlist {
    tl::enums::InputChatlist::DialogFilter(tl::types::InputChatlistDialogFilter { filter_id })
}

/// Resolves each requested peer target to the layer's input peer.
fn peers_input(
    native: &NativeClient,
    targets: &[PeerTarget],
) -> Result<Vec<tl::enums::InputPeer>, String> {
    let mut peers = Vec::with_capacity(targets.len());
    for target in targets {
        let resolved = native.runtime.block_on(resolve_peer(native, target))?;
        peers.push(tl::enums::InputPeer::from(resolved));
    }
    Ok(peers)
}

#[cfg(test)]
mod tests {
    //! Payload-decode tests and the input mapping.
    //!
    //! The handlers themselves need a live Telegram session, so what is pinned here is the wire
    //! shape Kotlin sends — the camelCase names, the absent-flag writes and the peer lists — and the
    //! `InputChatlist` the mapping produces.

    use super::*;
    use std::ptr::fn_addr_eq;

    fn decode<T: for<'de> Deserialize<'de>>(json: &str) -> T {
        serde_json::from_str(json).expect("the payload decodes")
    }

    #[test]
    fn a_folder_payload_reads_the_filter_id() {
        let data: ChatlistPayload = decode(r#"{"filterId": 4}"#);
        assert_eq!(data.filter_id, 4);
    }

    #[test]
    fn a_folder_is_addressed_by_the_filter_id_alone() {
        assert_eq!(
            input_chatlist(4),
            tl::enums::InputChatlist::DialogFilter(tl::types::InputChatlistDialogFilter {
                filter_id: 4,
            })
        );
    }

    #[test]
    fn an_export_payload_reads_the_title_and_the_peers() {
        let data: ExportInvitePayload = decode(
            r#"{"filterId": 4, "title": "News", "peers": [{"peerHandle": 12}]}"#,
        );
        assert_eq!(data.filter_id, 4);
        assert_eq!(data.title, "News");
        assert_eq!(data.peers.len(), 1);
        assert_eq!(data.peers[0].peer_handle, Some(12));
    }

    #[test]
    fn an_export_payload_without_peers_decodes_with_an_empty_list() {
        let data: ExportInvitePayload = decode(r#"{"filterId": 4, "title": "News"}"#);
        assert!(data.peers.is_empty());
    }

    #[test]
    fn a_delete_payload_reads_the_slug() {
        let data: DeleteInvitePayload = decode(r#"{"filterId": 4, "slug": "AbCdEf"}"#);
        assert_eq!(data.filter_id, 4);
        assert_eq!(data.slug, "AbCdEf");
    }

    #[test]
    fn an_edit_payload_leaves_an_absent_title_and_peers_alone() {
        let data: EditInvitePayload =
            decode(r#"{"filterId": 4, "slug": "AbCdEf", "title": "News"}"#);
        assert_eq!(data.title.as_deref(), Some("News"));
        assert!(data.peers.is_none());

        let data: EditInvitePayload = decode(r#"{"filterId": 4, "slug": "AbCdEf"}"#);
        assert_eq!(data.title, None);
        assert!(data.peers.is_none());

        let data: EditInvitePayload = decode(r#"{"filterId": 4, "slug": "AbCdEf", "peers": []}"#);
        assert_eq!(data.title, None);
        assert_eq!(data.peers.as_deref().map(<[PeerTarget]>::len), Some(0));
    }

    #[test]
    fn a_check_payload_reads_the_slug() {
        let data: CheckInvitePayload = decode(r#"{"slug": "AbCdEf"}"#);
        assert_eq!(data.slug, "AbCdEf");
    }

    #[test]
    fn a_join_payload_reads_the_slug_and_the_peers() {
        let data: JoinInvitePayload =
            decode(r#"{"slug": "AbCdEf", "peers": [{"username": "@someone"}]}"#);
        assert_eq!(data.slug, "AbCdEf");
        assert_eq!(data.peers.len(), 1);
        assert_eq!(data.peers[0].username.as_deref(), Some("@someone"));

        let data: JoinInvitePayload = decode(r#"{"slug": "AbCdEf"}"#);
        assert!(data.peers.is_empty());
    }

    #[test]
    fn a_chatlist_peers_payload_reads_the_filter_id_and_the_peers() {
        let data: ChatlistPeersPayload =
            decode(r#"{"filterId": 4, "peers": [{"peerHandle": 12}, {"peerHandle": 13}]}"#);
        assert_eq!(data.filter_id, 4);
        assert_eq!(data.peers.len(), 2);
        assert_eq!(data.peers[1].peer_handle, Some(13));
    }

    #[test]
    fn the_chatlist_operations_route_to_their_own_handler() {
        assert!(fn_addr_eq(
            route("chatlistsExportChatlistInvite").expect("routed"),
            export_chatlist_invite as Handler,
        ));
        assert!(fn_addr_eq(
            route("chatlistsCheckChatlistInvite").expect("routed"),
            check_chatlist_invite as Handler,
        ));
        assert!(fn_addr_eq(
            route("chatlistsJoinChatlistInvite").expect("routed"),
            join_chatlist_invite as Handler,
        ));
        assert!(fn_addr_eq(
            route("chatlistsGetLeaveChatlistSuggestions").expect("routed"),
            get_leave_chatlist_suggestions as Handler,
        ));
        assert!(fn_addr_eq(
            route("chatlistsLeaveChatlist").expect("routed"),
            leave_chatlist as Handler,
        ));
    }

    #[test]
    fn a_foreign_operation_is_not_routed_by_this_module() {
        assert!(route("messagesGetDialogFilters").is_none());
        assert!(route("getMe").is_none());
    }
}
