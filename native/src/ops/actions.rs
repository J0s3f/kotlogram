//! Chat actions: the typing, uploading and recording statuses Telegram shows next to a chat, and
//! the service action a message carries.
//!
//! The sending half wraps grammers' [`ActionSender`], which reaches the layer through
//! `messages.setTyping`; the reading half projects `Message::action()`, which is the only place
//! grammers exposes a service action at all. The `repeat` loop of the same sender is left out: it
//! drives a Rust future, which a request/response bridge has no way to supply.

use grammers_client::peer::ActionSender;
use grammers_client::tl;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::action::message_action_dto;
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

/// The wire name of the status that clears the current action.
const CANCEL: &str = "cancel";

/// The statuses the layer carries a progress percentage for. The percentage is how far the upload
/// is along, and only the uploading statuses have one to report.
const PROGRESS_ACTIONS: &[&str] = &[
    "uploadPhoto",
    "uploadDocument",
    "uploadVideo",
    "uploadVoice",
    "uploadVideoNote",
    "historyImport",
];

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SendChatActionPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    action: String,
    progress: Option<i32>,
    topic_id: Option<i32>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GetMessageActionPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &["sendChatAction", "getMessageAction"];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "sendChatAction" => send_chat_action,
        "getMessageAction" => get_message_action,
        _ => return None,
    })
}

fn send_chat_action(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SendChatActionPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let mut sender: ActionSender = native.client.action(peer.clone());
    // grammers carries the forum topic on the sender rather than on the action, and leaves it
    // unset when no topic is asked for, which is how it targets the whole chat.
    if let Some(topic_id) = data.topic_id {
        sender = sender.topic_id(topic_id);
    }
    // A failed status is reported by name before the request goes out, which is why the mapping
    // runs first: grammers would only reject an unknown one over the wire.
    // `cancel` needs no case of its own here: grammers' `ActionSender::cancel` is `oneshot` of the
    // same variant, so mapping it here reaches the identical request.
    let action = send_message_action(&data.action, data.progress)?;
    native
        .runtime
        .block_on(sender.oneshot(action))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

fn get_message_action(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetMessageActionPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    // The action is a property of the message, so the message is fetched first. It is fetched
    // rather than projected from a message the caller already holds because no earlier result of
    // this bridge carries the raw layer value the projection reads.
    let message = native
        .runtime
        .block_on(native.client.get_messages_by_id(peer, &[data.message_id]))
        .map_err(invocation_error)?
        .into_iter()
        .next()
        .flatten()
        .ok_or_else(|| format!("message not found: {}", data.message_id))?;
    // The action is wrapped rather than returned on its own because an ordinary message has none,
    // and a bare `null` result is what this bridge reads as an error string.
    json_string(json!({ "action": message_action_dto(&message) }))
}

/// Maps a wire status name onto the layer's own `SendMessageAction` variant.
///
/// The names are the ones the Bot API uses, because that is what a Kotlin caller already knows,
/// with the layer's own names for the statuses the Bot API has no equivalent for. The three the
/// layer has and this list leaves out — the two emoji interactions and the text draft — each carry
/// a layer payload of their own, which is left to `invokeRaw`.
fn send_message_action(
    name: &str,
    progress: Option<i32>,
) -> Result<tl::enums::SendMessageAction, String> {
    if progress.is_some() && !PROGRESS_ACTIONS.contains(&name) {
        return Err(format!("{name} does not take a progress percentage"));
    }
    if let Some(progress) = progress {
        if !(0..=100).contains(&progress) {
            return Err(format!(
                "{name} takes a progress percentage between 0 and 100, not {progress}"
            ));
        }
    }
    // The uploading statuses show no progress until the caller supplies one.
    let progress = progress.unwrap_or(0);

    use tl::enums::SendMessageAction as Action;
    Ok(match name {
        "typing" => Action::SendMessageTypingAction,
        CANCEL => Action::SendMessageCancelAction,
        "recordVideo" => Action::SendMessageRecordVideoAction,
        "uploadVideo" => {
            Action::SendMessageUploadVideoAction(tl::types::SendMessageUploadVideoAction {
                progress,
            })
        }
        "recordVoice" => Action::SendMessageRecordAudioAction,
        "uploadVoice" => {
            Action::SendMessageUploadAudioAction(tl::types::SendMessageUploadAudioAction {
                progress,
            })
        }
        "uploadPhoto" => {
            Action::SendMessageUploadPhotoAction(tl::types::SendMessageUploadPhotoAction {
                progress,
            })
        }
        "uploadDocument" => {
            Action::SendMessageUploadDocumentAction(tl::types::SendMessageUploadDocumentAction {
                progress,
            })
        }
        "recordVideoNote" => Action::SendMessageRecordRoundAction,
        "uploadVideoNote" => {
            Action::SendMessageUploadRoundAction(tl::types::SendMessageUploadRoundAction {
                progress,
            })
        }
        "chooseSticker" => Action::SendMessageChooseStickerAction,
        "chooseContact" => Action::SendMessageChooseContactAction,
        "geoLocation" => Action::SendMessageGeoLocationAction,
        "gamePlay" => Action::SendMessageGamePlayAction,
        "historyImport" => {
            Action::SendMessageHistoryImportAction(tl::types::SendMessageHistoryImportAction {
                progress,
            })
        }
        "speakingInGroupCall" => Action::SpeakingInGroupCallAction,
        _ => return Err(format!("unsupported chat action: {name}")),
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use tl::enums::SendMessageAction as Action;

    #[test]
    fn every_status_maps_to_the_variant_the_layer_documents() {
        let cases = [
            ("typing", Action::SendMessageTypingAction),
            (CANCEL, Action::SendMessageCancelAction),
            ("recordVideo", Action::SendMessageRecordVideoAction),
            ("recordVoice", Action::SendMessageRecordAudioAction),
            ("recordVideoNote", Action::SendMessageRecordRoundAction),
            ("chooseSticker", Action::SendMessageChooseStickerAction),
            ("chooseContact", Action::SendMessageChooseContactAction),
            ("geoLocation", Action::SendMessageGeoLocationAction),
            ("gamePlay", Action::SendMessageGamePlayAction),
            ("speakingInGroupCall", Action::SpeakingInGroupCallAction),
        ];
        for (name, expected) in cases {
            assert_eq!(send_message_action(name, None), Ok(expected), "{name}");
        }
    }

    #[test]
    fn an_uploading_status_carries_the_progress_it_was_given() {
        let Action::SendMessageUploadDocumentAction(action) =
            send_message_action("uploadDocument", Some(42)).expect("an upload status")
        else {
            panic!("uploadDocument must map to the document upload status");
        };
        assert_eq!(action.progress, 42);
    }

    #[test]
    fn an_uploading_status_without_a_progress_starts_at_zero() {
        let Action::SendMessageUploadPhotoAction(action) =
            send_message_action("uploadPhoto", None).expect("an upload status")
        else {
            panic!("uploadPhoto must map to the photo upload status");
        };
        assert_eq!(action.progress, 0);
    }

    #[test]
    fn only_the_uploading_statuses_take_a_progress_percentage() {
        assert!(send_message_action("typing", Some(10)).is_err());
        assert!(send_message_action("chooseSticker", Some(10)).is_err());
        assert!(send_message_action(CANCEL, Some(10)).is_err());
        assert!(send_message_action("uploadVoice", Some(10)).is_ok());
        assert!(send_message_action("historyImport", Some(10)).is_ok());
    }

    #[test]
    fn a_progress_percentage_outside_the_range_is_rejected() {
        let error = send_message_action("uploadDocument", Some(101)).expect_err("101 percent");
        assert!(error.contains("between 0 and 100"), "{error}");
        assert!(send_message_action("uploadDocument", Some(100)).is_ok());
    }

    #[test]
    fn an_unknown_status_is_rejected_by_name() {
        let error = send_message_action("teleporting", None).expect_err("no such status");
        assert_eq!(error, "unsupported chat action: teleporting");
    }
}
