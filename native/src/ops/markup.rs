//! Reply-markup operations: building the four markups grammers' `reply_markup` module builds, and
//! reading the markup a message carries.
//!
//! grammers attaches a markup to an outgoing message through `InputMessage::reply_markup`, which
//! the message operations own; the operations here build the markup itself and hand the projection
//! back, so a caller can read a markup off one message and build the same one on another.

use grammers_client::message::{Button, Key, ReplyMarkup};
use serde::Deserialize;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::markup::{markup_dto, reply_markup_dto};
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

/// Telegram accepts at most 64 bytes of callback data, and grammers requires the payload to be
/// non-empty, so the bridge checks both rather than letting the server reject the markup.
const CALLBACK_DATA_LIMIT: usize = 64;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct MessageMarkupPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
}

#[derive(Deserialize)]
struct InlineMarkupPayload {
    rows: Vec<Vec<InlineButtonSpec>>,
}

/// A button an inline markup may carry, one variant per grammers `button` function usable there.
///
/// grammers' `button::Inline` covers `inline` (the callback button), `switch_inline`,
/// `switch_inline_elsewhere`, `url` and `webview`, and nothing else: `Key::text` returns a
/// `button::Keyboard`, so a plain label cannot go in an inline markup at all.
#[derive(Deserialize)]
#[serde(
    tag = "type",
    rename_all = "camelCase",
    rename_all_fields = "camelCase"
)]
enum InlineButtonSpec {
    Url {
        text: String,
        url: String,
    },
    WebView {
        text: String,
        url: String,
    },
    Callback {
        text: String,
        data: String,
    },
    SwitchInline {
        text: String,
        query: String,
        /// Absent means grammers' `switch_inline`, which keeps the current peer; `false` is
        /// `switch_inline_elsewhere`, which asks the user to pick one.
        same_peer: Option<bool>,
    },
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ReplyKeyboardPayload {
    rows: Vec<Vec<KeyboardButtonSpec>>,
    #[serde(default)]
    fit_size: bool,
    #[serde(default)]
    single_use: bool,
    #[serde(default)]
    selective: bool,
}

/// A button a custom reply keyboard may carry, one variant per grammers `button` function usable
/// there. The inline kinds are absent because grammers' `reply_markup::keyboard` takes a matrix of
/// `button::Keyboard`, which they are not.
#[derive(Deserialize)]
#[serde(
    tag = "type",
    rename_all = "camelCase",
    rename_all_fields = "camelCase"
)]
enum KeyboardButtonSpec {
    Text {
        text: String,
    },
    RequestPhone {
        text: String,
    },
    RequestGeo {
        text: String,
    },
    RequestPoll {
        text: String,
        /// grammers' `request_quiz`; absent is its `request_poll`.
        #[serde(default)]
        quiz: bool,
    },
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ForceReplyPayload {
    #[serde(default)]
    single_use: bool,
    #[serde(default)]
    selective: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct HideKeyboardPayload {
    #[serde(default)]
    selective: bool,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "getReplyMarkup",
    "buildInlineMarkup",
    "buildReplyKeyboard",
    "buildForceReply",
    "buildHideKeyboard",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "getReplyMarkup" => get_reply_markup,
        "buildInlineMarkup" => build_inline_markup,
        "buildReplyKeyboard" => build_reply_keyboard,
        "buildForceReply" => build_force_reply,
        "buildHideKeyboard" => build_hide_keyboard,
        _ => return None,
    })
}

/// Reads the markup a message carries, which is `Message::reply_markup`.
///
/// Only a message produced by a bot carries one; anything else answers `null`.
fn get_reply_markup(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: MessageMarkupPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let message = native
        .runtime
        .block_on(native.client.get_messages_by_id(peer, &[data.message_id]))
        .map_err(invocation_error)?
        .into_iter()
        .flatten()
        .next()
        .ok_or_else(|| format!("message not found: {}", data.message_id))?;
    json_string(message.reply_markup().as_ref().map(reply_markup_dto))
}

/// Builds the markup grammers' `ReplyMarkup::from_buttons` builds: buttons attached to the message.
fn build_inline_markup(_native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: InlineMarkupPayload = parse_payload(payload)?;
    let rows = inline_rows(&data.rows)?;
    json_string(markup_dto(&ReplyMarkup::from_buttons(&rows)))
}

/// Builds the markup grammers' `ReplyMarkup::from_keys` builds, with the options its inherent
/// methods offer.
fn build_reply_keyboard(_native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ReplyKeyboardPayload = parse_payload(payload)?;
    let rows = keyboard_rows(&data.rows)?;
    json_string(markup_dto(&reply_keyboard_markup(&data, rows)))
}

/// Builds the markup grammers' `ReplyMarkup::force_reply` builds.
fn build_force_reply(_native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ForceReplyPayload = parse_payload(payload)?;
    json_string(markup_dto(&force_reply_markup(&data)))
}

/// Builds the markup grammers' `ReplyMarkup::hide` builds, which removes a keyboard this bot sent
/// earlier.
fn build_hide_keyboard(_native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: HideKeyboardPayload = parse_payload(payload)?;
    json_string(markup_dto(&hide_keyboard_markup(&data)))
}

/// Applies the requested options to grammers' `ReplyMarkup::from_keys`.
fn reply_keyboard_markup(data: &ReplyKeyboardPayload, rows: Vec<Vec<Key>>) -> ReplyMarkup {
    let mut markup = ReplyMarkup::from_keys(&rows);
    if data.fit_size {
        markup = markup.fit_size();
    }
    if data.single_use {
        markup = markup.single_use();
    }
    if data.selective {
        markup = markup.selective();
    }
    markup
}

/// Applies the requested options to grammers' `ReplyMarkup::force_reply`.
fn force_reply_markup(data: &ForceReplyPayload) -> ReplyMarkup {
    let mut markup = ReplyMarkup::force_reply();
    if data.single_use {
        markup = markup.single_use();
    }
    if data.selective {
        markup = markup.selective();
    }
    markup
}

/// Applies the requested options to grammers' `ReplyMarkup::hide`.
fn hide_keyboard_markup(data: &HideKeyboardPayload) -> ReplyMarkup {
    let mut markup = ReplyMarkup::hide();
    if data.selective {
        markup = markup.selective();
    }
    markup
}

/// Builds the rows of an inline markup out of the requested buttons.
fn inline_rows(specs: &[Vec<InlineButtonSpec>]) -> Result<Vec<Vec<Button>>, String> {
    build_rows(specs, inline_button)
}

/// Builds the rows of a custom keyboard out of the requested buttons.
fn keyboard_rows(specs: &[Vec<KeyboardButtonSpec>]) -> Result<Vec<Vec<Key>>, String> {
    build_rows(specs, keyboard_button)
}

/// Turns one requested button into the grammers button it names.
fn inline_button(spec: &InlineButtonSpec) -> Result<Button, String> {
    Ok(match spec {
        InlineButtonSpec::Url { text, url } => Button::url(label(text)?, url.as_str()),
        InlineButtonSpec::WebView { text, url } => Button::webview(label(text)?, url.as_str()),
        InlineButtonSpec::Callback { text, data } => {
            Button::data(label(text)?, callback_data(data)?)
        }
        InlineButtonSpec::SwitchInline {
            text,
            query,
            same_peer,
        } => {
            let text = label(text)?;
            // Absent is grammers' `switch`, the one that keeps the current peer.
            if same_peer.unwrap_or(true) {
                Button::switch(text, query.as_str())
            } else {
                Button::switch_elsewhere(text, query.as_str())
            }
        }
    })
}

/// Turns one requested button into the grammers keyboard button it names.
fn keyboard_button(spec: &KeyboardButtonSpec) -> Result<Key, String> {
    Ok(match spec {
        KeyboardButtonSpec::Text { text } => Key::text(label(text)?),
        KeyboardButtonSpec::RequestPhone { text } => Key::request_phone(label(text)?),
        KeyboardButtonSpec::RequestGeo { text } => Key::request_geo(label(text)?),
        KeyboardButtonSpec::RequestPoll { text, quiz } => {
            let text = label(text)?;
            if *quiz {
                Key::request_quiz(text)
            } else {
                Key::request_poll(text)
            }
        }
    })
}

/// Builds a row matrix, refusing the empty shapes Telegram rejects.
fn build_rows<T, U>(
    specs: &[Vec<T>],
    build: fn(&T) -> Result<U, String>,
) -> Result<Vec<Vec<U>>, String> {
    if specs.is_empty() {
        return Err("a reply markup must contain at least one row of buttons".to_owned());
    }
    specs
        .iter()
        .enumerate()
        .map(|(index, row)| {
            if row.is_empty() {
                return Err(format!("row {index} has no buttons"));
            }
            row.iter().map(build).collect()
        })
        .collect()
}

/// Checks the label every grammers button constructor documents as non-empty.
fn label(text: &str) -> Result<String, String> {
    if text.is_empty() {
        return Err("a button label must not be empty".to_owned());
    }
    Ok(text.to_owned())
}

/// Checks a callback payload, which the layer caps and grammers requires to be non-empty.
///
/// The payload travels as JSON, so it is a string: a caller that needs arbitrary bytes has to
/// encode them, because the wire has no room for them.
fn callback_data(data: &str) -> Result<Vec<u8>, String> {
    if data.is_empty() {
        return Err("callback data must not be empty".to_owned());
    }
    if data.len() > CALLBACK_DATA_LIMIT {
        return Err(format!(
            "callback data must be at most {CALLBACK_DATA_LIMIT} bytes, but it is {}",
            data.len()
        ));
    }
    Ok(data.as_bytes().to_vec())
}

#[cfg(test)]
mod tests {
    //! Tests for the request side: the button specs the payloads decode into, the checks the
    //! grammers constructors document, and the builder options reaching the projection.
    //!
    //! The handlers themselves need a live Telegram session, so what is asserted here is everything
    //! between the payload and the grammers call.

    use grammers_client::message::ReplyMarkup;
    use serde_json::json;

    use crate::dto::markup::{inline_button_dto, keyboard_button_dto, markup_dto};
    use crate::error::{json_string, parse_payload};

    use super::{
        build_rows, callback_data, force_reply_markup, hide_keyboard_markup, inline_button,
        inline_rows, keyboard_button, keyboard_rows, label, reply_keyboard_markup,
        ForceReplyPayload, HideKeyboardPayload, InlineButtonSpec, InlineMarkupPayload,
        KeyboardButtonSpec, ReplyKeyboardPayload,
    };

    /// Builds a markup the way `buildInlineMarkup` does, returning the JSON the handler answers.
    fn build_inline(rows: serde_json::Value) -> Result<String, String> {
        let data: InlineMarkupPayload = parse_payload(&json!({ "rows": rows }).to_string())?;
        let rows = inline_rows(&data.rows)?;
        json_string(markup_dto(&ReplyMarkup::from_buttons(&rows)))
    }

    /// Builds a keyboard the way `buildReplyKeyboard` does.
    fn build_keyboard(payload: serde_json::Value) -> Result<String, String> {
        let data: ReplyKeyboardPayload = parse_payload(&payload.to_string())?;
        let rows = keyboard_rows(&data.rows)?;
        json_string(markup_dto(&reply_keyboard_markup(&data, rows)))
    }

    /// Builds a force reply the way `buildForceReply` does.
    fn build_force_reply(payload: serde_json::Value) -> Result<String, String> {
        let data: ForceReplyPayload = parse_payload(&payload.to_string())?;
        json_string(markup_dto(&force_reply_markup(&data)))
    }

    /// Builds a hide markup the way `buildHideKeyboard` does.
    fn build_hide(payload: serde_json::Value) -> Result<String, String> {
        let data: HideKeyboardPayload = parse_payload(&payload.to_string())?;
        json_string(markup_dto(&hide_keyboard_markup(&data)))
    }

    #[test]
    fn an_inline_markup_is_built_from_every_button_grammers_allows_there() {
        let rows = json!([
            [
                { "type": "url", "text": "Docs", "url": "https://example.org" },
                { "type": "webView", "text": "Play", "url": "https://example.org/game" },
            ],
            [
                { "type": "callback", "text": "Yes", "data": "vote:yes" },
                {
                    "type": "switchInline",
                    "text": "Search",
                    "query": "cats ",
                    "samePeer": false,
                },
            ],
        ]);

        let built: serde_json::Value = parse_payload(&build_inline(rows).expect("a markup"))
            .expect("the handler answers JSON");
        assert_eq!(built["kind"], json!("inline"));
        assert_eq!(built["rows"].as_array().map(Vec::len), Some(2));
        assert_eq!(built["rows"][0][0]["kind"], json!("url"));
        assert_eq!(built["rows"][0][1]["kind"], json!("webView"));
        assert_eq!(built["rows"][1][0]["kind"], json!("callback"));
        assert_eq!(built["rows"][1][0]["data"], json!("vote:yes"));
        // `samePeer: false` is grammers' `switch_inline_elsewhere`, and `peer_types` is always
        // absent because the builder offers no way to set it.
        assert_eq!(built["rows"][1][1]["kind"], json!("switchInline"));
        assert_eq!(built["rows"][1][1]["samePeer"], json!(false));
        assert_eq!(built["rows"][1][1]["peerTypes"], json!(null));
    }

    #[test]
    fn an_absent_same_peer_is_grammers_switch_inline() {
        let built: serde_json::Value = parse_payload(
            &build_inline(json!([[{ "type": "switchInline", "text": "S", "query": "" }]]))
                .expect("a markup"),
        )
        .expect("the handler answers JSON");
        assert_eq!(built["rows"][0][0]["samePeer"], json!(true));
    }

    #[test]
    fn a_reply_keyboard_is_built_from_every_button_grammers_allows_there() {
        let built: serde_json::Value = parse_payload(
            &build_keyboard(json!({
                "rows": [
                    [{ "type": "text", "text": "Accept" }],
                    [
                        { "type": "requestPhone", "text": "Number" },
                        { "type": "requestGeo", "text": "Location" },
                        { "type": "requestPoll", "text": "Poll" },
                        { "type": "requestPoll", "text": "Quiz", "quiz": true },
                    ],
                ],
            }))
            .expect("a keyboard"),
        )
        .expect("the handler answers JSON");

        assert_eq!(built["kind"], json!("keyboard"));
        assert_eq!(built["rows"][1][0]["kind"], json!("requestPhone"));
        assert_eq!(built["rows"][1][1]["kind"], json!("requestGeo"));
        // An absent `quiz` is grammers' `request_poll`, which leaves the layer's flag unset, so it
        // projects as `null` rather than `false`; only its `request_quiz` sets it.
        assert_eq!(built["rows"][1][2]["quiz"], json!(null));
        assert_eq!(built["rows"][1][3]["quiz"], json!(true));
    }

    #[test]
    fn the_keyboard_options_reach_the_projection() {
        let default: serde_json::Value = parse_payload(
            &build_keyboard(json!({ "rows": [[{ "type": "text", "text": "Go" }]] }))
                .expect("a keyboard"),
        )
        .expect("the handler answers JSON");
        assert_eq!(default["fitSize"], json!(false));
        assert_eq!(default["singleUse"], json!(false));
        assert_eq!(default["selective"], json!(false));
        // grammers' `keyboard()` has no method for these two, so a built markup never reports them.
        assert_eq!(default["persistent"], json!(false));
        assert_eq!(default["placeholder"], json!(null));

        let configured: serde_json::Value = parse_payload(
            &build_keyboard(json!({
                "rows": [[{ "type": "text", "text": "Go" }]],
                "fitSize": true,
                "singleUse": true,
                "selective": true,
            }))
            .expect("a keyboard"),
        )
        .expect("the handler answers JSON");
        assert_eq!(configured["fitSize"], json!(true));
        assert_eq!(configured["singleUse"], json!(true));
        assert_eq!(configured["selective"], json!(true));
    }

    #[test]
    fn a_force_reply_and_a_hide_carry_their_options() {
        let force: serde_json::Value = parse_payload(
            &build_force_reply(json!({ "singleUse": true, "selective": true }))
                .expect("a force reply"),
        )
        .expect("the handler answers JSON");
        assert_eq!(force["kind"], json!("forceReply"));
        assert_eq!(force["singleUse"], json!(true));
        assert_eq!(force["selective"], json!(true));
        assert_eq!(force["rows"], json!([]));

        let hide: serde_json::Value =
            parse_payload(&build_hide(json!({ "selective": true })).expect("a hide markup"))
                .expect("the handler answers JSON");
        assert_eq!(hide["kind"], json!("hide"));
        assert_eq!(hide["selective"], json!(true));
        assert_eq!(hide["rows"], json!([]));
    }

    #[test]
    fn the_empty_markups_need_no_payload_fields() {
        let force: serde_json::Value =
            parse_payload(&build_force_reply(json!({})).expect("a markup"))
                .expect("the handler answers JSON");
        assert_eq!(force["kind"], json!("forceReply"));
        assert_eq!(force["singleUse"], json!(false));
        assert_eq!(force["selective"], json!(false));

        let hide: serde_json::Value = parse_payload(&build_hide(json!({})).expect("a markup"))
            .expect("the handler answers JSON");
        assert_eq!(hide["kind"], json!("hide"));
        assert_eq!(hide["selective"], json!(false));
    }

    #[test]
    fn an_empty_label_or_row_is_refused() {
        let empty_label = build_inline(json!([[{ "type": "url", "text": "", "url": "x" }]]));
        assert_eq!(
            empty_label,
            Err("a button label must not be empty".to_owned())
        );

        let empty_rows = build_inline(json!([]));
        assert_eq!(
            empty_rows,
            Err("a reply markup must contain at least one row of buttons".to_owned())
        );

        let empty_row = build_keyboard(json!({ "rows": [[], [{ "type": "text", "text": "Go" }]] }));
        assert_eq!(empty_row, Err("row 0 has no buttons".to_owned()));
    }

    #[test]
    fn an_inline_markup_refuses_the_buttons_grammers_excludes_there() {
        // `Key::text` and the request buttons are `button::Keyboard`, which
        // `reply_markup::inline` cannot hold, so the payload does not even decode.
        for spec in [
            json!({ "type": "text", "text": "Accept" }),
            json!({ "type": "requestPhone", "text": "Number" }),
            json!({ "type": "requestGeo", "text": "Location" }),
            json!({ "type": "requestPoll", "text": "Poll" }),
            json!({ "type": "game", "text": "Play" }),
        ] {
            let error = build_inline(json!([[spec]]))
                .expect_err("the button is not one grammers can put in an inline markup");
            assert!(
                error.starts_with("invalid request payload:"),
                "unexpected error: {error}"
            );
        }
    }

    #[test]
    fn callback_data_must_be_present_and_within_the_layer_limit() {
        assert_eq!(
            callback_data("vote:yes").expect("valid"),
            b"vote:yes".to_vec()
        );
        assert_eq!(
            callback_data(""),
            Err("callback data must not be empty".to_owned())
        );

        let too_long = "x".repeat(65);
        assert_eq!(
            callback_data(&too_long),
            Err("callback data must be at most 64 bytes, but it is 65".to_owned())
        );
        assert!(callback_data(&"x".repeat(64)).is_ok());
    }

    #[test]
    fn a_label_is_kept_exactly_as_requested() {
        assert_eq!(label("  Go  "), Ok("  Go  ".to_owned()));
        assert_eq!(
            label(""),
            Err("a button label must not be empty".to_owned())
        );
    }

    #[test]
    fn a_requested_button_keeps_its_label_and_payload() {
        let spec: InlineButtonSpec =
            parse_payload(r#"{"type":"callback","text":"Vote","data":"v"}"#).expect("a button");
        let built = inline_button(&spec).expect("a button");
        // `Button::data` is the only grammers button that carries a payload, and the only one
        // with a `requires_password` flag, which it always clears.
        let dto = inline_button_dto(&built.raw);
        assert_eq!(dto.kind, "callback");
        assert_eq!(dto.text, "Vote");
        assert_eq!(dto.data, Some("v".to_owned()));
        assert_eq!(dto.requires_password, Some(false));

        let spec: KeyboardButtonSpec =
            parse_payload(r#"{"type":"text","text":"Accept"}"#).expect("a button");
        let built = keyboard_button(&spec).expect("a button");
        let dto = keyboard_button_dto(&built.raw);
        assert_eq!(dto.kind, "text");
        assert_eq!(dto.text, "Accept");
    }

    #[test]
    fn a_row_matrix_of_nothing_is_refused() {
        let empty: Vec<Vec<KeyboardButtonSpec>> = Vec::new();
        assert!(build_rows(&empty, keyboard_button).is_err());
    }
}
