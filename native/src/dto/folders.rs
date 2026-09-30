//! Dialog-filter (folder) projection.
//!
//! The layer answers `messages.getDialogFilters` with a flat list of `DialogFilter` variants and no
//! accompanying user or chat vectors: the peers a filter holds are bare `InputPeer`s. Each one is
//! therefore resolved through the client before [`peer_dto`] registers it, so every handle the
//! answer reports stays sendable back to the native side.

use grammers_client::tl;
use serde::Serialize;

use crate::client::NativeClient;
use crate::dto::peer::{peer_dto, PeerDto};
use crate::error::invocation_error;

/// One dialog filter: its identity, its flag set and its resolved peers.
///
/// The layer splits a filter into three constructors; [`Self::kind`] is `filter`, `chatlist` or
/// `default`. Only `dialogFilterChatlist` reports [`Self::has_my_invites`], and only the plain
/// `dialogFilter` carries the contacts/bots/broadcasts flags.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct DialogFolderDto {
    pub(crate) id: i32,
    /// `filter`, `chatlist` or `default`, which is how the layer splits `DialogFilter`.
    pub(crate) kind: &'static str,
    /// The plain text of the layer's `TextWithEntities` title.
    pub(crate) title: String,
    pub(crate) contacts: bool,
    pub(crate) non_contacts: bool,
    pub(crate) groups: bool,
    pub(crate) broadcasts: bool,
    pub(crate) bots: bool,
    pub(crate) exclude_muted: bool,
    pub(crate) exclude_read: bool,
    pub(crate) exclude_archived: bool,
    /// True for a `dialogFilterChatlist` that carries invites of the account's own chats.
    pub(crate) has_my_invites: bool,
    pub(crate) pinned_peers: Vec<PeerDto>,
    pub(crate) include_peers: Vec<PeerDto>,
    pub(crate) exclude_peers: Vec<PeerDto>,
}

/// The `messagesGetDialogFilters` document.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct DialogFoldersDto {
    /// The layer's `messages.dialogFilters.tags_enabled` flag.
    pub(crate) tags_enabled: bool,
    pub(crate) filters: Vec<DialogFolderDto>,
}

/// Projects a whole `messages.dialogFilters` answer.
pub(crate) fn dialog_folders_dto(
    native: &NativeClient,
    filters: tl::types::messages::DialogFilters,
) -> Result<DialogFoldersDto, String> {
    let mut rows = Vec::with_capacity(filters.filters.len());
    for filter in &filters.filters {
        rows.push(dialog_folder_dto(native, filter)?);
    }
    Ok(DialogFoldersDto {
        tags_enabled: filters.tags_enabled,
        filters: rows,
    })
}

/// Projects one `DialogFilter` variant, resolving every peer it holds.
pub(crate) fn dialog_folder_dto(
    native: &NativeClient,
    filter: &tl::enums::DialogFilter,
) -> Result<DialogFolderDto, String> {
    Ok(match filter {
        tl::enums::DialogFilter::Filter(filter) => DialogFolderDto {
            id: filter.id,
            kind: "filter",
            title: title_text(&filter.title),
            contacts: filter.contacts,
            non_contacts: filter.non_contacts,
            groups: filter.groups,
            broadcasts: filter.broadcasts,
            bots: filter.bots,
            exclude_muted: filter.exclude_muted,
            exclude_read: filter.exclude_read,
            exclude_archived: filter.exclude_archived,
            has_my_invites: false,
            pinned_peers: peers_dto(native, &filter.pinned_peers)?,
            include_peers: peers_dto(native, &filter.include_peers)?,
            exclude_peers: peers_dto(native, &filter.exclude_peers)?,
        },
        tl::enums::DialogFilter::Chatlist(chatlist) => DialogFolderDto {
            id: chatlist.id,
            kind: "chatlist",
            title: title_text(&chatlist.title),
            contacts: false,
            non_contacts: false,
            groups: false,
            broadcasts: false,
            bots: false,
            exclude_muted: false,
            exclude_read: false,
            exclude_archived: false,
            has_my_invites: chatlist.has_my_invites,
            pinned_peers: peers_dto(native, &chatlist.pinned_peers)?,
            include_peers: peers_dto(native, &chatlist.include_peers)?,
            // `dialogFilterChatlist` carries no exclude list.
            exclude_peers: Vec::new(),
        },
        tl::enums::DialogFilter::Default => DialogFolderDto {
            // `dialogFilterDefault` is an empty marker with no id, title or peers of its own.
            id: 0,
            kind: "default",
            title: String::new(),
            contacts: false,
            non_contacts: false,
            groups: false,
            broadcasts: false,
            bots: false,
            exclude_muted: false,
            exclude_read: false,
            exclude_archived: false,
            has_my_invites: false,
            pinned_peers: Vec::new(),
            include_peers: Vec::new(),
            exclude_peers: Vec::new(),
        },
    })
}

/// The plain text of a layer `TextWithEntities` title.
fn title_text(title: &tl::enums::TextWithEntities) -> String {
    let tl::enums::TextWithEntities::Entities(value) = title;
    value.text.clone()
}

/// Resolves each layer input peer through the client and registers it.
///
/// A filter's peer list carries no user or chat objects, so unlike the contacts projections there
/// is nothing local to resolve against: `resolve_peer` fetches the full peer and gives it an access
/// hash, which is what makes the registered handle usable again.
fn peers_dto(
    native: &NativeClient,
    peers: &[tl::enums::InputPeer],
) -> Result<Vec<PeerDto>, String> {
    let mut rows = Vec::with_capacity(peers.len());
    for peer in peers {
        // `InputPeer::Empty` names no peer and cannot be converted; a filter never holds one.
        if matches!(peer, tl::enums::InputPeer::Empty) {
            continue;
        }
        let resolved = native
            .runtime
            .block_on(native.client.resolve_peer(peer.clone()))
            .map_err(invocation_error)?;
        rows.push(peer_dto(native, &resolved)?);
    }
    Ok(rows)
}

#[cfg(test)]
mod tests {
    //! Wire-contract tests for the folder documents.
    //!
    //! Like the other DTO tests, the projection itself needs a live grammers client, so what is
    //! pinned here is the exact document each constructor encodes.

    use super::*;
    use crate::dto::permissions::ChatPermissionsDto;
    use serde::Serialize;

    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    fn peer() -> PeerDto {
        PeerDto {
            native_handle: 12,
            id: 7,
            kind: "user",
            username: Some("someone".to_owned()),
            name: Some("Some One".to_owned()),
            usernames: Vec::new(),
            is_megagroup: None,
            has_photo: false,
            permissions: None,
        }
    }

    fn folder() -> DialogFolderDto {
        DialogFolderDto {
            id: 2,
            kind: "filter",
            title: "News".to_owned(),
            contacts: true,
            non_contacts: false,
            groups: true,
            broadcasts: false,
            bots: false,
            exclude_muted: true,
            exclude_read: false,
            exclude_archived: true,
            has_my_invites: false,
            pinned_peers: vec![peer()],
            include_peers: Vec::new(),
            exclude_peers: Vec::new(),
        }
    }

    #[test]
    fn a_folder_document_carries_the_flags_and_the_peers() {
        assert_json(
            &folder(),
            serde_json::json!({
                "id": 2,
                "kind": "filter",
                "title": "News",
                "contacts": true,
                "nonContacts": false,
                "groups": true,
                "broadcasts": false,
                "bots": false,
                "excludeMuted": true,
                "excludeRead": false,
                "excludeArchived": true,
                "hasMyInvites": false,
                "pinnedPeers": [serde_json::to_value(peer()).unwrap()],
                "includePeers": [],
                "excludePeers": [],
            }),
        );
    }

    #[test]
    fn a_folders_document_carries_the_tags_flag_and_the_list() {
        assert_json(
            &DialogFoldersDto {
                tags_enabled: true,
                filters: vec![folder()],
            },
            serde_json::json!({
                "tagsEnabled": true,
                "filters": [serde_json::to_value(folder()).unwrap()],
            }),
        );
    }

    #[test]
    fn an_empty_folders_document_still_carries_the_shape() {
        assert_json(
            &DialogFoldersDto {
                tags_enabled: false,
                filters: Vec::new(),
            },
            serde_json::json!({
                "tagsEnabled": false,
                "filters": [],
            }),
        );
    }

    #[test]
    fn a_chatlist_folder_reports_its_kind_and_no_excludes() {
        assert_json(
            &DialogFolderDto {
                id: 4,
                kind: "chatlist",
                title: "Chats".to_owned(),
                contacts: false,
                non_contacts: false,
                groups: false,
                broadcasts: false,
                bots: false,
                exclude_muted: false,
                exclude_read: false,
                exclude_archived: false,
                has_my_invites: true,
                pinned_peers: Vec::new(),
                include_peers: vec![PeerDto {
                    native_handle: 3,
                    id: -1_000_007,
                    kind: "channel",
                    username: None,
                    name: Some("A Channel".to_owned()),
                    usernames: Vec::new(),
                    is_megagroup: None,
                    has_photo: false,
                    permissions: Some(ChatPermissionsDto::default()),
                }],
                exclude_peers: Vec::new(),
            },
            serde_json::json!({
                "id": 4,
                "kind": "chatlist",
                "title": "Chats",
                "contacts": false,
                "nonContacts": false,
                "groups": false,
                "broadcasts": false,
                "bots": false,
                "excludeMuted": false,
                "excludeRead": false,
                "excludeArchived": false,
                "hasMyInvites": true,
                "pinnedPeers": [],
                "includePeers": [{
                    "nativeHandle": 3,
                    "id": -1_000_007,
                    "kind": "channel",
                    "username": null,
                    "name": "A Channel",
                    "usernames": [],
                    "isMegagroup": null,
                    "hasPhoto": false,
                    "permissions": serde_json::to_value(ChatPermissionsDto::default()).unwrap(),
                }],
                "excludePeers": [],
            }),
        );
    }
}
