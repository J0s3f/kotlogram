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

use super::markup::reply_markup_from_spec;
use super::media::external_url_media;
use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::inline::{inline_query_results_dto, SentInlineResultDto};
use crate::dto::message_dto;
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::{resolve_format, EntitySpec, MarkupSpec, PeerTarget};

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
pub(crate) struct InlineArticleSpec {
    /// The result id; a timestamp-derived value is used when the caller leaves it out, as
    /// grammers' `Article` does.
    pub(crate) id: Option<String>,
    pub(crate) title: String,
    pub(crate) description: Option<String>,
    pub(crate) url: Option<String>,
    pub(crate) thumb_url: Option<String>,
    pub(crate) message_text: String,
    /// Absent means grammers' default, which keeps the web page preview.
    pub(crate) link_preview: Option<bool>,
    #[serde(default)]
    pub(crate) invert_media: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct InlineSwitchPmSpec {
    text: String,
    start_param: String,
}

/// One result an inline answer carries, tagged by [kind]: an `article` (the default, mirroring
/// grammers' `Article` builder) or one of the media kinds `photo`, `gif`, `video`, `voice` and
/// `document`, each described by a content URL.
///
/// grammers' builder only covers articles, so the media kinds are built from the raw layer
/// constructors: a plain `InputBotInlineResult` whose `type` names the media kind and whose
/// `content` web document points at the URL, carrying a media message with the caption.
///
/// This is the same flat shape the reply markup uses: one object whose [kind] names the variant,
/// with the fields the other variant cannot answer left at their defaults.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InlineResultSpec {
    /// `article` when absent, or `photo`, `gif`, `video`, `voice`, `document`.
    pub(crate) kind: Option<String>,
    // Article fields.
    pub(crate) title: Option<String>,
    /// The text the article's message posts.
    pub(crate) message_text: Option<String>,
    /// Absent means grammers' default, which keeps the web page preview.
    pub(crate) link_preview: Option<bool>,
    #[serde(default)]
    pub(crate) invert_media: bool,
    // Shared fields.
    /// The result id; a timestamp-derived value is used when the caller leaves it out.
    pub(crate) id: Option<String>,
    pub(crate) description: Option<String>,
    /// The click-through URL the layer renders the result with.
    pub(crate) url: Option<String>,
    /// The JPEG thumbnail URL.
    pub(crate) thumb_url: Option<String>,
    // Media fields.
    /// The URL a media kind's content is fetched from.
    pub(crate) content_url: Option<String>,
    /// The caption a media kind's message carries.
    pub(crate) caption: Option<String>,
}

impl InlineResultSpec {
    /// The kind this result names, defaulting to the article grammers' builder covers.
    pub(crate) fn result_kind(&self) -> &str {
        match self.kind.as_deref() {
            None | Some("") => "article",
            Some(kind) => kind,
        }
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct AnswerInlineQueryPayload {
    query_id: i64,
    results: Vec<InlineResultSpec>,
    #[serde(default)]
    cache_time_seconds: i32,
    #[serde(default)]
    gallery: bool,
    #[serde(default)]
    private: bool,
    next_offset: Option<String>,
    switch_pm: Option<InlineSwitchPmSpec>,
    switch_webview: Option<InlineSwitchWebviewSpec>,
}

/// The switch-to-webview prompt an inline answer may carry.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct InlineSwitchWebviewSpec {
    text: String,
    url: String,
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
    /// `html`, `markdown` or `none` (the default): how [text] is parsed into entities. Ignored
    /// when [entities] is set.
    parse_mode: Option<String>,
    /// Explicit formatting entities, which override the ones the parse mode derives.
    entities: Option<Vec<EntitySpec>>,
    markup: Option<MarkupSpec>,
    /// Replaces the message's media; absent leaves it untouched.
    media: Option<InlineEditMediaSpec>,
}

/// The media an inline edit attaches, which is always a URL: an inline message has no local file
/// to upload. [kind] is `photo` or `document` (the default).
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct InlineEditMediaSpec {
    url: String,
    kind: Option<String>,
}

/// Payload of `sendInlineBotResult`: the peer the chosen result is sent to, the query it was
/// chosen from, the result id, and the send options the layer's `messages.SendInlineBotResult`
/// carries.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SendInlineBotResultPayload {
    peer: PeerTarget,
    /// The query id the result was chosen from, read off a projected inline update.
    query_id: i64,
    /// The result id the caller chose.
    result_id: String,
    #[serde(default)]
    silent: bool,
    #[serde(default)]
    background: bool,
    #[serde(default)]
    clear_draft: bool,
    #[serde(default)]
    hide_via: bool,
    /// The message this one replies to, when it is a reply.
    reply_to_message_id: Option<i32>,
    /// Epoch milliseconds at which to schedule the send.
    schedule_date: Option<i64>,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "inlineQuery",
    "answerCallbackQuery",
    "answerInlineQuery",
    "sendInlineBotResult",
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
        "sendInlineBotResult" => send_inline_bot_result,
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
        .map(inline_result)
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
        switch_webview: data.switch_webview.as_ref().map(|spec| {
            tl::enums::InlineBotWebView::View(tl::types::InlineBotWebView {
                text: spec.text.clone(),
                url: spec.url.clone(),
            })
        }),
    };
    native
        .runtime
        .block_on(native.client.invoke(&request))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Builds one result of an inline answer, whichever kind it is.
fn inline_result(spec: &InlineResultSpec) -> Result<tl::enums::InputBotInlineResult, String> {
    match spec.result_kind() {
        "article" => article_result(&InlineArticleSpec {
            id: spec.id.clone(),
            title: spec.title.clone().unwrap_or_default(),
            description: spec.description.clone(),
            url: spec.url.clone(),
            thumb_url: spec.thumb_url.clone(),
            message_text: spec.message_text.clone().unwrap_or_default(),
            link_preview: spec.link_preview,
            invert_media: spec.invert_media,
        }),
        kind => media_result(spec, kind),
    }
}

/// Builds one article result the way grammers' `Article` does: an article kind, the metadata, an
/// optional JPEG thumbnail and a text message.
pub(crate) fn article_result(
    spec: &InlineArticleSpec,
) -> Result<tl::enums::InputBotInlineResult, String> {
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

/// The media kinds a result may name, which the layer renders as the media type of the content
/// web document.
const MEDIA_KINDS: &[&str] = &["photo", "gif", "video", "voice", "document"];

/// Builds one media result from the raw layer constructors: a plain `InputBotInlineResult` whose
/// `type` names the kind and whose `content` web document points at the URL, with an optional JPEG
/// thumbnail and a media message carrying the caption.
///
/// grammers' `Article` builder only covers articles, so this is the raw-TL path the module already
/// takes for the options its builders cannot express.
pub(crate) fn media_result(
    spec: &InlineResultSpec,
    kind: &str,
) -> Result<tl::enums::InputBotInlineResult, String> {
    if !MEDIA_KINDS.contains(&kind) {
        return Err(format!("unsupported inline result kind: {kind}"));
    }
    let content_url = spec.content_url.as_deref().unwrap_or_default();
    if content_url.is_empty() {
        return Err(format!("a {kind} inline result requires a content URL"));
    }
    let send_message =
        tl::enums::InputBotInlineMessage::MediaAuto(tl::types::InputBotInlineMessageMediaAuto {
            invert_media: false,
            message: spec.caption.clone().unwrap_or_default(),
            entities: None,
            reply_markup: None,
        });
    Ok(tl::enums::InputBotInlineResult::Result(
        tl::types::InputBotInlineResult {
            id: spec.id.clone().unwrap_or_else(generate_result_id),
            r#type: kind.to_owned(),
            title: spec.title.clone(),
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
            content: Some(tl::enums::InputWebDocument::Document(
                tl::types::InputWebDocument {
                    url: content_url.to_owned(),
                    size: 0,
                    mime_type: content_mime_type(kind),
                    attributes: Vec::new(),
                },
            )),
            send_message,
        },
    ))
}

/// The MIME type the layer expects for a media kind's content web document. The generic
/// `application/octet-stream` is the layer's own fallback for a document it does not inspect.
fn content_mime_type(kind: &str) -> String {
    match kind {
        "photo" => "image/jpeg",
        "gif" => "image/gif",
        "video" => "video/mp4",
        "voice" => "audio/ogg",
        _ => "application/octet-stream",
    }
    .to_owned()
}

/// Sends the inline result a caller chose, which is `InlineResult::send`'s send with the silent,
/// background, draft, hide-via, reply and schedule options its payload carries.
///
/// The layer answers with an `Updates` bundle rather than the message itself. When the bundle
/// names the message it produced, that message is fetched and projected; when it does not (a
/// scheduled send, for instance), only `{"ok": true}` is answered and the update stream delivers
/// the message asynchronously.
fn send_inline_bot_result(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SendInlineBotResultPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let random_id = next_generated_id();
    let request = send_request(data, peer, random_id)?;
    let updates = native
        .runtime
        .block_on(native.client.invoke(&request))
        .map_err(invocation_error)?;
    let message = match sent_message_id(&updates, random_id) {
        Some(id) => native
            .runtime
            .block_on(native.client.get_messages_by_id(peer, &[id]))
            .map_err(invocation_error)?
            .into_iter()
            .flatten()
            .next(),
        None => None,
    };
    json_string(SentInlineResultDto {
        ok: true,
        message: message.as_ref().map(|message| message_dto(native, message)),
    })
}

/// Builds the send the layer's `messages.SendInlineBotResult` carries, after the checks the layer
/// makes: the result id is required and a schedule date is seconds, not the milliseconds the bridge
/// carries elsewhere.
fn send_request(
    data: SendInlineBotResultPayload,
    peer: grammers_session::types::PeerRef,
    random_id: i64,
) -> Result<tl::functions::messages::SendInlineBotResult, String> {
    if data.result_id.is_empty() {
        return Err("an inline result id must not be empty".to_owned());
    }
    Ok(tl::functions::messages::SendInlineBotResult {
        silent: data.silent,
        background: data.background,
        clear_draft: data.clear_draft,
        hide_via: data.hide_via,
        peer: peer.into(),
        reply_to: data.reply_to_message_id.map(input_reply_to),
        random_id,
        query_id: data.query_id,
        id: data.result_id,
        schedule_date: data.schedule_date.map(schedule_seconds).transpose()?,
        send_as: None,
        quick_reply_shortcut: None,
        allow_paid_stars: None,
    })
}

/// The id of the message an `Updates` bundle reports for [random_id], when it names one.
///
/// The layer usually sends `UpdateMessageId` mapping the random id to the new message id, next to
/// the `UpdateNewMessage` that carries the message; a short bundle is the message id alone.
fn sent_message_id(updates: &tl::enums::Updates, random_id: i64) -> Option<i32> {
    let matched = match updates {
        tl::enums::Updates::Updates(bundle) => Some(&bundle.updates),
        tl::enums::Updates::Combined(bundle) => Some(&bundle.updates),
        _ => None,
    };
    let matched = matched?;
    for update in matched {
        if let tl::enums::Update::MessageId(id) = update {
            if id.random_id == random_id {
                return Some(id.id);
            }
        }
    }
    None
}

/// Builds the reply-to a scheduled or threaded inline send carries, matching the plain reply the
/// send side builds.
fn input_reply_to(reply_to_msg_id: i32) -> tl::enums::InputReplyTo {
    tl::types::InputReplyToMessage {
        reply_to_msg_id,
        top_msg_id: None,
        reply_to_peer_id: None,
        quote_text: None,
        quote_entities: None,
        quote_offset: None,
        monoforum_peer_id: None,
        todo_item_id: None,
        poll_option: None,
    }
    .into()
}

/// Converts the epoch milliseconds the bridge carries into the seconds the layer schedules with.
fn schedule_seconds(millis: i64) -> Result<i32, String> {
    let seconds = millis.div_euclid(1_000);
    i32::try_from(seconds).map_err(|_| format!("the schedule date is out of range: {millis}"))
}

/// Edits the inline message a chosen result produced.
///
/// The request is `messages.EditInlineBotMessage`, which is the send behind `InlineSend::edit_message`
/// and `CallbackQuery::Answer::edit`: the text, its formatting entities, a reply markup and a
/// URL-only media replacement are all configurable here. grammers' own `Client::edit_inline_message`
/// is crate-private, so the layer request is invoked directly.
fn edit_inline_message(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: EditInlineMessagePayload = parse_payload(payload)?;
    let (text, entities) = resolve_format(
        &data.text,
        data.parse_mode.as_deref(),
        data.entities.as_deref(),
    )?;
    let media = data.media.as_ref().map(inline_edit_media).transpose()?;
    let reply_markup = data
        .markup
        .as_ref()
        .map(|markup| reply_markup_from_spec(markup).map(|built| built.raw))
        .transpose()?;
    // An absent text on a media-only edit is omitted; otherwise the text is sent, even empty, so a
    // caller can clear it.
    let message = if text.is_empty() && media.is_some() {
        None
    } else {
        Some(text)
    };
    let request = tl::functions::messages::EditInlineBotMessage {
        no_webpage: !data.link_preview.unwrap_or(true),
        invert_media: data.invert_media,
        id: inline_message_id(&data.message_id),
        message,
        media,
        reply_markup,
        entities: (!entities.is_empty()).then_some(entities),
        rich_message: None,
    };
    let edited = native
        .runtime
        .block_on(native.client.invoke(&request))
        .map_err(invocation_error)?;
    json_string(json!({ "edited": edited }))
}

/// Builds the URL media an inline edit attaches, through the same external-media builders the
/// send side uses.
fn inline_edit_media(spec: &InlineEditMediaSpec) -> Result<tl::enums::InputMedia, String> {
    external_url_media(spec.kind.as_deref(), &spec.url)
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

/// A per-process counter for the small identifiers a caller left out: the inline result ids grammers
/// generates, and the random ids a send carries.
///
/// Telegram only requires a result identifier to be unique within the result set, but grammers seeds
/// its own generator with the current time so two answers do not collide in Telegram's cache
/// either; this is the same idea, and the counter keeps two values from one call apart.
fn generate_result_id() -> String {
    next_generated_id().to_string()
}

/// The next value of the shared timestamp-seeded counter.
fn next_generated_id() -> i64 {
    static NEXT: AtomicI64 = AtomicI64::new(0);
    let counter = NEXT.fetch_add(1, Ordering::SeqCst);
    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_nanos() as i64)
        .unwrap_or(0);
    now.wrapping_add(counter)
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
        article_result, callback_answer, generate_result_id, inline_edit_media, inline_message_id,
        inline_result, send_request, sent_message_id, AnswerCallbackQueryPayload,
        EditInlineMessagePayload, InlineArticleSpec, InlineEditMediaSpec, InlineMessageIdPayload,
        InlineResultSpec, SendInlineBotResultPayload,
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

    #[test]
    fn an_inline_edit_payload_decodes_the_rich_options() {
        let data: EditInlineMessagePayload = parse_payload(
            &json!({
                "messageId": { "dcId": 2, "accessHash": 77, "id": 88 },
                "text": "<b>hi</b>",
                "parseMode": "html",
                "invertMedia": true,
                "markup": { "kind": "hide", "selective": true },
                "entities": [{ "offset": 0, "length": 2, "type": "bold" }],
                "media": { "url": "https://example.org/movie.mp4", "kind": "document" },
            })
            .to_string(),
        )
        .expect("an inline edit payload");

        assert_eq!(data.text, "<b>hi</b>");
        assert_eq!(data.parse_mode.as_deref(), Some("html"));
        assert!(data.invert_media);
        assert!(matches!(
            data.markup,
            Some(super::MarkupSpec::Hide { selective: true })
        ));
        assert_eq!(data.entities.as_ref().map(Vec::len), Some(1));
        let media = data.media.expect("the media");
        assert_eq!(media.url, "https://example.org/movie.mp4");
        assert_eq!(media.kind.as_deref(), Some("document"));

        // The rich options are absent by default.
        let data: EditInlineMessagePayload = parse_payload(
            &json!({
                "messageId": { "dcId": 2, "accessHash": 77, "id": 88 },
                "text": "hello",
            })
            .to_string(),
        )
        .expect("an inline edit payload");
        assert!(data.parse_mode.is_none() && data.entities.is_none());
        assert!(data.markup.is_none() && data.media.is_none());
    }

    #[test]
    fn an_inline_edit_media_is_built_from_its_url() {
        let photo: InlineEditMediaSpec = parse_payload(
            &json!({ "url": "https://example.org/a.jpg", "kind": "photo" }).to_string(),
        )
        .expect("a photo media");
        let media = inline_edit_media(&photo).expect("a photo media");
        assert!(matches!(
            media,
            tl::enums::InputMedia::PhotoExternal(photo)
                if photo.url == "https://example.org/a.jpg"
        ));

        let document: InlineEditMediaSpec =
            parse_payload(&json!({ "url": "https://example.org/a.pdf" }).to_string())
                .expect("a document media");
        let media = inline_edit_media(&document).expect("a document media");
        assert!(matches!(
            media,
            tl::enums::InputMedia::DocumentExternal(document)
                if document.url == "https://example.org/a.pdf"
        ));
    }

    /// A peer reference with no access hash, which is enough to serialize a request.
    fn peer() -> grammers_session::types::PeerRef {
        grammers_session::types::PeerRef {
            id: grammers_session::types::PeerId::user_unchecked(7),
            auth: grammers_session::types::PeerAuth::default(),
        }
    }

    /// Decodes a send payload exactly as the handler does.
    fn send(payload: serde_json::Value) -> SendInlineBotResultPayload {
        parse_payload(&payload.to_string()).expect("a send inline result payload")
    }

    #[test]
    fn a_send_payload_decodes_the_peer_query_and_result() {
        let data = send(json!({
            "peer": { "peerHandle": 5 },
            "queryId": 900,
            "resultId": "result-1",
            "silent": true,
            "background": true,
            "clearDraft": true,
            "hideVia": true,
            "replyToMessageId": 42,
            "scheduleDate": 1_700_000_000_000i64,
        }));
        assert_eq!(data.peer.peer_handle, Some(5));
        assert_eq!(data.query_id, 900);
        assert_eq!(data.result_id, "result-1");
        assert!(data.silent && data.background && data.clear_draft && data.hide_via);
        assert_eq!(data.reply_to_message_id, Some(42));
        assert_eq!(data.schedule_date, Some(1_700_000_000_000));

        // Every option is optional except the peer, the query and the result.
        let data = send(json!({
            "peer": { "username": "bot" },
            "queryId": 1,
            "resultId": "r",
        }));
        assert!(!data.silent && !data.background && !data.clear_draft && !data.hide_via);
        assert_eq!(data.reply_to_message_id, None);
        assert_eq!(data.schedule_date, None);
    }

    #[test]
    fn a_send_request_carries_the_flags_and_a_second_precision_schedule() {
        let request = send_request(
            send(json!({
                "peer": { "peerHandle": 5 },
                "queryId": 900,
                "resultId": "result-1",
                "silent": true,
                "background": true,
                "clearDraft": true,
                "hideVia": true,
                "replyToMessageId": 42,
                "scheduleDate": 1_700_000_000_000i64,
            })),
            peer(),
            1234,
        )
        .expect("a send request");
        assert_eq!(request.query_id, 900);
        assert_eq!(request.id, "result-1");
        assert_eq!(request.random_id, 1234);
        assert!(request.silent && request.background && request.clear_draft && request.hide_via);
        // Milliseconds on the wire become seconds on the layer.
        assert_eq!(request.schedule_date, Some(1_700_000_000));
        assert!(matches!(
            request.reply_to,
            Some(tl::enums::InputReplyTo::Message(reply))
                if reply.reply_to_msg_id == 42
        ));
        // The options grammers' `InlineResult::send` leaves unset stay unset here.
        assert_eq!(request.send_as, None);
        assert_eq!(request.quick_reply_shortcut, None);
        assert_eq!(request.allow_paid_stars, None);
    }

    #[test]
    fn a_send_request_without_a_schedule_or_reply_leaves_them_unset() {
        let request = send_request(
            send(json!({ "peer": { "peerHandle": 5 }, "queryId": 1, "resultId": "r" })),
            peer(),
            1,
        )
        .expect("a send request");
        assert_eq!(request.schedule_date, None);
        assert_eq!(request.reply_to, None);
    }

    #[test]
    fn a_send_request_needs_a_result_id() {
        assert_eq!(
            send_request(
                send(json!({ "peer": { "peerHandle": 5 }, "queryId": 1, "resultId": "" })),
                peer(),
                1,
            ),
            Err("an inline result id must not be empty".to_owned()),
        );
    }

    #[test]
    fn a_sent_message_id_is_read_from_the_random_id_mapping() {
        let bundle = tl::enums::Updates::Updates(tl::types::Updates {
            updates: vec![
                tl::enums::Update::MessageId(tl::types::UpdateMessageId {
                    id: 77,
                    random_id: 1234,
                }),
                tl::enums::Update::MessageId(tl::types::UpdateMessageId {
                    id: 78,
                    random_id: 9999,
                }),
            ],
            users: Vec::new(),
            chats: Vec::new(),
            date: 0,
            seq: 0,
        });
        assert_eq!(sent_message_id(&bundle, 1234), Some(77));
        assert_eq!(sent_message_id(&bundle, 4242), None);
    }

    #[test]
    fn a_short_bundle_names_no_message_id() {
        // Only `Updates` and `Combined` carry the `UpdateMessageId` mapping this reads.
        assert_eq!(sent_message_id(&tl::enums::Updates::TooLong, 1), None);
    }

    /// Decodes one inline result exactly as the answer handler does.
    fn result(payload: serde_json::Value) -> InlineResultSpec {
        parse_payload(&payload.to_string()).expect("an inline result spec")
    }

    #[test]
    fn each_media_kind_is_built_from_its_content_url() {
        for (kind, mime) in [
            ("photo", "image/jpeg"),
            ("gif", "image/gif"),
            ("video", "video/mp4"),
            ("voice", "audio/ogg"),
            ("document", "application/octet-stream"),
        ] {
            let built = inline_result(&result(json!({
                "kind": kind,
                "id": format!("{kind}-1"),
                "title": "A result",
                "contentUrl": "https://example.org/content",
                "thumbUrl": "https://example.org/thumb.jpg",
                "caption": "it me",
            })))
            .expect("a media result");

            let tl::enums::InputBotInlineResult::Result(result) = built else {
                panic!("a media result is the plain result constructor");
            };
            assert_eq!(result.id, format!("{kind}-1"));
            assert_eq!(result.r#type, kind);
            let Some(tl::enums::InputWebDocument::Document(content)) = result.content else {
                panic!("the content is a web document");
            };
            assert_eq!(content.url, "https://example.org/content");
            assert_eq!(content.mime_type, mime);
            let Some(tl::enums::InputWebDocument::Document(thumb)) = result.thumb else {
                panic!("the thumbnail is a web document");
            };
            assert_eq!(thumb.url, "https://example.org/thumb.jpg");
            let tl::enums::InputBotInlineMessage::MediaAuto(message) = result.send_message else {
                panic!("a media result sends a media message");
            };
            assert_eq!(message.message, "it me");
        }
    }

    #[test]
    fn a_media_result_without_a_thumbnail_or_caption_leaves_them_out() {
        let built = inline_result(&result(json!({
            "kind": "photo",
            "contentUrl": "https://example.org/a.jpg",
        })))
        .expect("a media result");
        let tl::enums::InputBotInlineResult::Result(result) = built else {
            panic!("a media result is the plain result constructor");
        };
        assert_eq!(result.thumb, None);
        assert_eq!(result.title, None);
        let tl::enums::InputBotInlineMessage::MediaAuto(message) = result.send_message else {
            panic!("a media result sends a media message");
        };
        assert_eq!(message.message, "");
        // An absent id still gets the generated one.
        assert!(!result.id.is_empty());
    }

    #[test]
    fn a_media_result_needs_a_known_kind_and_a_content_url() {
        assert_eq!(
            inline_result(&result(
                json!({ "kind": "sticker", "contentUrl": "https://x" })
            )),
            Err("unsupported inline result kind: sticker".to_owned()),
        );
        assert_eq!(
            inline_result(&result(json!({ "kind": "photo", "contentUrl": "" }))),
            Err("a photo inline result requires a content URL".to_owned()),
        );
    }

    #[test]
    fn an_untagged_result_is_still_an_article() {
        let built = inline_result(&result(json!({
            "title": "An article",
            "messageText": "hello",
        })))
        .expect("an article result");
        let tl::enums::InputBotInlineResult::Result(result) = built else {
            panic!("an article is the plain result constructor");
        };
        assert_eq!(result.r#type, "article");
        assert_eq!(result.content, None);
    }

    #[test]
    fn a_tagged_article_result_is_an_article_too() {
        let built = inline_result(&result(json!({
            "kind": "article",
            "title": "An article",
            "messageText": "hello",
        })))
        .expect("an article result");
        let tl::enums::InputBotInlineResult::Result(result) = built else {
            panic!("an article is the plain result constructor");
        };
        assert_eq!(result.r#type, "article");
    }
}
