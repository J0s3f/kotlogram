//! Dialog listing and read-state operations.

use grammers_client::client::DialogIter;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::dialog::dialog_dto;
use crate::dto::dialog_meta::dialog_with_meta_dto;
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

#[cfg_attr(not(test), allow(dead_code))]
/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "getDialogs",
    "getDialogsMeta",
    "getDialogsTotal",
    "markAsRead",
    "clearMentions",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "getDialogs" => get_dialogs,
        "getDialogsMeta" => get_dialogs_meta,
        "getDialogsTotal" => get_dialogs_total,
        "markAsRead" => mark_as_read,
        "clearMentions" => clear_mentions,
        _ => return None,
    })
}

/// Payload of `getDialogs` and `getDialogsMeta`: the page size and, to resume an earlier listing,
/// the cursor that names the position to resume after.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct DialogsPayload {
    limit: Option<usize>,
    /// Paging cursor: resume after the dialog with this peer. One of three values that travel
    /// together; see [DialogsCursor].
    offset_peer: Option<i64>,
    /// Paging cursor: resume after this message id.
    offset_id: Option<i32>,
    /// Paging cursor: resume after this date, in epoch milliseconds.
    offset_date: Option<i64>,
}

/// The dialog paging cursor: the position a listing resumes after.
///
/// grammers pages dialogs with three values together — its `messages.GetDialogs` request carries
/// `offset_peer`, `offset_id` and `offset_date`, and its `DialogIter` advances the three as one
/// from the last dialog of each page. A cursor is therefore all three or none: a partial one
/// would leave the missing values at zero, which Telegram reads as "start from the top" and so
/// silently re-serves the first page.
struct DialogsCursor {
    /// The peer to resume after, as a Bot API dialog id (the `PeerDto::id` the listing reports).
    peer: i64,
    /// The id of the last message of the dialog to resume after.
    id: i32,
    /// The date of that message, in epoch milliseconds.
    date: i64,
}

impl DialogsCursor {
    /// Reads the cursor off the payload, or `None` when any of the three values is absent.
    fn from_payload(payload: &DialogsPayload) -> Option<Self> {
        Some(Self {
            peer: payload.offset_peer?,
            id: payload.offset_id?,
            date: payload.offset_date?,
        })
    }

    /// True when a dialog is the one this cursor resumes after: the same peer, and the same last
    /// message (id and date) the cursor was read from. [last_message] is the dialog's last message
    /// as `(id, epoch milliseconds)`, or `None` when the dialog has none.
    fn matches(&self, peer_id: Option<i64>, last_message: Option<(i32, i64)>) -> bool {
        peer_id == Some(self.peer) && last_message == Some((self.id, self.date))
    }
}

/// Consumes dialogs until the cursor's dialog has been passed, so the next page starts after it.
///
/// grammers' `DialogIter` keeps its paging cursor in `pub(crate)` request fields with no public
/// setter, so the cursor cannot be handed to the iterator directly. Instead the iterator is
/// consumed up to the cursor's position — exactly the position the iterator itself advances to
/// after serving a page — and the dialogs that follow form the next page. A cursor that matches
/// no dialog (the list changed since it was issued) simply ends the listing.
async fn skip_past_cursor(iterator: &mut DialogIter, cursor: &DialogsCursor) -> Result<(), String> {
    while let Some(dialog) = iterator.next().await.map_err(invocation_error)? {
        if cursor.matches(
            dialog.peer.id().bot_api_dialog_id(),
            dialog
                .last_message
                .as_ref()
                .map(|message| (message.id(), message.date().as_millisecond())),
        ) {
            return Ok(());
        }
    }
    Ok(())
}

fn get_dialogs(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: DialogsPayload = parse_payload(payload)?;
    let limit = data.limit.unwrap_or(50).clamp(1, 100);
    let cursor = DialogsCursor::from_payload(&data);
    let dialogs = native.runtime.block_on(async {
        let mut iterator = native.client.iter_dialogs().limit(limit);
        if let Some(cursor) = &cursor {
            skip_past_cursor(&mut iterator, cursor).await?;
        }
        let mut result = Vec::new();
        while let Some(dialog) = iterator.next().await.map_err(invocation_error)? {
            result.push(dialog_dto(native, &dialog)?);
        }
        Ok::<_, String>(result)
    })?;
    json_string(dialogs)
}

/// Lists dialogs with the state the [`getDialogs`] projection leaves out attached under `meta`.
///
/// grammers' `Client::iter_dialogs` yields the same rows as [`getDialogs`]; only the projection
/// differs, so the operation is a separate one rather than a change to the pinned listing shape.
fn get_dialogs_meta(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: DialogsPayload = parse_payload(payload)?;
    let limit = data.limit.unwrap_or(50).clamp(1, 100);
    let cursor = DialogsCursor::from_payload(&data);
    let dialogs = native.runtime.block_on(async {
        let mut iterator = native.client.iter_dialogs().limit(limit);
        if let Some(cursor) = &cursor {
            skip_past_cursor(&mut iterator, cursor).await?;
        }
        let mut result = Vec::new();
        while let Some(dialog) = iterator.next().await.map_err(invocation_error)? {
            result.push(dialog_with_meta_dto(native, &dialog)?);
        }
        Ok::<_, String>(result)
    })?;
    json_string(dialogs)
}

/// Counts the dialogs the account has, which is grammers' `DialogIter::total`.
///
/// The count performs no dialog fetch of its own once the iterator has one, and the iterator is
/// fresh here, so it is the one network call.
fn get_dialogs_total(native: &NativeClient, _payload: &str) -> Result<String, String> {
    let total = native.runtime.block_on(async {
        let mut dialogs = native.client.iter_dialogs();
        dialogs.total().await.map_err(invocation_error)
    })?;
    json_string(json!({ "total": total }))
}

fn mark_as_read(native: &NativeClient, payload: &str) -> Result<String, String> {
    let target: PeerTarget = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &target))?;
    native
        .runtime
        .block_on(native.client.mark_as_read(peer))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Clears the account's pending mentions in a peer, which is grammers' `Client::clear_mentions`.
fn clear_mentions(native: &NativeClient, payload: &str) -> Result<String, String> {
    let target: PeerTarget = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &target))?;
    native
        .runtime
        .block_on(native.client.clear_mentions(peer))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

#[cfg(test)]
mod tests {
    //! Tests for the module's own operation inventory.
    //!
    //! The handlers themselves need a live Telegram session, so what is asserted here is that the
    //! names the bridge annotates and `native/operations.txt` carries all route out of this module.

    use super::{route, DialogsCursor, DialogsPayload, OPERATIONS};
    use crate::error::parse_payload;

    #[test]
    fn a_full_dialog_cursor_decodes_from_the_payload() {
        let data: DialogsPayload = parse_payload(
            r#"{"limit":25,"offsetPeer":-1000007,"offsetId":31,"offsetDate":1700000000000}"#,
        )
        .expect("a dialogs payload");

        assert_eq!(data.limit, Some(25));
        let cursor = DialogsCursor::from_payload(&data).expect("a full cursor");
        assert_eq!(cursor.peer, -100_000_7);
        assert_eq!(cursor.id, 31);
        assert_eq!(cursor.date, 1_700_000_000_000);
    }

    #[test]
    fn a_partial_dialog_cursor_is_ignored() {
        // Any one of the three missing means no cursor at all: the listing starts from the top,
        // because a partial cursor would silently re-serve the first page.
        for payload in [
            r#"{"limit":25}"#,
            r#"{"offsetPeer":-1000007}"#,
            r#"{"offsetId":31}"#,
            r#"{"offsetDate":1700000000000}"#,
            r#"{"offsetPeer":-1000007,"offsetId":31}"#,
            r#"{"offsetPeer":-1000007,"offsetDate":1700000000000}"#,
            r#"{"offsetId":31,"offsetDate":1700000000000}"#,
        ] {
            let data: DialogsPayload = parse_payload(payload).expect("a dialogs payload");
            assert!(
                DialogsCursor::from_payload(&data).is_none(),
                "{payload} is a partial cursor and must be ignored"
            );
        }
    }

    #[test]
    fn a_cursor_matches_the_dialog_it_was_read_from() {
        let cursor = DialogsCursor {
            peer: -100_000_7,
            id: 31,
            date: 1_700_000_000_000,
        };
        // The dialog the cursor names: same peer, same last message id and date.
        assert!(cursor.matches(Some(-100_000_7), Some((31, 1_700_000_000_000))));
        // A different message id, a different date, a different peer, or no last message at all
        // is a different position.
        assert!(!cursor.matches(Some(-100_000_7), Some((30, 1_700_000_000_000))));
        assert!(!cursor.matches(Some(-100_000_7), Some((31, 1_700_000_000_001))));
        assert!(!cursor.matches(Some(7), Some((31, 1_700_000_000_000))));
        assert!(!cursor.matches(Some(-100_000_7), None));
        assert!(!cursor.matches(None, Some((31, 1_700_000_000_000))));
    }

    #[test]
    fn every_dialog_operation_is_declared_and_routed() {
        for operation in [
            "getDialogs",
            "getDialogsMeta",
            "getDialogsTotal",
            "markAsRead",
            "clearMentions",
        ] {
            assert!(
                OPERATIONS.contains(&operation),
                "{operation} is not declared by this module"
            );
            assert!(route(operation).is_some(), "{operation} is not routed");
        }
    }

    #[test]
    fn an_unknown_name_does_not_route_here() {
        assert!(route("notADialogOperation").is_none());
    }
}
