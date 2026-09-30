//! Inline bots: asking a bot for inline results, answering the bot updates that carry a query,
//! and editing the inline message a chosen result produced.
//!
//! grammers splits this domain in two. The user half lives in `client/bots.rs`, where
//! `Client::inline_query` returns an iterator over the raw result values and `edit_inline_message`
//! edits a message by its layer identifier. The bot half lives on the typed updates, where
//! `CallbackQuery::answer`, `InlineQuery::answer` and `InlineSend::edit_message` wrap the same
//! requests behind builders that need the update value itself.
//!
//! An update is transient: it borrows the client and the peer map that travelled with it, so a
//! request/response bridge cannot hold one and answer it later. The answers are therefore rebuilt
//! from the identifiers the Kotlin caller read off the projected update, and the native handler
//! invokes exactly the request the builder would have sent. The user half invokes the layer
//! request itself rather than grammers' iterator, because the iterator hides the query id and the
//! next-page offset a caller needs to page through the results and to address a chosen one.
//!
//! The one thing this bridge cannot rebuild is the owner id of the layer's 64-bit inline-message
//! identifier: `InputBotInlineMessageId` exposes only the data centre and the access hash, and the
//! update projection (owned elsewhere) does not carry the owner either. A message addressed by the
//! 64-bit constructor is therefore editable only by a caller that already knows the owner id,
//! which the payload accepts as an optional field.

use std::sync::atomic::{AtomicI64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

use grammers_client::tl;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::inline::inline_query_results_dto;
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

/// Payload of `inlineQuery`: the bot to ask, the peer the query is typed in, the text and the
/// offset that asks for the next page of an earlier answer.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct InlineQueryPayload {
    bot: PeerTarget,
    query: String,
    /// The peer the query is sent from. grammers sends an empty peer when none is given, which the
    /// bot is allowed to use.
    peer: Option<PeerTarget>,
    offset: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct AnswerCallbackQueryPayload {
    query_id: i64,
    text: Option<String>,
    #[serde(default)]
    alert: bool,
    #[serde(default)]
    cache_time_seconds: i32,
}

/// One article result an inline answer carries, mirroring grammers' `Article` builder.
///
/// `Article` is the only result grammers 0.8.1 has a builder for; it wraps an `InputMessage`, so
/// the fields here are the text message it sends plus the metadata Telegram renders it with. The
/// reply markup and the format entities an `InputMessage` can also carry are left out, because
/// they are the markup operations' own payload shapes and not this domain's.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct InlineArticleSpec {
    /// The result id; a timestamp-derived value is used when the caller leaves it out, as
    /// grammers' `Article` does.
    id: Option<String>,
    title: String,
    description: Option<String>,
    url: Option<String>,
    thumb_url: Option<String>,
    message_text: String,
    /// Absent means grammers' default, which keeps the web page preview.
    link_preview: Option<bool>,
    #[serde(default)]
    invert_media: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct InlineSwitchPmSpec {
    text: String,
    start_param: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct AnswerInlineQueryPayload {
    query_id: i64,
    results: Vec<InlineArticleSpec>,
    #[serde(default)]
    cache_time_seconds: i32,
    #[serde(default)]
    gallery: bool,
    #[serde(default)]
    private: bool,
    next_offset: Option<String>,
    switch_pm: Option<InlineSwitchPmSpec>,
}

/// The identifier of an inline message, as the update projection reports it plus the owner id the
/// projection cannot.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct InlineMessageIdPayload {
    dc_id: i32,
    access_hash: i64,
    id: i64,
    /// Set only when the layer's 64-bit constructor is the one to rebuild.
    owner_id: Option<i64>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct EditInlineMessagePayload {
    message_id: InlineMessageIdPayload,
    text: String,
    link_preview: Option<bool>,
    #[serde(default)]
    invert_media: bool,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "inlineQuery",
    "answerCallbackQuery",
    "answerInlineQuery",
    "editInlineMessage",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "inlineQuery" => inline_query,
        "answerCallbackQuery" => answer_callback_query,
        "answerInlineQuery" => answer_inline_query,
        "editInlineMessage" => edit_inline_message,
        _ => return None,
    })
}

/// Asks an inline bot for a page of results, as `Client::inline_query` does but keeping the query
/// id and the next-page offset the iterator hides.
fn inline_query(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: InlineQueryPayload = parse_payload(payload)?;
    let bot = native.runtime.block_on(resolve_peer(native, &data.bot))?;
    let bot = tl::enums::InputUser::from(bot);
    let peer = match &data.peer {
        Some(target) => {
            let peer = native.runtime.block_on(resolve_peer(native, target))?;
            tl::enums::InputPeer::from(peer)
        }
        None => tl::enums::InputPeer::Empty,
    };
    let request = tl::functions::messages::GetInlineBotResults {
        bot,
        peer,
        geo_point: None,
        query: data.query,
        offset: data.offset.unwrap_or_default(),
    };
    let results = native
        .runtime
        .block_on(native.client.invoke(&request))
        .map_err(invocation_error)?;
    // `messages.botResults` has a single constructor, so the pattern is irrefutable.
    let tl::enums::messages::BotResults::Results(results) = results;
    json_string(inline_query_results_dto(&results))
}

/// Answers a pressed callback button, which is `CallbackQuery::answer`'s send with the text,
/// alert and cache options its builder exposes.
fn answer_callback_query(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: AnswerCallbackQueryPayload = parse_payload(payload)?;
    let request = callback_answer(data)?;
    native
        .runtime
        .block_on(native.client.invoke(&request))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Builds the callback answer, refusing the one shape grammers' builder cannot express: an alert
/// always carries the modal text it shows.
fn callback_answer(
    data: AnswerCallbackQueryPayload,
) -> Result<tl::functions::messages::SetBotCallbackAnswer, String> {
    if data.alert && data.text.as_deref().map_or(true, str::is_empty) {
        return Err("answering with an alert requires text".to_owned());
    }
    Ok(tl::functions::messages::SetBotCallbackAnswer {
        alert: data.alert,
        query_id: data.query_id,
        message: data.text,
        // grammers' `Answer` has no method for the layer's answer URL, so it is never set here.
        url: None,
        cache_time: data.cache_time_seconds,
    })
}

/// Answers an inline query with a page of results, which is `InlineQuery::answer`'s send with the
/// gallery, cache, next-offset and switch-to-PM options its builder exposes.
fn answer_inline_query(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: AnswerInlineQueryPayload = parse_payload(payload)?;
    let results = data
        .results
        .iter()
        .map(article_result)
        .collect::<Result<Vec<_>, _>>()?;
    let request = tl::functions::messages::SetInlineBotResults {
        gallery: data.gallery,
        private: data.private,
        query_id: data.query_id,
        results,
        cache_time: data.cache_time_seconds,
        next_offset: data.next_offset,
        switch_pm: data.switch_pm.as_ref().map(|spec| {
            tl::enums::InlineBotSwitchPm::Pm(tl::types::InlineBotSwitchPm {
                text: spec.text.clone(),
                start_param: spec.start_param.clone(),
            })
        }),
        switch_webview: None,
    };
    native
        .runtime
        .block_on(native.client.invoke(&request))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Builds one article result the way grammers' `Article` does: an article kind, the metadata, an
/// optional JPEG thumbnail and a text message.
fn article_result(spec: &InlineArticleSpec) -> Result<tl::enums::InputBotInlineResult, String> {
    if spec.title.is_empty() {
        return Err("an inline result title must not be empty".to_owned());
    }
    let send_message =
        tl::enums::InputBotInlineMessage::Text(tl::types::InputBotInlineMessageText {
            no_webpage: !spec.link_preview.unwrap_or(true),
            invert_media: spec.invert_media,
            message: spec.message_text.clone(),
            entities: None,
            reply_markup: None,
        });
    Ok(tl::enums::InputBotInlineResult::Result(
        tl::types::InputBotInlineResult {
            id: spec.id.clone().unwrap_or_else(generate_result_id),
            r#type: "article".to_owned(),
            title: Some(spec.title.clone()),
            description: spec.description.clone(),
            url: spec.url.clone(),
            thumb: spec.thumb_url.as_ref().map(|url| {
                tl::enums::InputWebDocument::Document(tl::types::InputWebDocument {
                    url: url.clone(),
                    size: 0,
                    mime_type: "image/jpeg".to_owned(),
                    attributes: Vec::new(),
                })
            }),
            content: None,
            send_message,
        },
    ))
}

/// Edits the inline message a chosen result produced.
///
/// The request is `messages.EditInlineBotMessage`, which is the send behind `InlineSend::edit_message`
/// and `CallbackQuery::Answer::edit`; only the text of the new message is configurable here. grammers'
/// own `Client::edit_inline_message` is crate-private, so the layer request is invoked directly.
fn edit_inline_message(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: EditInlineMessagePayload = parse_payload(payload)?;
    let request = tl::functions::messages::EditInlineBotMessage {
        no_webpage: !data.link_preview.unwrap_or(true),
        invert_media: data.invert_media,
        id: inline_message_id(&data.message_id),
        message: Some(data.text),
        media: None,
        reply_markup: None,
        entities: None,
        rich_message: None,
    };
    let edited = native
        .runtime
        .block_on(native.client.invoke(&request))
        .map_err(invocation_error)?;
    json_string(json!({ "edited": edited }))
}

/// Rebuilds the layer identifier, choosing the 64-bit constructor only when its owner is known.
fn inline_message_id(id: &InlineMessageIdPayload) -> tl::enums::InputBotInlineMessageId {
    match id.owner_id {
        Some(owner_id) => {
            tl::enums::InputBotInlineMessageId::Id64(tl::types::InputBotInlineMessageId64 {
                dc_id: id.dc_id,
                owner_id,
                // The 64-bit constructor's id is 32 bits; the projection widened it to the shared
                // field's type, so it narrows back here.
                id: id.id as i32,
                access_hash: id.access_hash,
            })
        }
        None => tl::enums::InputBotInlineMessageId::Id(tl::types::InputBotInlineMessageId {
            dc_id: id.dc_id,
            id: id.id,
            access_hash: id.access_hash,
        }),
    }
}

/// A per-process counter for the result identifiers a caller left out.
///
/// Telegram only requires the identifier to be unique within the result set, but grammers seeds
/// its own generator with the current time so two answers do not collide in Telegram's cache
/// either; this is the same idea, and the counter keeps two results in one call apart.
fn generate_result_id() -> String {
    static NEXT: AtomicI64 = AtomicI64::new(0);
    let counter = NEXT.fetch_add(1, Ordering::SeqCst);
    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_nanos() as i64)
        .unwrap_or(0);
    now.wrapping_add(counter).to_string()
}

#[cfg(test)]
mod tests {
    //! Tests for the request side: the checks the grammers builders document, the identifiers the
    //! handlers rebuild, and the article result the answer is built from.
    //!
    //! The handlers themselves need a live Telegram session, so what is asserted here is everything
    //! between the payload and the grammers request.

    use grammers_client::tl;
    use serde_json::json;

    use crate::error::parse_payload;

    use super::{
        article_result, callback_answer, generate_result_id, inline_message_id,
        AnswerCallbackQueryPayload, InlineArticleSpec, InlineMessageIdPayload,
    };

    /// Decodes the payload exactly as the handlers do.
    fn answer(payload: serde_json::Value) -> AnswerCallbackQueryPayload {
        parse_payload(&payload.to_string()).expect("a callback answer payload")
    }

    /// Decodes an inline-message identifier exactly as the edit handler does.
    fn message_id(payload: serde_json::Value) -> InlineMessageIdPayload {
        parse_payload(&payload.to_string()).expect("an inline message id payload")
    }

    #[test]
    fn a_callback_answer_keeps_its_text_alert_and_cache_time() {
        let request = callback_answer(answer(json!({
            "queryId": 900,
            "text": "Thanks!",
            "alert": false,
            "cacheTimeSeconds": 30,
        })))
        .expect("an answer");
        assert!(!request.alert);
        assert_eq!(request.query_id, 900);
        assert_eq!(request.message.as_deref(), Some("Thanks!"));
        assert_eq!(request.url, None);
        assert_eq!(request.cache_time, 30);
    }

    #[test]
    fn a_bare_callback_answer_only_acknowledges_the_query() {
        let request = callback_answer(answer(json!({ "queryId": 901 }))).expect("an answer");
        assert!(!request.alert);
        assert_eq!(request.query_id, 901);
        assert_eq!(request.message, None);
        assert_eq!(request.cache_time, 0);
    }

    #[test]
    fn an_alert_without_text_is_refused() {
        assert_eq!(
            callback_answer(answer(json!({ "queryId": 900, "alert": true }))),
            Err("answering with an alert requires text".to_owned()),
        );
        // The same check the builders make: `Answer::alert` always sets the text it shows.
        assert_eq!(
            callback_answer(answer(json!({ "queryId": 900, "alert": true, "text": "" }))),
            Err("answering with an alert requires text".to_owned()),
        );
        assert!(callback_answer(answer(json!({
            "queryId": 900,
            "alert": true,
            "text": "Read this",
        })))
        .is_ok());
    }

    /// Decodes one article result exactly as the answer handler does.
    fn article(payload: serde_json::Value) -> InlineArticleSpec {
        parse_payload(&payload.to_string()).expect("an article spec")
    }

    #[test]
    fn an_article_result_is_built_as_grammers_article_is() {
        let built = article_result(&article(json!({
            "id": "result-1",
            "title": "An article",
            "description": "It describes things",
            "url": "https://example.org/article",
            "thumbUrl": "https://example.org/thumb.jpg",
            "messageText": "hello",
            "linkPreview": false,
        })))
        .expect("an article result");

        let tl::enums::InputBotInlineResult::Result(result) = built else {
            panic!("an article is the plain result constructor");
        };
        assert_eq!(result.id, "result-1");
        assert_eq!(result.r#type, "article");
        assert_eq!(result.title.as_deref(), Some("An article"));
        assert_eq!(result.description.as_deref(), Some("It describes things"));
        assert_eq!(result.url.as_deref(), Some("https://example.org/article"));
        assert_eq!(result.content, None);
        // The thumbnail grammers' `Article` builds is the same JPEG web document.
        let Some(tl::enums::InputWebDocument::Document(thumb)) = result.thumb else {
            panic!("the thumbnail is a web document");
        };
        assert_eq!(thumb.url, "https://example.org/thumb.jpg");
        assert_eq!(thumb.mime_type, "image/jpeg");
        let tl::enums::InputBotInlineMessage::Text(message) = result.send_message else {
            panic!("an article sends a text message");
        };
        assert_eq!(message.message, "hello");
        // `linkPreview: false` is the layer's `no_webpage` flag.
        assert!(message.no_webpage);
        assert!(!message.invert_media);
        assert_eq!(message.reply_markup, None);
    }

    #[test]
    fn an_article_without_a_thumbnail_or_link_flag_keeps_the_preview() {
        let built = article_result(&article(json!({
            "title": "An article",
            "messageText": "hello",
        })))
        .expect("an article result");
        let tl::enums::InputBotInlineResult::Result(result) = built else {
            panic!("an article is the plain result constructor");
        };
        assert_eq!(result.thumb, None);
        assert_eq!(result.description, None);
        let tl::enums::InputBotInlineMessage::Text(message) = result.send_message else {
            panic!("an article sends a text message");
        };
        assert!(!message.no_webpage);
    }

    #[test]
    fn an_article_result_needs_a_title() {
        assert_eq!(
            article_result(&article(json!({ "title": "", "messageText": "hello" }))),
            Err("an inline result title must not be empty".to_owned()),
        );
    }

    #[test]
    fn a_missing_result_id_becomes_a_unique_one() {
        let first = generate_result_id();
        let second = generate_result_id();
        assert_ne!(first, second);
        assert!(!first.is_empty());
    }

    #[test]
    fn a_32_bit_inline_message_id_carries_no_owner() {
        let built = inline_message_id(&message_id(json!({
            "dcId": 2,
            "accessHash": 77,
            "id": 8_000_000_001i64,
        })));
        let tl::enums::InputBotInlineMessageId::Id(id) = built else {
            panic!("a projected identifier with no owner is the 32-bit constructor");
        };
        assert_eq!(id.dc_id, 2);
        assert_eq!(id.access_hash, 77);
        assert_eq!(id.id, 8_000_000_001);
    }

    #[test]
    fn a_64_bit_inline_message_id_is_rebuilt_when_its_owner_is_known() {
        let built = inline_message_id(&message_id(json!({
            "dcId": 2,
            "accessHash": 77,
            "id": 88,
            "ownerId": -1_000_007,
        })));
        let tl::enums::InputBotInlineMessageId::Id64(id) = built else {
            panic!("an identifier with an owner is the 64-bit constructor");
        };
        assert_eq!(id.dc_id, 2);
        assert_eq!(id.access_hash, 77);
        assert_eq!(id.owner_id, -1_000_007);
        assert_eq!(id.id, 88);
    }
}
