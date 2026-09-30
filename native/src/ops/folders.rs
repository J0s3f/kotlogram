//! Typed dialog-filter (folder) operations built on grammers' TL layer.
//!
//! grammers exposes no folder surface of its own, so each operation builds a
//! `tl::functions::messages::*` request, invokes it through the client and projects the result
//! through [`crate::dto::folders`]. The payload shapes are the whole contract.
//!
//! `messages.updateDialogFilter` is the one mutation for every case: a present `filter` creates or
//! replaces the filter named by `id`, and an absent `filter` deletes it. The layer has no separate
//! delete constructor, so this module does not add one. The pinned layer's `messages.DialogFilter`
//! input is the plain `dialogFilter` constructor; `dialogFilterChatlist` (the chatlists API's own
//! folders) is not built here and stays behind `invokeRaw`.

use grammers_client::tl;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::folders::dialog_folders_dto;
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UpdateDialogFilterPayload {
    id: i32,
    /// Absent removes the filter named by [Self::id]; present creates or replaces it.
    filter: Option<DialogFilterSpec>,
}

/// The filter to create or replace, in the shape `dialogFilter` takes.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct DialogFilterSpec {
    title: String,
    #[serde(default)]
    contacts: bool,
    #[serde(default)]
    non_contacts: bool,
    #[serde(default)]
    groups: bool,
    #[serde(default)]
    broadcasts: bool,
    #[serde(default)]
    bots: bool,
    #[serde(default)]
    exclude_muted: bool,
    #[serde(default)]
    exclude_read: bool,
    #[serde(default)]
    exclude_archived: bool,
    #[serde(default)]
    pinned_peers: Vec<PeerTarget>,
    #[serde(default)]
    include_peers: Vec<PeerTarget>,
    #[serde(default)]
    exclude_peers: Vec<PeerTarget>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UpdateDialogFiltersOrderPayload {
    /// The filter ids, in the order the account should present them.
    order: Vec<i32>,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "messagesGetDialogFilters",
    "messagesUpdateDialogFilter",
    "messagesUpdateDialogFiltersOrder",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "messagesGetDialogFilters" => get_dialog_filters,
        "messagesUpdateDialogFilter" => update_dialog_filter,
        "messagesUpdateDialogFiltersOrder" => update_dialog_filters_order,
        _ => return None,
    })
}

/// Lists the account's dialog filters.
///
/// The pinned layer has a single `messages.dialogFilters` constructor; there is no
/// `messages.dialogFiltersNotModified` variant to answer, so no empty-page flag is projected.
fn get_dialog_filters(native: &NativeClient, _payload: &str) -> Result<String, String> {
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::messages::GetDialogFilters {}),
        )
        .map_err(invocation_error)?;
    let tl::enums::messages::DialogFilters::Filters(filters) = result;
    json_string(dialog_folders_dto(native, filters)?)
}

/// Creates, replaces or removes one dialog filter.
fn update_dialog_filter(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UpdateDialogFilterPayload = parse_payload(payload)?;
    let filter = match &data.filter {
        Some(spec) => Some(dialog_filter_input(native, data.id, spec)?),
        // An absent filter is the layer's delete form.
        None => None,
    };
    native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::messages::UpdateDialogFilter {
                    id: data.id,
                    filter,
                }),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Reorders the account's dialog filters.
fn update_dialog_filters_order(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UpdateDialogFiltersOrderPayload = parse_payload(payload)?;
    native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::messages::UpdateDialogFiltersOrder { order: data.order }),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Builds the layer's plain `dialogFilter`, resolving each requested peer to its input form.
fn dialog_filter_input(
    native: &NativeClient,
    id: i32,
    spec: &DialogFilterSpec,
) -> Result<tl::enums::DialogFilter, String> {
    Ok(tl::enums::DialogFilter::Filter(tl::types::DialogFilter {
        contacts: spec.contacts,
        non_contacts: spec.non_contacts,
        groups: spec.groups,
        broadcasts: spec.broadcasts,
        bots: spec.bots,
        exclude_muted: spec.exclude_muted,
        exclude_read: spec.exclude_read,
        exclude_archived: spec.exclude_archived,
        // `title_noanimate` is a cosmetic animation flag this operation does not carry.
        title_noanimate: false,
        id,
        title: tl::enums::TextWithEntities::Entities(tl::types::TextWithEntities {
            text: spec.title.clone(),
            entities: Vec::new(),
        }),
        emoticon: None,
        color: None,
        pinned_peers: peers_input(native, &spec.pinned_peers)?,
        include_peers: peers_input(native, &spec.include_peers)?,
        exclude_peers: peers_input(native, &spec.exclude_peers)?,
    }))
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
    //! Payload-decode tests.
    //!
    //! The handlers themselves need a live Telegram session, so what is pinned here is the wire
    //! shape Kotlin sends: the camelCase names, the optional fields and the absent-filter delete.

    use super::*;

    fn decode<T: for<'de> Deserialize<'de>>(json: &str) -> T {
        serde_json::from_str(json).expect("the payload decodes")
    }

    #[test]
    fn an_update_payload_with_a_filter_reads_the_flags_and_peers() {
        let data: UpdateDialogFilterPayload = decode(
            r#"{"id": 2, "filter": {
                "title": "News",
                "contacts": true,
                "groups": true,
                "excludeMuted": true,
                "includePeers": [{"peerHandle": 12}],
                "excludePeers": [{"username": "@someone"}]
            }}"#,
        );
        assert_eq!(data.id, 2);
        let spec = data.filter.expect("a filter");
        assert_eq!(spec.title, "News");
        assert!(spec.contacts && spec.groups && spec.exclude_muted);
        assert!(!spec.non_contacts && !spec.bots && !spec.exclude_read);
        assert_eq!(spec.include_peers.len(), 1);
        assert_eq!(spec.include_peers[0].peer_handle, Some(12));
        assert_eq!(spec.exclude_peers[0].username.as_deref(), Some("@someone"));
    }

    #[test]
    fn an_absent_filter_is_the_delete_form() {
        let data: UpdateDialogFilterPayload = decode(r#"{"id": 2}"#);
        assert_eq!(data.id, 2);
        assert!(data.filter.is_none());

        let data: UpdateDialogFilterPayload = decode(r#"{"id": 3, "filter": null}"#);
        assert!(data.filter.is_none());
    }

    #[test]
    fn a_filter_without_peers_decodes_with_empty_lists() {
        let data: UpdateDialogFilterPayload = decode(r#"{"id": 5, "filter": {"title": "Bare"}}"#);
        let spec = data.filter.expect("a filter");
        assert_eq!(spec.title, "Bare");
        assert!(spec.pinned_peers.is_empty());
        assert!(spec.include_peers.is_empty());
        assert!(spec.exclude_peers.is_empty());
    }

    #[test]
    fn an_order_payload_reads_the_id_list() {
        let data: UpdateDialogFiltersOrderPayload = decode(r#"{"order": [3, 1, 2]}"#);
        assert_eq!(data.order, vec![3, 1, 2]);
    }

    #[test]
    fn the_folder_operations_route_to_their_own_handler() {
        assert_eq!(
            route("messagesGetDialogFilters"),
            Some(get_dialog_filters as Handler)
        );
        assert_eq!(
            route("messagesUpdateDialogFilter"),
            Some(update_dialog_filter as Handler)
        );
        assert_eq!(
            route("messagesUpdateDialogFiltersOrder"),
            Some(update_dialog_filters_order as Handler)
        );
    }

    #[test]
    fn a_foreign_operation_is_not_routed_by_this_module() {
        assert!(route("getDialogs").is_none());
        assert!(route("messagesDeleteFolder").is_none());
    }
}
