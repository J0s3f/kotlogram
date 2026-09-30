//! Dialog projection.

use grammers_client::peer::Dialog as ClientDialog;
use grammers_client::tl;
use serde::Serialize;

use crate::client::NativeClient;
use crate::dto::message::{message_dto, MessageDto};
use crate::dto::peer::{peer_dto, PeerDto};

/// A dialog with its most recent message.
///
/// grammers' [`ClientDialog`] types a dialog as only its peer and its last message; the state
/// below comes from the `raw` layer payload it publishes, which is the only place these fields
/// exist.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct DialogDto {
    pub(crate) peer: PeerDto,
    pub(crate) last_message: Option<MessageDto>,
    pub(crate) pinned: bool,
    /// The identifier of the newest message counted for this dialog.
    pub(crate) top_message: i32,
    /// `None` for a folder row, which only carries folder-wide unread counts.
    pub(crate) unread_count: Option<i32>,
    /// `None` for a folder row, which has no mentions of its own.
    pub(crate) unread_mentions_count: Option<i32>,
    /// The unsent text of the current draft, if the dialog has one.
    pub(crate) draft_text: Option<String>,
    /// The Telegram folder the dialog sits in, if any.
    pub(crate) folder_id: Option<i32>,
    /// True when this entry is a per-folder row rather than a chat.
    pub(crate) is_folder: bool,
}

/// Projects a grammers dialog, registering its peer so Kotlin can send messages back to it.
pub(crate) fn dialog_dto(
    native: &NativeClient,
    dialog: &ClientDialog,
) -> Result<DialogDto, String> {
    let peer = peer_dto(native, &dialog.peer)?;

    let (pinned, top_message, unread_count, unread_mentions_count, draft_text, folder_id) =
        match &dialog.raw {
            tl::enums::Dialog::Dialog(raw) => (
                raw.pinned,
                raw.top_message,
                Some(raw.unread_count),
                Some(raw.unread_mentions_count),
                raw.draft.as_ref().and_then(|draft| match draft {
                    tl::enums::DraftMessage::Message(draft) => Some(draft.message.clone()),
                    tl::enums::DraftMessage::Empty(_) => None,
                }),
                raw.folder_id,
            ),
            tl::enums::Dialog::Folder(raw) => (raw.pinned, raw.top_message, None, None, None, None),
            // A community is a folder-like row: it carries a pinned flag and nothing else.
            tl::enums::Dialog::Community(raw) => (raw.pinned, 0, None, None, None, None),
        };

    Ok(DialogDto {
        peer,
        last_message: dialog.last_message.as_ref().map(message_dto),
        pinned,
        top_message,
        unread_count,
        unread_mentions_count,
        draft_text,
        folder_id,
        is_folder: matches!(dialog.raw, tl::enums::Dialog::Folder(_)),
    })
}
