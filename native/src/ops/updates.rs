//! Update-stream operations.

use std::time::Duration;

use grammers_client::tl;
use grammers_client::update::Update;
use serde::{Deserialize, Serialize};
use serde_json::json;

use super::Handler;
use crate::client::NativeClient;
use crate::dto::peer::{peer_dto, PeerDto};
use crate::dto::update::{
    raw_update_dto, update_dto, update_state_dto, RawUpdateDto, UpdateDto, UpdateStateDto,
};
use crate::error::{invocation_error, json_string, parse_payload};
use crate::ops::inline::{article_result, InlineArticleSpec};
use crate::update_pump::PolledUpdate;

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

#[cfg_attr(not(test), allow(dead_code))]
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
    // The wait is on the pump's own channel, so giving up on it costs nothing: the update is already
    // built and buffered, and the next poll takes it. See [`crate::update_pump`] for why grammers'
    // own poll cannot be waited on this way.
    let polled = native.runtime.block_on(native.update_pump.next(timeout));
    let update = match polled {
        Some(polled) => Some(typed_dto(native, polled?)?),
        None => None,
    };
    json_string(json!({ "update": update }))
}

fn next_raw_update(native: &NativeClient, payload: &str) -> Result<String, String> {
    let timeout = timeout_of(payload)?;
    let polled = native.runtime.block_on(native.update_pump.next(timeout));
    let update = match polled {
        Some(polled) => {
            let PolledUpdate {
                update,
                state,
                peers,
            } = polled?;
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

/// Projects a polled update as the typed view, which is what grammers' `UpdateStream::next` does
/// with the output of `next_raw`.
///
/// Building the typed payload here rather than in the pump is what lets the two read operations
/// share one stream: the pump hands the same raw triple to whichever of them reads next, so a caller
/// alternating them alternates between the two views of the same updates instead of racing for the
/// stream.
fn typed_dto(native: &NativeClient, polled: PolledUpdate) -> Result<UpdateDto, String> {
    let PolledUpdate {
        update,
        state,
        peers,
    } = polled;
    update_dto(
        native,
        &Update::from_raw(&native.client, update, state, peers),
    )
}

fn sync_update_state(native: &NativeClient, _payload: &str) -> Result<String, String> {
    // grammers only lends its stream immutably for this, and a poll needs it mutably for as long as
    // it waits for Telegram, so the pump carries the request out between two polls. A client that
    // wants to persist the state without closing the session asks for it here; on a quiet stream the
    // request waits for the next update, bounded by the pump so a JVM thread is never blocked for as
    // long as Telegram takes to speak.
    native.update_pump.sync_update_state(&native.runtime)?;
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
