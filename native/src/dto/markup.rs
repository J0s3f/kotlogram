//! Reply-markup projection.
//!
//! grammers builds a markup through `reply_markup::{inline, keyboard, force_reply, hide}` and
//! hands back an opaque `Markup`; it reads one off a message through `Message::reply_markup`, which
//! returns the raw `grammers-tl-types` value. Both sides are the same four shapes, so this module
//! projects them as one nested document rather than a flattened union: a markup carries its rows,
//! each row its buttons, in the order Telegram renders them.
//!
//! A markup is one object with a `kind` plus the union of the fields its shape can carry. The
//! fields a shape cannot answer are `false` or `null` rather than absent, so a decoder never has to
//! distinguish "not set" from "not modelled". A button is the same idea one level down.
//!
//! [kind] is the grammers `reply_markup` function for the four kinds grammers can build (`inline`,
//! `keyboard`, `forceReply`, `hide`), and the Bot API name for the button kinds it cannot
//! (`game`, `pay`, `urlAuth`, `userProfile`, `copy`, `requestPeer`, ...).

use grammers_client::grammers_tl_types as tl;
use grammers_client::types::reply_markup;
use serde::Serialize;

/// A reply markup, mirroring the four shapes grammers' `reply_markup` module builds.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ReplyMarkupDto {
    /// `inline`, `keyboard`, `forceReply` or `hide`.
    pub(crate) kind: &'static str,
    /// Rows from top to bottom, each holding its buttons from left to right.
    ///
    /// Empty for [Self::kind] `forceReply` and `hide`, which carry no buttons.
    pub(crate) rows: Vec<Vec<ButtonDto>>,
    /// grammers' `Keyboard::fit_size`, the layer's `resize` flag. Keyboard only.
    pub(crate) fit_size: bool,
    /// grammers' `Keyboard::single_use` and `ForceReply::single_use`.
    pub(crate) single_use: bool,
    /// grammers' `Keyboard::selective`, `Hide::selective` and `ForceReply::selective`.
    pub(crate) selective: bool,
    /// The layer's `persistent` flag, which keeps a custom keyboard after the bot is gone.
    ///
    /// grammers' builder offers no method for it, so a markup this crate builds always reports
    /// `false`; only a markup read off a received message can report `true`.
    pub(crate) persistent: bool,
    /// The input-field placeholder of a keyboard or a force-reply.
    ///
    /// As with [Self::persistent], grammers' builder cannot set it, so a built markup always
    /// reports `null`.
    pub(crate) placeholder: Option<String>,
}

/// One button of a reply markup.
///
/// Every field is always present in the JSON; the ones a [Self::kind] cannot answer are `null`,
/// so a decoder sees one shape rather than one per button kind.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ButtonDto {
    /// The button kind, named after the grammers `button` function that builds it where there is
    /// one, and after the Bot API button name otherwise.
    pub(crate) kind: &'static str,
    /// The label. grammers requires it to be non-empty on every button it builds.
    pub(crate) text: String,
    /// `url`, `webView` and `simpleWebView` buttons.
    pub(crate) url: Option<String>,
    /// A `callback` button's payload.
    ///
    /// grammers' `button::inline` accepts arbitrary bytes, but the wire is JSON, so a payload that
    /// is not valid UTF-8 projects as `null`: a bot that put binary data in a callback button
    /// cannot read it back through this bridge. A text payload always survives unchanged.
    pub(crate) data: Option<String>,
    /// A `callback` button's `requires_password` flag. grammers' builder always clears it.
    pub(crate) requires_password: Option<bool>,
    /// The confirmation label of a `urlAuth` or `inputUrlAuth` button.
    pub(crate) fwd_text: Option<String>,
    /// The identifier of a `urlAuth`, `inputUrlAuth` or `requestPeer` button.
    pub(crate) button_id: Option<i32>,
    /// The pre-filled query of a `switchInline` button.
    pub(crate) query: Option<String>,
    /// grammers' `switch_inline` sets this, `switch_inline_elsewhere` clears it, asking the user
    /// to pick a peer first.
    pub(crate) same_peer: Option<bool>,
    /// The peer types a `switchInline` button accepts, in lowerCamelCase.
    pub(crate) peer_types: Option<Vec<&'static str>>,
    /// A `requestPoll` button: `Some(true)` is grammers' `request_quiz`, `Some(false)` its
    /// `request_poll`, and `None` a poll of unspecified kind, which only a received markup carries.
    pub(crate) quiz: Option<bool>,
    /// A `userProfile` button.
    pub(crate) user_id: Option<i64>,
    /// A `copy` button's text to place on the clipboard.
    pub(crate) copy_text: Option<String>,
    /// A `requestPeer` button's cap on how many peers may be picked.
    pub(crate) max_quantity: Option<i32>,
    /// An `inputUrlAuth` button's `request_write_access` flag, asking for permission to message
    /// the bot that placed the button.
    pub(crate) request_write_access: Option<bool>,
}

impl ReplyMarkupDto {
    /// A markup of [kind] with nothing but its kind and flags, which every shape has. Each
    /// variant arm of [`reply_markup_dto`] starts here.
    pub(crate) fn empty(kind: &'static str) -> Self {
        Self {
            kind,
            rows: Vec::new(),
            fit_size: false,
            single_use: false,
            selective: false,
            persistent: false,
            placeholder: None,
        }
    }
}

impl ButtonDto {
    /// A button of [kind] carrying nothing but its label, which every button shape has. Each
    /// variant arm of [`button_dto`] starts here.
    pub(crate) fn of(kind: &'static str, text: &str) -> Self {
        Self {
            kind,
            text: text.to_owned(),
            url: None,
            data: None,
            requires_password: None,
            fwd_text: None,
            button_id: None,
            query: None,
            same_peer: None,
            peer_types: None,
            quiz: None,
            user_id: None,
            copy_text: None,
            max_quantity: None,
            request_write_access: None,
        }
    }
}

/// Projects a markup this crate has just built with the grammers `reply_markup` builders, so the
/// caller sees exactly the document Telegram will be sent.
pub(crate) fn markup_dto(markup: &reply_markup::Markup) -> ReplyMarkupDto {
    reply_markup_dto(&markup.raw)
}

/// Projects a markup read off a message, which is the value `Message::reply_markup` returns.
pub(crate) fn reply_markup_dto(markup: &tl::enums::ReplyMarkup) -> ReplyMarkupDto {
    match markup {
        tl::enums::ReplyMarkup::ReplyInlineMarkup(markup) => ReplyMarkupDto {
            rows: rows_dto(&markup.rows),
            ..ReplyMarkupDto::empty("inline")
        },
        tl::enums::ReplyMarkup::ReplyKeyboardMarkup(markup) => ReplyMarkupDto {
            rows: rows_dto(&markup.rows),
            fit_size: markup.resize,
            single_use: markup.single_use,
            selective: markup.selective,
            persistent: markup.persistent,
            placeholder: markup.placeholder.clone(),
            ..ReplyMarkupDto::empty("keyboard")
        },
        tl::enums::ReplyMarkup::ReplyKeyboardForceReply(markup) => ReplyMarkupDto {
            single_use: markup.single_use,
            selective: markup.selective,
            placeholder: markup.placeholder.clone(),
            ..ReplyMarkupDto::empty("forceReply")
        },
        tl::enums::ReplyMarkup::ReplyKeyboardHide(markup) => ReplyMarkupDto {
            selective: markup.selective,
            ..ReplyMarkupDto::empty("hide")
        },
    }
}

/// Projects a row matrix, keeping the order Telegram renders it in.
fn rows_dto(rows: &[tl::enums::KeyboardButtonRow]) -> Vec<Vec<ButtonDto>> {
    rows.iter()
        .map(|row| match row {
            tl::enums::KeyboardButtonRow::Row(row) => row.buttons.iter().map(button_dto).collect(),
        })
        .collect()
}

/// Projects one button.
///
/// `KeyboardButton` is a plain enum, so every variant grammers 0.8.1 generated is handled here and
/// a variant added by a later layer fails the build rather than projecting as nothing.
pub(crate) fn button_dto(button: &tl::enums::KeyboardButton) -> ButtonDto {
    match button {
        // grammers: `button::text`
        tl::enums::KeyboardButton::Button(button) => ButtonDto::of("text", &button.text),
        // grammers: `button::url`
        tl::enums::KeyboardButton::Url(button) => ButtonDto {
            url: Some(button.url.clone()),
            ..ButtonDto::of("url", &button.text)
        },
        // grammers: `button::inline`
        tl::enums::KeyboardButton::Callback(button) => ButtonDto {
            data: String::from_utf8(button.data.clone()).ok(),
            requires_password: Some(button.requires_password),
            ..ButtonDto::of("callback", &button.text)
        },
        // grammers: `button::request_phone`
        tl::enums::KeyboardButton::RequestPhone(button) => {
            ButtonDto::of("requestPhone", &button.text)
        }
        // grammers: `button::request_geo`
        tl::enums::KeyboardButton::RequestGeoLocation(button) => {
            ButtonDto::of("requestGeo", &button.text)
        }
        // grammers: `button::switch_inline` and `button::switch_inline_elsewhere`
        tl::enums::KeyboardButton::SwitchInline(button) => ButtonDto {
            query: Some(button.query.clone()),
            same_peer: Some(button.same_peer),
            peer_types: button
                .peer_types
                .as_ref()
                .map(|types| types.iter().map(peer_type_name).collect::<Vec<_>>()),
            ..ButtonDto::of("switchInline", &button.text)
        },
        // No grammers builder: Telegram places this one itself for a game shortcut.
        tl::enums::KeyboardButton::Game(button) => ButtonDto::of("game", &button.text),
        // No grammers builder: the Bot API's pay button.
        tl::enums::KeyboardButton::Buy(button) => ButtonDto::of("pay", &button.text),
        tl::enums::KeyboardButton::UrlAuth(button) => ButtonDto {
            fwd_text: button.fwd_text.clone(),
            url: Some(button.url.clone()),
            button_id: Some(button.button_id),
            ..ButtonDto::of("urlAuth", &button.text)
        },
        // The same button as the one above, as a bot would send it: it also names the bot asking
        // for authorization, which grammers exposes no accessor for.
        tl::enums::KeyboardButton::InputKeyboardButtonUrlAuth(button) => ButtonDto {
            fwd_text: button.fwd_text.clone(),
            url: Some(button.url.clone()),
            request_write_access: Some(button.request_write_access),
            ..ButtonDto::of("inputUrlAuth", &button.text)
        },
        // grammers: `button::request_poll` and `button::request_quiz`
        tl::enums::KeyboardButton::RequestPoll(button) => ButtonDto {
            quiz: button.quiz,
            ..ButtonDto::of("requestPoll", &button.text)
        },
        tl::enums::KeyboardButton::InputKeyboardButtonUserProfile(button) => {
            ButtonDto::of("inputUserProfile", &button.text)
        }
        tl::enums::KeyboardButton::UserProfile(button) => ButtonDto {
            user_id: Some(button.user_id),
            ..ButtonDto::of("userProfile", &button.text)
        },
        // grammers: `button::webview`
        tl::enums::KeyboardButton::WebView(button) => ButtonDto {
            url: Some(button.url.clone()),
            ..ButtonDto::of("webView", &button.text)
        },
        tl::enums::KeyboardButton::SimpleWebView(button) => ButtonDto {
            url: Some(button.url.clone()),
            ..ButtonDto::of("simpleWebView", &button.text)
        },
        tl::enums::KeyboardButton::RequestPeer(button) => ButtonDto {
            button_id: Some(button.button_id),
            max_quantity: Some(button.max_quantity),
            ..ButtonDto::of("requestPeer", &button.text)
        },
        tl::enums::KeyboardButton::InputKeyboardButtonRequestPeer(button) => ButtonDto {
            button_id: Some(button.button_id),
            max_quantity: Some(button.max_quantity),
            ..ButtonDto::of("inputRequestPeer", &button.text)
        },
        tl::enums::KeyboardButton::Copy(button) => ButtonDto {
            copy_text: Some(button.copy_text.clone()),
            ..ButtonDto::of("copy", &button.text)
        },
    }
}

/// The lowerCamelCase name of a `switchInline` button's accepted peer type.
fn peer_type_name(peer_type: &tl::enums::InlineQueryPeerType) -> &'static str {
    match peer_type {
        tl::enums::InlineQueryPeerType::SameBotPm => "sameBotPm",
        tl::enums::InlineQueryPeerType::Pm => "pm",
        tl::enums::InlineQueryPeerType::Chat => "chat",
        tl::enums::InlineQueryPeerType::Megagroup => "megagroup",
        tl::enums::InlineQueryPeerType::Broadcast => "broadcast",
        tl::enums::InlineQueryPeerType::BotPm => "botPm",
    }
}

#[cfg(test)]
mod tests {
    //! Wire-contract tests for the reply-markup projection.
    //!
    //! The operations can only reach a projection through a live Telegram session, so this is where
    //! the exact document is pinned: a button variant that stops being projected, a field renamed
    //! or a flag dropped breaks a test here rather than a Kotlin decoder. The Kotlin mirror of
    //! these documents is `MarkupProtocolTest`.

    use grammers_client::grammers_tl_types as tl;
    use serde::Serialize;
    use serde_json::json;

    use crate::dto::markup::{button_dto, reply_markup_dto, ButtonDto, ReplyMarkupDto};

    /// Asserts that [dto] encodes to exactly [expected].
    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    /// A button of [kind] carrying nothing but its label, the baseline every variant below adds
    /// its own fields to.
    fn bare(kind: &str, text: &str) -> serde_json::Value {
        json!({
            "kind": kind,
            "text": text,
            "url": null,
            "data": null,
            "requiresPassword": null,
            "fwdText": null,
            "buttonId": null,
            "query": null,
            "samePeer": null,
            "peerTypes": null,
            "quiz": null,
            "userId": null,
            "copyText": null,
            "maxQuantity": null,
            "requestWriteAccess": null,
        })
    }

    /// [bare] with one field filled in.
    fn with(
        mut button: serde_json::Value,
        name: &str,
        value: serde_json::Value,
    ) -> serde_json::Value {
        button[name] = value;
        button
    }

    /// Asserts that [button] projects to [expected], which starts from [`bare`].
    fn assert_button(button: &tl::enums::KeyboardButton, expected: serde_json::Value) {
        assert_json(&button_dto(button), expected);
    }

    /// A row of buttons, in the order Telegram renders it in.
    fn row(buttons: Vec<tl::enums::KeyboardButton>) -> tl::enums::KeyboardButtonRow {
        tl::types::KeyboardButtonRow { buttons }.into()
    }

    #[test]
    fn an_inline_markup_projects_its_rows_and_answers_no_keyboard_option() {
        let markup = tl::enums::ReplyMarkup::ReplyInlineMarkup(tl::types::ReplyInlineMarkup {
            rows: vec![
                row(vec![tl::enums::KeyboardButton::Url(
                    tl::types::KeyboardButtonUrl {
                        text: "Open docs".to_owned(),
                        url: "https://example.org/docs".to_owned(),
                    }
                    .into(),
                )]),
                row(vec![
                    tl::enums::KeyboardButton::Callback(
                        tl::types::KeyboardButtonCallback {
                            requires_password: false,
                            text: "Vote yes".to_owned(),
                            data: b"vote:yes".to_vec(),
                        }
                        .into(),
                    ),
                    tl::enums::KeyboardButton::Button(
                        tl::types::KeyboardButton {
                            text: "Vote no".to_owned(),
                        }
                        .into(),
                    ),
                ]),
            ],
        });

        assert_json(
            &reply_markup_dto(&markup),
            json!({
                "kind": "inline",
                "rows": [
                    [with(bare("url", "Open docs"), "url", json!("https://example.org/docs"))],
                    [
                        with(
                            with(bare("callback", "Vote yes"), "data", json!("vote:yes")),
                            "requiresPassword",
                            json!(false),
                        ),
                        bare("text", "Vote no"),
                    ],
                ],
                "fitSize": false,
                "singleUse": false,
                "selective": false,
                "persistent": false,
                "placeholder": null,
            }),
        );
    }

    #[test]
    fn a_reply_keyboard_projects_every_flag_grammers_cannot_set() {
        let markup = tl::enums::ReplyMarkup::ReplyKeyboardMarkup(tl::types::ReplyKeyboardMarkup {
            resize: true,
            single_use: true,
            selective: true,
            persistent: true,
            rows: vec![row(vec![tl::enums::KeyboardButton::RequestPoll(
                tl::types::KeyboardButtonRequestPoll {
                    quiz: Some(true),
                    text: "Quiz".to_owned(),
                }
                .into(),
            )])],
            placeholder: Some("Pick one".to_owned()),
        });

        assert_json(
            &reply_markup_dto(&markup),
            json!({
                "kind": "keyboard",
                "rows": [[with(bare("requestPoll", "Quiz"), "quiz", json!(true))]],
                "fitSize": true,
                "singleUse": true,
                "selective": true,
                "persistent": true,
                "placeholder": "Pick one",
            }),
        );
    }

    #[test]
    fn a_force_reply_and_a_hide_carry_no_rows() {
        assert_json(
            &reply_markup_dto(&tl::enums::ReplyMarkup::ReplyKeyboardForceReply(
                tl::types::ReplyKeyboardForceReply {
                    single_use: true,
                    selective: true,
                    placeholder: Some("Type here".to_owned()),
                }
                .into(),
            )),
            json!({
                "kind": "forceReply",
                "rows": [],
                "fitSize": false,
                "singleUse": true,
                "selective": true,
                "persistent": false,
                "placeholder": "Type here",
            }),
        );

        assert_json(
            &reply_markup_dto(&tl::enums::ReplyMarkup::ReplyKeyboardHide(
                tl::types::ReplyKeyboardHide { selective: false }.into(),
            )),
            json!({
                "kind": "hide",
                "rows": [],
                "fitSize": false,
                "singleUse": false,
                "selective": false,
                "persistent": false,
                "placeholder": null,
            }),
        );
    }

    #[test]
    fn an_empty_markup_is_still_an_object() {
        let dto = ReplyMarkupDto::empty("hide");
        let encoded = serde_json::to_value(&dto).expect("a markup always encodes");
        assert_eq!(encoded.as_object().expect("an object").len(), 7);
        assert_eq!(encoded["rows"], json!([]));

        let button = ButtonDto::of("text", "Go");
        let encoded = serde_json::to_value(&button).expect("a button always encodes");
        assert_eq!(encoded.as_object().expect("an object").len(), 15);
    }

    #[test]
    fn a_switch_inline_button_projects_its_query_and_peer_types() {
        assert_button(
            &tl::enums::KeyboardButton::SwitchInline(
                tl::types::KeyboardButtonSwitchInline {
                    same_peer: true,
                    text: "Search here".to_owned(),
                    query: "cats ".to_owned(),
                    peer_types: None,
                }
                .into(),
            ),
            {
                let expected = bare("switchInline", "Search here");
                with(
                    with(expected, "query", json!("cats ")),
                    "samePeer",
                    json!(true),
                )
            },
        );

        assert_button(
            &tl::enums::KeyboardButton::SwitchInline(
                tl::types::KeyboardButtonSwitchInline {
                    same_peer: false,
                    text: "Search anywhere".to_owned(),
                    query: String::new(),
                    peer_types: Some(vec![
                        tl::enums::InlineQueryPeerType::SameBotPm,
                        tl::enums::InlineQueryPeerType::Megagroup,
                        tl::enums::InlineQueryPeerType::BotPm,
                    ]),
                }
                .into(),
            ),
            {
                let expected = bare("switchInline", "Search anywhere");
                with(
                    with(with(expected, "query", json!("")), "samePeer", json!(false)),
                    "peerTypes",
                    json!(["sameBotPm", "megagroup", "botPm"]),
                )
            },
        );
    }

    #[test]
    fn a_callback_button_projects_its_payload_and_password_flag() {
        assert_button(
            &tl::enums::KeyboardButton::Callback(
                tl::types::KeyboardButtonCallback {
                    requires_password: true,
                    text: "Confirm".to_owned(),
                    data: b"{\"id\":7}".to_vec(),
                }
                .into(),
            ),
            with(
                with(bare("callback", "Confirm"), "data", json!("{\"id\":7}")),
                "requiresPassword",
                json!(true),
            ),
        );
    }

    #[test]
    fn callback_data_that_is_not_text_projects_as_null() {
        assert_button(
            &tl::enums::KeyboardButton::Callback(
                tl::types::KeyboardButtonCallback {
                    requires_password: false,
                    text: "Binary".to_owned(),
                    // grammers' `button::inline` takes any bytes, including a lone continuation byte.
                    data: vec![0xff, 0xfe],
                }
                .into(),
            ),
            with(bare("callback", "Binary"), "requiresPassword", json!(false)),
        );
    }

    #[test]
    fn the_game_pay_and_auth_buttons_project_what_they_carry() {
        assert_button(
            &tl::enums::KeyboardButton::Game(
                tl::types::KeyboardButtonGame {
                    text: "Play".to_owned(),
                }
                .into(),
            ),
            bare("game", "Play"),
        );

        assert_button(
            &tl::enums::KeyboardButton::Buy(
                tl::types::KeyboardButtonBuy {
                    text: "Pay".to_owned(),
                }
                .into(),
            ),
            bare("pay", "Pay"),
        );

        assert_button(
            &tl::enums::KeyboardButton::UrlAuth(
                tl::types::KeyboardButtonUrlAuth {
                    text: "Authorize".to_owned(),
                    fwd_text: Some("Allow the bot?".to_owned()),
                    url: "https://example.org/auth".to_owned(),
                    button_id: 42,
                }
                .into(),
            ),
            with(
                with(
                    with(
                        bare("urlAuth", "Authorize"),
                        "fwdText",
                        json!("Allow the bot?"),
                    ),
                    "url",
                    json!("https://example.org/auth"),
                ),
                "buttonId",
                json!(42),
            ),
        );

        assert_button(
            &tl::enums::KeyboardButton::InputKeyboardButtonUrlAuth(
                tl::types::InputKeyboardButtonUrlAuth {
                    request_write_access: true,
                    text: "Authorize".to_owned(),
                    fwd_text: None,
                    url: "https://example.org/auth".to_owned(),
                    // The requesting bot is an `InputUser`, which carries an access hash this
                    // bridge deliberately does not project.
                    bot: tl::enums::InputUser::UserSelf,
                }
                .into(),
            ),
            {
                let expected = with(
                    bare("inputUrlAuth", "Authorize"),
                    "url",
                    json!("https://example.org/auth"),
                );
                with(expected, "requestWriteAccess", json!(true))
            },
        );
    }

    #[test]
    fn the_request_buttons_grammers_cannot_build_still_project() {
        assert_button(
            &tl::enums::KeyboardButton::RequestPhone(
                tl::types::KeyboardButtonRequestPhone {
                    text: "Share my number".to_owned(),
                }
                .into(),
            ),
            bare("requestPhone", "Share my number"),
        );

        assert_button(
            &tl::enums::KeyboardButton::RequestGeoLocation(
                tl::types::KeyboardButtonRequestGeoLocation {
                    text: "Share my location".to_owned(),
                }
                .into(),
            ),
            bare("requestGeo", "Share my location"),
        );

        assert_button(
            &tl::enums::KeyboardButton::RequestPoll(
                tl::types::KeyboardButtonRequestPoll {
                    quiz: None,
                    text: "Poll".to_owned(),
                }
                .into(),
            ),
            bare("requestPoll", "Poll"),
        );

        assert_button(
            &tl::enums::KeyboardButton::UserProfile(
                tl::types::KeyboardButtonUserProfile {
                    text: "Open profile".to_owned(),
                    user_id: 99,
                }
                .into(),
            ),
            with(bare("userProfile", "Open profile"), "userId", json!(99)),
        );

        assert_button(
            &tl::enums::KeyboardButton::Copy(
                tl::types::KeyboardButtonCopy {
                    text: "Copy".to_owned(),
                    copy_text: "kotlogramme".to_owned(),
                }
                .into(),
            ),
            with(bare("copy", "Copy"), "copyText", json!("kotlogramme")),
        );
    }

    #[test]
    fn the_web_view_buttons_project_their_url() {
        assert_button(
            &tl::enums::KeyboardButton::WebView(
                tl::types::KeyboardButtonWebView {
                    text: "Play".to_owned(),
                    url: "https://example.org/game".to_owned(),
                }
                .into(),
            ),
            with(
                bare("webView", "Play"),
                "url",
                json!("https://example.org/game"),
            ),
        );

        assert_button(
            &tl::enums::KeyboardButton::SimpleWebView(
                tl::types::KeyboardButtonSimpleWebView {
                    text: "Read".to_owned(),
                    url: "https://example.org/doc".to_owned(),
                }
                .into(),
            ),
            with(
                bare("simpleWebView", "Read"),
                "url",
                json!("https://example.org/doc"),
            ),
        );
    }

    #[test]
    fn the_request_peer_buttons_project_their_identifiers() {
        let peer_type = tl::enums::RequestPeerType::Chat(tl::types::RequestPeerTypeChat {
            creator: false,
            bot_participant: false,
            has_username: None,
            forum: None,
            user_admin_rights: None,
            bot_admin_rights: None,
        });

        assert_button(
            &tl::enums::KeyboardButton::RequestPeer(
                tl::types::KeyboardButtonRequestPeer {
                    text: "Suggest".to_owned(),
                    button_id: 5,
                    peer_type: peer_type.clone(),
                    max_quantity: 3,
                }
                .into(),
            ),
            with(
                with(bare("requestPeer", "Suggest"), "buttonId", json!(5)),
                "maxQuantity",
                json!(3),
            ),
        );

        assert_button(
            &tl::enums::KeyboardButton::InputKeyboardButtonRequestPeer(
                tl::types::InputKeyboardButtonRequestPeer {
                    name_requested: true,
                    username_requested: true,
                    photo_requested: true,
                    text: "Suggest".to_owned(),
                    button_id: 6,
                    peer_type,
                    max_quantity: 1,
                }
                .into(),
            ),
            with(
                with(bare("inputRequestPeer", "Suggest"), "buttonId", json!(6)),
                "maxQuantity",
                json!(1),
            ),
        );
    }

    #[test]
    fn a_user_profile_input_button_keeps_only_its_label() {
        assert_button(
            &tl::enums::KeyboardButton::InputKeyboardButtonUserProfile(
                tl::types::InputKeyboardButtonUserProfile {
                    text: "Open profile".to_owned(),
                    // As with the authorization button, the `InputUser` names an account and an
                    // access hash, neither of which this bridge projects.
                    user_id: tl::enums::InputUser::UserSelf,
                }
                .into(),
            ),
            bare("inputUserProfile", "Open profile"),
        );
    }
}
