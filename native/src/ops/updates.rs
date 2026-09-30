//! Update-stream operations.

use std::sync::MutexGuard;
use std::time::Duration;

use grammers_client::client::UpdateStream;
use grammers_client::tl;
use serde::{Deserialize, Serialize};
use serde_json::json;

use super::Handler;
use crate::client::NativeClient;
use crate::dto::peer::{peer_dto, PeerDto};
use crate::dto::update::{
    raw_update_dto, update_dto, update_state_dto, RawUpdateDto, UpdateStateDto,
};
use crate::error::{invocation_error, json_string, parse_payload};
use crate::ops::inline::{article_result, InlineArticleSpec};

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

/// Payload of `answerGuestChatQuery`: the query to answer and the article result to send.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct AnswerGuestChatQueryPayload {
    query_id: i64,
    result: InlineArticleSpec,
}

/// The identifier of the inline message a guest-chat answer produced, which is what
/// `messages.SetBotGuestChatResult` returns.
///
/// The layer has two constructors: a 32-bit one and a 64-bit one that also carries the owner. The
/// projection reports the owner only when the 64-bit constructor is the one returned.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct GuestChatAnswerResult {
    dc_id: i32,
    id: i64,
    access_hash: i64,
    owner_id: Option<i64>,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "nextUpdate",
    "nextRawUpdate",
    "syncUpdateState",
    "answerGuestChatQuery",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "nextUpdate" => next_update,
        "nextRawUpdate" => next_raw_update,
        "syncUpdateState" => sync_update_state,
        "answerGuestChatQuery" => answer_guest_chat_query,
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
    native
        .runtime
        .block_on(updates.sync_update_state())
        .map_err(|error| error.to_string())?;
    json_string(json!({ "ok": true }))
}

/// Answers a guest-chat query, which is `GuestChatQuery::answer`'s send with the article result
/// the caller chooses.
///
/// The request is `messages.SetBotGuestChatResult`, which is the send behind grammers' own
/// `GuestChatQuery::answer`; only the result is configurable here. The response is the identifier
/// of the inline message the answer produced, which a caller can edit later.
fn answer_guest_chat_query(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: AnswerGuestChatQueryPayload = parse_payload(payload)?;
    let result = article_result(&data.result)?;
    let request = tl::functions::messages::SetBotGuestChatResult {
        query_id: data.query_id,
        result,
    };
    let answer = native
        .runtime
        .block_on(native.client.invoke(&request))
        .map_err(invocation_error)?;
    json_string(guest_chat_answer_result(&answer))
}

/// Projects the `InputBotInlineMessageId` a guest-chat answer returns, reporting the owner only
/// when the 64-bit constructor is the one the layer chose.
fn guest_chat_answer_result(id: &tl::enums::InputBotInlineMessageId) -> GuestChatAnswerResult {
    GuestChatAnswerResult {
        dc_id: id.dc_id(),
        id: match id {
            tl::enums::InputBotInlineMessageId::Id(id) => id.id,
            tl::enums::InputBotInlineMessageId::Id64(id) => i64::from(id.id),
        },
        access_hash: id.access_hash(),
        owner_id: match id {
            tl::enums::InputBotInlineMessageId::Id(_) => None,
            tl::enums::InputBotInlineMessageId::Id64(id) => Some(id.owner_id),
        },
    }
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

#[cfg(test)]
mod tests {
    //! Tests for the request side: the payload the handler decodes and the result it projects.
    //!
    //! The handler itself needs a live Telegram session, so what is asserted here is everything
    //! between the payload and the grammers request.

    use grammers_client::tl;
    use serde_json::json;

    use super::{guest_chat_answer_result, AnswerGuestChatQueryPayload};
    use crate::error::parse_payload;

    /// Decodes the payload exactly as the handler does.
    fn answer(payload: serde_json::Value) -> AnswerGuestChatQueryPayload {
        parse_payload(&payload.to_string()).expect("a guest chat answer payload")
    }

    #[test]
    fn a_guest_chat_answer_payload_carries_the_query_id_and_the_result() {
        let payload = answer(json!({
            "queryId": 900,
            "result": {
                "title": "An article",
                "messageText": "hello",
            },
        }));
        assert_eq!(payload.query_id, 900);
        assert_eq!(payload.result.title, "An article");
        assert_eq!(payload.result.message_text, "hello");
    }

    #[test]
    fn a_guest_chat_answer_result_projects_the_32_bit_identifier() {
        let result = guest_chat_answer_result(&tl::enums::InputBotInlineMessageId::Id(
            tl::types::InputBotInlineMessageId {
                dc_id: 2,
                id: 88,
                access_hash: 77,
            },
        ));
        assert_eq!(result.dc_id, 2);
        assert_eq!(result.id, 88);
        assert_eq!(result.access_hash, 77);
        assert_eq!(result.owner_id, None);
    }

    #[test]
    fn a_guest_chat_answer_result_projects_the_64_bit_identifier_with_its_owner() {
        let result = guest_chat_answer_result(&tl::enums::InputBotInlineMessageId::Id64(
            tl::types::InputBotInlineMessageId64 {
                dc_id: 2,
                owner_id: -1_000_007,
                id: 88,
                access_hash: 77,
            },
        ));
        assert_eq!(result.dc_id, 2);
        assert_eq!(result.id, 88);
        assert_eq!(result.access_hash, 77);
        assert_eq!(result.owner_id, Some(-1_000_007));
    }
}
