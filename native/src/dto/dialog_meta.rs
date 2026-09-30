//! Dialog metadata projection.
//!
//! grammers types a dialog as only its peer and its last message; the state the listing projection
//! ([`crate::dto::dialog::DialogDto`]) deliberately leaves out — the read markers, the unread
//! reaction count, the notification settings and, for a folder row, the folder's own counters —
//! lives only in the raw layer payload the dialog publishes.
//!
//! The listing projection is pinned field by field and must stay stable, so this state travels as
//! its own document: [`dialog_with_meta_dto`] flattens the listing projection and attaches one
//! [`DialogMetaDto`] under `meta`. A dialog the listing produced without asking for the metadata
//! simply carries no `meta`.

use grammers_client::peer::Dialog as ClientDialog;
use grammers_client::tl;
use serde::Serialize;

use crate::client::NativeClient;
use crate::dto::dialog::{dialog_dto, DialogDto};

/// The notification settings of a dialog, from the layer's `PeerNotifySettings`.
///
/// The layer also carries per-platform sounds and story flags, which grammers exposes through no
/// accessor and this projection has no shape for; only the three settings a client reads are
/// carried.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct DialogNotifySettingsDto {
    pub(crate) show_previews: Option<bool>,
    pub(crate) silent: Option<bool>,
    /// Epoch milliseconds, converted from the layer's unix-second `mute_until`.
    pub(crate) mute_until: Option<i64>,
}

/// The dialog state the listing projection does not carry.
///
/// A regular dialog populates the first block and leaves the folder counters unset; a folder row
/// does the opposite. Which one a document came from is readable from `isFolder` on the listing
/// projection it is flattened into.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct DialogMetaDto {
    /// The dialog is marked as unread by the account.
    pub(crate) unread_mark: Option<bool>,
    /// Show the forum's topics as ordinary messages.
    pub(crate) view_forum_as_messages: Option<bool>,
    pub(crate) read_inbox_max_id: Option<i32>,
    pub(crate) read_outbox_max_id: Option<i32>,
    /// Unread reactions, which the unread count the listing carries does not include.
    pub(crate) unread_reactions_count: Option<i32>,
    pub(crate) notify_settings: Option<DialogNotifySettingsDto>,
    /// The update state of the dialog, when the layer carries one.
    pub(crate) pts: Option<i32>,
    /// The time messages in the dialog are kept, in seconds; `None` when nothing expires.
    pub(crate) ttl_period: Option<i32>,
    /// The folder's title, for a folder row.
    pub(crate) folder_title: Option<String>,
    pub(crate) autofill_new_broadcasts: Option<bool>,
    pub(crate) autofill_public_groups: Option<bool>,
    pub(crate) autofill_new_correspondents: Option<bool>,
    pub(crate) unread_muted_peers_count: Option<i32>,
    pub(crate) unread_unmuted_peers_count: Option<i32>,
    pub(crate) unread_muted_messages_count: Option<i32>,
    pub(crate) unread_unmuted_messages_count: Option<i32>,
}

/// A listing projection with its dialog metadata attached under `meta`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct DialogWithMetaDto {
    #[serde(flatten)]
    pub(crate) dialog: DialogDto,
    pub(crate) meta: DialogMetaDto,
}

/// Projects a grammers dialog and the metadata the listing projection leaves out.
///
/// The peer is registered once, through the listing projection.
pub(crate) fn dialog_with_meta_dto(
    native: &NativeClient,
    dialog: &ClientDialog,
) -> Result<DialogWithMetaDto, String> {
    Ok(DialogWithMetaDto {
        dialog: dialog_dto(native, dialog)?,
        meta: dialog_meta_dto(&dialog.raw),
    })
}

/// Reads the metadata off the raw layer payload.
fn dialog_meta_dto(raw: &tl::enums::Dialog) -> DialogMetaDto {
    match raw {
        tl::enums::Dialog::Dialog(dialog) => DialogMetaDto {
            unread_mark: Some(dialog.unread_mark),
            view_forum_as_messages: Some(dialog.view_forum_as_messages),
            read_inbox_max_id: Some(dialog.read_inbox_max_id),
            read_outbox_max_id: Some(dialog.read_outbox_max_id),
            unread_reactions_count: Some(dialog.unread_reactions_count),
            notify_settings: Some(notify_settings_dto(&dialog.notify_settings)),
            pts: dialog.pts,
            ttl_period: dialog.ttl_period,
            folder_title: None,
            autofill_new_broadcasts: None,
            autofill_public_groups: None,
            autofill_new_correspondents: None,
            unread_muted_peers_count: None,
            unread_unmuted_peers_count: None,
            unread_muted_messages_count: None,
            unread_unmuted_messages_count: None,
        },
        tl::enums::Dialog::Folder(folder) => {
            let (folder_title, broadcasts, groups, correspondents) = match &folder.folder {
                tl::enums::Folder::Folder(folder) => (
                    Some(folder.title.clone()),
                    Some(folder.autofill_new_broadcasts),
                    Some(folder.autofill_public_groups),
                    Some(folder.autofill_new_correspondents),
                ),
            };
            DialogMetaDto {
                unread_mark: None,
                view_forum_as_messages: None,
                read_inbox_max_id: None,
                read_outbox_max_id: None,
                unread_reactions_count: None,
                notify_settings: None,
                pts: None,
                ttl_period: None,
                folder_title,
                autofill_new_broadcasts: broadcasts,
                autofill_public_groups: groups,
                autofill_new_correspondents: correspondents,
                unread_muted_peers_count: Some(folder.unread_muted_peers_count),
                unread_unmuted_peers_count: Some(folder.unread_unmuted_peers_count),
                unread_muted_messages_count: Some(folder.unread_muted_messages_count),
                unread_unmuted_messages_count: Some(folder.unread_unmuted_messages_count),
            }
        }
        // A community is a new, folder-like row with no chat metadata of its own.
        tl::enums::Dialog::Community(_) => DialogMetaDto {
            unread_mark: None,
            view_forum_as_messages: None,
            read_inbox_max_id: None,
            read_outbox_max_id: None,
            unread_reactions_count: None,
            notify_settings: None,
            pts: None,
            ttl_period: None,
            folder_title: None,
            autofill_new_broadcasts: None,
            autofill_public_groups: None,
            autofill_new_correspondents: None,
            unread_muted_peers_count: None,
            unread_unmuted_peers_count: None,
            unread_muted_messages_count: None,
            unread_unmuted_messages_count: None,
        },
    }
}

/// Projects the layer's notification settings.
fn notify_settings_dto(settings: &tl::enums::PeerNotifySettings) -> DialogNotifySettingsDto {
    match settings {
        tl::enums::PeerNotifySettings::Settings(settings) => DialogNotifySettingsDto {
            show_previews: settings.show_previews,
            silent: settings.silent,
            mute_until: settings
                .mute_until
                .map(|seconds| i64::from(seconds) * 1_000),
        },
    }
}

#[cfg(test)]
mod tests {
    //! Wire-contract tests for the dialog-metadata projection.
    //!
    //! The metadata only exists in the raw layer payload, which the operations can reach only
    //! through a live session, so the projection is built by hand straight off the layer types and
    //! pinned as an exact JSON document.

    use grammers_client::tl;
    use serde_json::json;

    use crate::dto::dialog::DialogDto;
    use crate::dto::peer::PeerDto;

    use super::{dialog_meta_dto, DialogWithMetaDto};

    /// A layer peer the projections below reuse; only the variant matters for the metadata.
    fn raw_peer() -> tl::enums::Peer {
        tl::enums::Peer::User(tl::types::PeerUser { user_id: 7 })
    }

    /// The raw regular-dialog row the first test reads its metadata from.
    fn raw_dialog() -> tl::enums::Dialog {
        tl::enums::Dialog::Dialog(tl::types::Dialog {
            pinned: true,
            unread_mark: true,
            view_forum_as_messages: true,
            peer: raw_peer(),
            top_message: 31,
            read_inbox_max_id: 30,
            read_outbox_max_id: 29,
            unread_count: 2,
            unread_mentions_count: 1,
            unread_reactions_count: 3,
            unread_poll_votes_count: 0,
            notify_settings: tl::enums::PeerNotifySettings::Settings(
                tl::types::PeerNotifySettings {
                    show_previews: Some(false),
                    silent: Some(true),
                    mute_until: Some(1_700_000_000),
                    ios_sound: None,
                    android_sound: None,
                    other_sound: None,
                    stories_muted: None,
                    stories_hide_sender: None,
                    stories_ios_sound: None,
                    stories_android_sound: None,
                    stories_other_sound: None,
                },
            ),
            pts: Some(44),
            draft: None,
            folder_id: Some(1),
            ttl_period: Some(86_400),
        })
    }

    /// The raw folder row the second test reads its metadata from.
    fn raw_folder() -> tl::enums::Dialog {
        tl::enums::Dialog::Folder(tl::types::DialogFolder {
            pinned: true,
            folder: tl::enums::Folder::Folder(tl::types::Folder {
                autofill_new_broadcasts: true,
                autofill_public_groups: false,
                autofill_new_correspondents: true,
                id: 1,
                title: "News".to_owned(),
                photo: Some(tl::enums::ChatPhoto::Empty),
            }),
            peer: raw_peer(),
            top_message: 0,
            unread_muted_peers_count: 4,
            unread_unmuted_peers_count: 5,
            unread_muted_messages_count: 6,
            unread_unmuted_messages_count: 7,
        })
    }

    #[test]
    fn a_dialog_metadata_document_pins_every_field() {
        let actual =
            serde_json::to_value(dialog_meta_dto(&raw_dialog())).expect("metadata encodes");
        assert_eq!(
            actual,
            json!({
                "unreadMark": true,
                "viewForumAsMessages": true,
                "readInboxMaxId": 30,
                "readOutboxMaxId": 29,
                "unreadReactionsCount": 3,
                "notifySettings": {
                    "showPreviews": false,
                    "silent": true,
                    "muteUntil": 1_700_000_000_000i64,
                },
                "pts": 44,
                "ttlPeriod": 86_400,
                "folderTitle": null,
                "autofillNewBroadcasts": null,
                "autofillPublicGroups": null,
                "autofillNewCorrespondents": null,
                "unreadMutedPeersCount": null,
                "unreadUnmutedPeersCount": null,
                "unreadMutedMessagesCount": null,
                "unreadUnmutedMessagesCount": null,
            })
        );
    }

    #[test]
    fn a_folder_row_carries_the_folder_counters_and_no_notify_settings() {
        let actual =
            serde_json::to_value(dialog_meta_dto(&raw_folder())).expect("metadata encodes");
        assert_eq!(
            actual,
            json!({
                "unreadMark": null,
                "viewForumAsMessages": null,
                "readInboxMaxId": null,
                "readOutboxMaxId": null,
                "unreadReactionsCount": null,
                "notifySettings": null,
                "pts": null,
                "ttlPeriod": null,
                "folderTitle": "News",
                "autofillNewBroadcasts": true,
                "autofillPublicGroups": false,
                "autofillNewCorrespondents": true,
                "unreadMutedPeersCount": 4,
                "unreadUnmutedPeersCount": 5,
                "unreadMutedMessagesCount": 6,
                "unreadUnmutedMessagesCount": 7,
            })
        );
    }

    #[test]
    fn the_metadata_is_flattened_next_to_the_listing_projection() {
        let dialog = DialogDto {
            peer: PeerDto {
                native_handle: 12,
                id: 7,
                kind: "user",
                username: Some("someone".to_owned()),
                name: Some("Some One".to_owned()),
                usernames: Vec::new(),
                is_megagroup: None,
                has_photo: false,
                permissions: None,
            },
            last_message: None,
            pinned: true,
            top_message: 31,
            unread_count: Some(2),
            unread_mentions_count: Some(1),
            draft_text: None,
            folder_id: Some(1),
            is_folder: false,
        };
        let actual = serde_json::to_value(DialogWithMetaDto {
            dialog,
            meta: dialog_meta_dto(&raw_dialog()),
        })
        .expect("a dialog encodes");

        // The listing projection is inlined rather than nested, and `meta` sits beside it.
        assert!(actual.get("dialog").is_none());
        assert_eq!(actual["peer"]["nativeHandle"], json!(12));
        assert_eq!(actual["unreadCount"], json!(2));
        assert_eq!(actual["isFolder"], json!(false));
        assert_eq!(actual["meta"]["unreadMark"], json!(true));
        assert_eq!(
            actual["meta"]["notifySettings"]["muteUntil"],
            json!(1_700_000_000_000i64)
        );
    }
}
