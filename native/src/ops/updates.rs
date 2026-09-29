//! Update-stream operations.

use std::sync::MutexGuard;
use std::time::Duration;

use grammers_client::client::updates::UpdateStream;
use serde::{Deserialize, Serialize};
use serde_json::json;

use super::Handler;
use crate::client::NativeClient;
use crate::dto::peer::{peer_dto, PeerDto};
use crate::dto::update::{
    raw_update_dto, update_dto, update_state_dto, RawUpdateDto, UpdateStateDto,
};
use crate::error::{invocation_error, json_string, parse_payload};

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct NextUpdatePayload {
    timeout_millis: Option<u64>,
}

/// The raw update a stream delivered, with the state and the peers Telegram sent it with.
///
/// This is the whole of grammers' `UpdateStream::next_raw`, which is what a caller needs when the
/// typed [`UpdateDto`](crate::dto::update::UpdateDto) does not describe the event.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct RawUpdateResult {
    update: RawUpdateDto,
    state: UpdateStateDto,
    peers: Vec<PeerDto>,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &["nextUpdate", "nextRawUpdate", "syncUpdateState"];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "nextUpdate" => next_update,
        "nextRawUpdate" => next_raw_update,
        "syncUpdateState" => sync_update_state,
        _ => return None,
    })
}

fn next_update(native: &NativeClient, payload: &str) -> Result<String, String> {
    let timeout = timeout_of(payload)?;
    let mut updates = stream(native)?;
    let update = native.runtime.block_on(async {
        match tokio::time::timeout(timeout, updates.next()).await {
            Ok(Ok(update)) => Ok(Some(update)),
            Ok(Err(error)) => Err(invocation_error(error)),
            Err(_) => Ok(None),
        }
    })?;
    let update = update
        .map(|update| update_dto(native, &update))
        .transpose()?;
    json_string(json!({ "update": update }))
}

fn next_raw_update(native: &NativeClient, payload: &str) -> Result<String, String> {
    let timeout = timeout_of(payload)?;
    let mut updates = stream(native)?;
    let update = native.runtime.block_on(async {
        match tokio::time::timeout(timeout, updates.next_raw()).await {
            Ok(Ok(update)) => Ok(Some(update)),
            Ok(Err(error)) => Err(invocation_error(error)),
            Err(_) => Ok(None),
        }
    })?;
    let update = match update {
        Some((update, state, peers)) => {
            // The peer map is a hash map, so its order is whatever the hash gave; sorting by id
            // makes the document stable for a caller that compares it.
            let mut projected = Vec::new();
            for peer in peers.iter_peers() {
                projected.push(peer_dto(native, peer)?);
            }
            projected.sort_by_key(|peer| peer.id);
            Some(RawUpdateResult {
                update: raw_update_dto(&update),
                state: update_state_dto(&state),
                peers: projected,
            })
        }
        None => None,
    };
    json_string(json!({ "update": update }))
}

fn sync_update_state(native: &NativeClient, _payload: &str) -> Result<String, String> {
    let updates = stream(native)?;
    // grammers writes the state again when the stream is dropped; doing it on demand lets a client
    // persist it before the session is closed, or without closing at all.
    updates.sync_update_state();
    json_string(json!({ "ok": true }))
}

/// The one update stream of a client, which every streaming operation polls in turn.
fn stream(native: &NativeClient) -> Result<MutexGuard<'_, UpdateStream>, String> {
    native
        .updates
        .lock()
        .map_err(|_| "update stream is poisoned".to_owned())
}

/// The wait a streaming operation applies, taken from the `timeoutMillis` its payload carries.
fn timeout_of(payload: &str) -> Result<Duration, String> {
    let data: NextUpdatePayload = parse_payload(payload)?;
    Ok(Duration::from_millis(
        data.timeout_millis.unwrap_or(30_000).clamp(1, 60_000),
    ))
}
