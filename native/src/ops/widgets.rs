//! Widgets: HTML payloads a bot attaches to a message (layer 233).
//!
//! grammers has no helper for any of this, so every operation builds the layer's request itself,
//! the way `users.rs` does for `users.getUsers`. The request builders are pure functions so the unit
//! tests in [`super::layer233_tests`] can pin the exact `tl::functions` values without a session.
//!
//! Wallet-carrying query types (`widgetQueryTypeSignData`, `widgetQueryTypeSentTransaction`) and
//! `wallet.card` are deliberately not modelled: they stay behind `invokeRaw`.

use grammers_client::tl;
use serde::{Deserialize, Serialize};
use serde_json::json;

use super::inline::{
    inline_message_id, input_reply_to, next_generated_id, schedule_seconds, sent_message_id,
    InlineMessageIdPayload,
};
use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::files::decode_base64;
use crate::dto::inline::SentInlineResultDto;
use crate::dto::message_dto;
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

/// The HTML of a new widget and how it is shown.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct WidgetSpec {
    pub(crate) html: String,
    pub(crate) width: i32,
    pub(crate) height: i32,
    #[serde(default)]
    pub(crate) allow_queries: bool,
    #[serde(default)]
    pub(crate) shared_widget: bool,
    pub(crate) public_data: Option<String>,
    pub(crate) chat_data: Option<String>,
}

/// Payload of `sendWidget`: a new widget or an existing one by id, and the message it travels in.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SendWidgetPayload {
    pub(crate) peer: PeerTarget,
    pub(crate) widget: Option<WidgetSpec>,
    pub(crate) widget_id: Option<i64>,
    /// Per-chat data for an existing widget; a new widget carries its own in the spec.
    pub(crate) chat_data: Option<String>,
    #[serde(default)]
    pub(crate) caption: String,
    pub(crate) reply_to_message_id: Option<i32>,
    #[serde(default)]
    pub(crate) silent: bool,
    /// Epoch milliseconds at which to schedule the send.
    pub(crate) schedule_date: Option<i64>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GetWidgetPublicDataPayload {
    pub(crate) peer: PeerTarget,
    pub(crate) message_id: i32,
    /// The hash of the copy the caller holds; zero asks for the data unconditionally.
    #[serde(default)]
    pub(crate) hash: i64,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct EditMessageWidgetDataPayload {
    pub(crate) peer: PeerTarget,
    pub(crate) message_id: i32,
    pub(crate) public_data: Option<String>,
    pub(crate) chat_data: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct EditInlineMessageWidgetDataPayload {
    pub(crate) message_id: InlineMessageIdPayload,
    pub(crate) public_data: Option<String>,
    pub(crate) chat_data: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct EditWidgetDataPayload {
    pub(crate) widget_id: i64,
    pub(crate) public_data: String,
    pub(crate) cache_time_seconds: Option<i32>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GetBotWidgetQueryAnswerPayload {
    pub(crate) peer: PeerTarget,
    pub(crate) message_id: i32,
    /// The query data, base64 because JSON has no byte string.
    pub(crate) data_base64: String,
    #[serde(default)]
    pub(crate) with_wallet: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SetBotWidgetQueryAnswerPayload {
    pub(crate) query_id: i64,
    pub(crate) result: Option<String>,
}

/// The layer's `messages.WidgetPublicData`: fresh data, or a note that the caller's copy is current.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct WidgetPublicDataDto {
    pub(crate) not_modified: bool,
    pub(crate) hash: Option<i64>,
    pub(crate) data: Option<String>,
    /// How long the data may be reused before it is asked for again.
    pub(crate) period_seconds: i32,
}

pub(crate) fn public_data_dto(data: &tl::enums::messages::WidgetPublicData) -> WidgetPublicDataDto {
    match data {
        tl::enums::messages::WidgetPublicData::Data(data) => WidgetPublicDataDto {
            not_modified: false,
            hash: Some(data.hash),
            data: Some(data.data.clone()),
            period_seconds: data.period,
        },
        tl::enums::messages::WidgetPublicData::NotModified(same) => WidgetPublicDataDto {
            not_modified: true,
            hash: None,
            data: None,
            period_seconds: same.period,
        },
    }
}

#[cfg_attr(not(test), allow(dead_code))]
/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "sendWidget",
    "getWidgetPublicData",
    "editMessageWidgetData",
    "editInlineMessageWidgetData",
    "editWidgetData",
    "getBotWidgetQueryAnswer",
    "setBotWidgetQueryAnswer",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "sendWidget" => send_widget,
        "getWidgetPublicData" => get_widget_public_data,
        "editMessageWidgetData" => edit_message_widget_data,
        "editInlineMessageWidgetData" => edit_inline_message_widget_data,
        "editWidgetData" => edit_widget_data,
        "getBotWidgetQueryAnswer" => get_bot_widget_query_answer,
        "setBotWidgetQueryAnswer" => set_bot_widget_query_answer,
        _ => return None,
    })
}

/// Checks the one thing a widget cannot do without: markup to show and a size to show it at.
fn validate(spec: &WidgetSpec) -> Result<(), String> {
    if spec.html.is_empty() {
        return Err("a widget's html must not be empty".to_owned());
    }
    if spec.width <= 0 || spec.height <= 0 {
        return Err("a widget's width and height must be positive".to_owned());
    }
    Ok(())
}

/// A send names a new widget or an existing one, never both and never neither.
fn require_one(spec: Option<&WidgetSpec>, widget_id: Option<i64>) -> Result<(), String> {
    match (spec, widget_id) {
        (Some(_), None) | (None, Some(_)) => Ok(()),
        _ => Err("exactly one of widget or widgetId must be set".to_owned()),
    }
}

/// Builds the media a widget message carries: `inputMediaWidget` for a new widget,
/// `inputMediaWidgetId` for an existing one with its per-chat data.
pub(crate) fn widget_input_media(
    spec: Option<&WidgetSpec>,
    widget_id: Option<i64>,
    chat_data: Option<&str>,
) -> Result<tl::enums::InputMedia, String> {
    require_one(spec, widget_id)?;
    match (spec, widget_id) {
        (Some(spec), _) => {
            validate(spec)?;
            Ok(tl::enums::InputMedia::Widget(tl::types::InputMediaWidget {
                allow_queries: spec.allow_queries,
                shared_widget: spec.shared_widget,
                width: spec.width,
                height: spec.height,
                html: spec.html.clone(),
                public_data: spec.public_data.clone(),
                chat_data: spec.chat_data.clone(),
            }))
        }
        (None, Some(widget_id)) => Ok(tl::enums::InputMedia::WidgetId(
            tl::types::InputMediaWidgetId {
                widget_id,
                chat_data: chat_data.map(str::to_owned),
            },
        )),
        (None, None) => unreachable!("require_one rejected it"),
    }
}

/// Builds the message an inline result sends for a widget, with [message] as its text.
pub(crate) fn widget_inline_message(
    spec: Option<&WidgetSpec>,
    widget_id: Option<i64>,
    message: String,
    invert_media: bool,
) -> Result<tl::enums::InputBotInlineMessage, String> {
    require_one(spec, widget_id)?;
    match (spec, widget_id) {
        (Some(spec), _) => {
            validate(spec)?;
            Ok(tl::enums::InputBotInlineMessage::Widget(
                tl::types::InputBotInlineMessageWidget {
                    allow_queries: spec.allow_queries,
                    shared_widget: spec.shared_widget,
                    invert_media,
                    width: spec.width,
                    height: spec.height,
                    html: spec.html.clone(),
                    public_data: spec.public_data.clone(),
                    chat_data: spec.chat_data.clone(),
                    reply_markup: None,
                    message,
                    entities: None,
                },
            ))
        }
        (None, Some(widget_id)) => Ok(tl::enums::InputBotInlineMessage::WidgetId(
            tl::types::InputBotInlineMessageWidgetId {
                invert_media,
                widget_id,
                chat_data: None,
                reply_markup: None,
                message,
                entities: None,
            },
        )),
        (None, None) => unreachable!("require_one rejected it"),
    }
}

/// Builds the `messages.sendMedia` a widget send is.
pub(crate) fn send_widget_request(
    peer: tl::enums::InputPeer,
    data: &SendWidgetPayload,
    random_id: i64,
) -> Result<tl::functions::messages::SendMedia, String> {
    Ok(tl::functions::messages::SendMedia {
        silent: data.silent,
        background: false,
        clear_draft: false,
        noforwards: false,
        update_stickersets_order: false,
        invert_media: false,
        allow_paid_floodskip: false,
        peer,
        reply_to: data.reply_to_message_id.map(input_reply_to),
        media: widget_input_media(data.widget.as_ref(), data.widget_id, data.chat_data.as_deref())?,
        message: data.caption.clone(),
        random_id,
        reply_markup: None,
        entities: None,
        schedule_date: data.schedule_date.map(schedule_seconds).transpose()?,
        schedule_repeat_period: None,
        send_as: None,
        quick_reply_shortcut: None,
        effect: None,
        allow_paid_stars: None,
        suggested_post: None,
    })
}

pub(crate) fn get_public_data_request(
    peer: tl::enums::InputPeer,
    data: &GetWidgetPublicDataPayload,
) -> tl::functions::messages::GetWidgetPublicData {
    tl::functions::messages::GetWidgetPublicData {
        peer,
        msg_id: data.message_id,
        hash: data.hash,
    }
}

pub(crate) fn edit_message_data_request(
    peer: tl::enums::InputPeer,
    data: &EditMessageWidgetDataPayload,
) -> tl::functions::messages::EditMessageWidgetData {
    tl::functions::messages::EditMessageWidgetData {
        peer,
        id: data.message_id,
        public_data: data.public_data.clone(),
        chat_data: data.chat_data.clone(),
    }
}

pub(crate) fn edit_inline_data_request(
    data: &EditInlineMessageWidgetDataPayload,
) -> tl::functions::messages::EditInlineBotMessageWidgetData {
    tl::functions::messages::EditInlineBotMessageWidgetData {
        id: inline_message_id(&data.message_id),
        public_data: data.public_data.clone(),
        chat_data: data.chat_data.clone(),
    }
}

pub(crate) fn edit_widget_data_request(
    data: &EditWidgetDataPayload,
) -> tl::functions::messages::EditWidgetData {
    tl::functions::messages::EditWidgetData {
        widget_id: data.widget_id,
        public_data: data.public_data.clone(),
        cache_time: data.cache_time_seconds,
    }
}

/// Builds the bot's query-answer read. Only the plain `data` query type is built here; the
/// wallet-carrying ones stay behind `invokeRaw`.
pub(crate) fn get_answer_request(
    peer: tl::enums::InputPeer,
    data: &GetBotWidgetQueryAnswerPayload,
) -> Result<tl::functions::messages::GetBotWidgetQueryAnswer, String> {
    Ok(tl::functions::messages::GetBotWidgetQueryAnswer {
        peer,
        msg_id: data.message_id,
        query_type: tl::enums::WidgetQueryType::Data(tl::types::WidgetQueryTypeData {
            with_wallet: data.with_wallet,
        }),
        data: decode_base64(&data.data_base64)?,
    })
}

pub(crate) fn set_answer_request(
    data: &SetBotWidgetQueryAnswerPayload,
) -> tl::functions::messages::SetBotWidgetQueryAnswer {
    tl::functions::messages::SetBotWidgetQueryAnswer {
        query_id: data.query_id,
        result: data.result.clone(),
    }
}

/// The input peer a payload names.
async fn input_peer(
    native: &NativeClient,
    target: &PeerTarget,
) -> Result<tl::enums::InputPeer, String> {
    Ok(resolve_peer(native, target).await?.into())
}

/// Sends a widget as a message, which is `messages.sendMedia` with the layer's widget media.
///
/// Like `sendInlineBotResult`, the layer answers with an `Updates` bundle; the sent message is
/// fetched and projected when the bundle names it, and only the acknowledgement is answered when it
/// does not (a scheduled send, for instance).
fn send_widget(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SendWidgetPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let random_id = next_generated_id();
    let request = send_widget_request(peer.into(), &data, random_id)?;
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

/// Reads a widget's public data, which the layer answers as "not modified" when the hash matches.
fn get_widget_public_data(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetWidgetPublicDataPayload = parse_payload(payload)?;
    let answer = native.runtime.block_on(async {
        let peer = input_peer(native, &data.peer).await?;
        native
            .client
            .invoke(&get_public_data_request(peer, &data))
            .await
            .map_err(invocation_error)
    })?;
    json_string(public_data_dto(&answer))
}

fn edit_message_widget_data(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: EditMessageWidgetDataPayload = parse_payload(payload)?;
    native.runtime.block_on(async {
        let peer = input_peer(native, &data.peer).await?;
        native
            .client
            .invoke(&edit_message_data_request(peer, &data))
            .await
            .map_err(invocation_error)
    })?;
    json_string(json!({ "ok": true }))
}

fn edit_inline_message_widget_data(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: EditInlineMessageWidgetDataPayload = parse_payload(payload)?;
    let edited = native
        .runtime
        .block_on(native.client.invoke(&edit_inline_data_request(&data)))
        .map_err(invocation_error)?;
    json_string(json!({ "edited": edited }))
}

fn edit_widget_data(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: EditWidgetDataPayload = parse_payload(payload)?;
    native
        .runtime
        .block_on(native.client.invoke(&edit_widget_data_request(&data)))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

fn get_bot_widget_query_answer(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetBotWidgetQueryAnswerPayload = parse_payload(payload)?;
    let answer = native.runtime.block_on(async {
        let peer = input_peer(native, &data.peer).await?;
        let request = get_answer_request(peer, &data)?;
        native.client.invoke(&request).await.map_err(invocation_error)
    })?;
    let tl::enums::messages::BotWidgetQueryAnswer::Answer(answer) = answer;
    json_string(json!({ "result": answer.result }))
}

fn set_bot_widget_query_answer(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SetBotWidgetQueryAnswerPayload = parse_payload(payload)?;
    native
        .runtime
        .block_on(native.client.invoke(&set_answer_request(&data)))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}
