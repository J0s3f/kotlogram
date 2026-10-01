//! Dialog listing and read-state operations.

use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::dialog::dialog_dto;
use crate::dto::dialog_meta::dialog_with_meta_dto;
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::{LimitPayload, PeerTarget};

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

fn get_dialogs(native: &NativeClient, payload: &str) -> Result<String, String> {
    let LimitPayload { limit } = parse_payload(payload)?;
    let limit = limit.unwrap_or(50).clamp(1, 100);
    let dialogs = native.runtime.block_on(async {
        let mut iterator = native.client.iter_dialogs().limit(limit);
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
    let LimitPayload { limit } = parse_payload(payload)?;
    let limit = limit.unwrap_or(50).clamp(1, 100);
    let dialogs = native.runtime.block_on(async {
        let mut iterator = native.client.iter_dialogs().limit(limit);
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

    use super::{route, OPERATIONS};

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
