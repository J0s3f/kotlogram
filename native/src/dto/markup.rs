//! Reply-markup projection.
//!
//! grammers builds a markup through `ReplyMarkup::{from_buttons, from_keys, force_reply, hide}` and
//! hands back the raw `grammers-tl-types` value; it reads one off a message through
//! `Message::reply_markup`, which returns the same value. Both sides are the same four shapes, so
//! this module projects them as one nested document rather than a flattened union: a markup carries
//! its rows, each row its buttons, in the order Telegram renders them.
//!
//! A markup is one object with a `kind` plus the union of the fields its shape can carry. The
//! fields a shape cannot answer are `false` or `null` rather than absent, so a decoder never has to
//! distinguish "not set" from "not modelled". A button is the same idea one level down.
//!
//! [kind] is the grammers `ReplyMarkup` builder for the four kinds grammers can build (`inline`,
//! `keyboard`, `forceReply`, `hide`), and the Bot API name for the button kinds it cannot
//! (`game`, `pay`, `urlAuth`, `userProfile`, `copy`, `requestPeer`, ...).
//!
//! The layer unified its many button constructors into a single `KeyboardButton` /
//! `KeyboardInlineButton` carrying a `type` field, so a button's kind is read off that field here.

use grammers_client::message::ReplyMarkup;
use grammers_client::tl;
use serde::Serialize;

/// A reply markup, mirroring the four shapes grammers' `ReplyMarkup` builds.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ReplyMarkupDto {
    /// `inline`, `keyboard`, `forceReply` or `hide`.
    pub(crate) kind: &'static str,
    /// Rows from top to bottom, each holding its buttons from left to right.
    ///
    /// Empty for [Self::kind] `forceReply` and `hide`, which carry no buttons.
    pub(crate) rows: Vec<Vec<ButtonDto>>,
    /// grammers' `ReplyMarkup::fit_size`, the layer's `resize` flag. Keyboard only.
    pub(crate) fit_size: bool,
    /// grammers' `ReplyMarkup::single_use` for a keyboard or a force-reply.
    pub(crate) single_use: bool,
    /// grammers' `ReplyMarkup::selective` for a keyboard, force-reply or hide.
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
    /// The button kind, named after the grammers `Button`/`Key` function that builds it where
    /// there is one, and after the Bot API button name otherwise.
    pub(crate) kind: &'static str,
    /// The label. grammers requires it to be non-empty on every button it builds.
    pub(crate) text: String,
    /// `url`, `webView` and `simpleWebView` buttons.
    pub(crate) url: Option<String>,
    /// A `callback` button's payload.
    ///
    /// grammers' `Button::data` accepts arbitrary bytes, but the wire is JSON, so a payload that
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
    /// grammers' `Button::switch` sets this, `Button::switch_elsewhere` clears it, asking the
    /// user to pick a peer first.
    pub(crate) same_peer: Option<bool>,
    /// The peer types a `switchInline` button accepts, in lowerCamelCase.
    pub(crate) peer_types: Option<Vec<&'static str>>,
    /// A `requestPoll` button: `Some(true)` is grammers' `Key::request_quiz`, `Some(false)` its
    /// `Key::request_poll`, and `None` a poll of unspecified kind, which only a received markup
    /// carries.
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
    /// variant arm of the button projections starts here.
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

/// Projects a markup this crate has just built with the grammers `ReplyMarkup` builders, so the
/// caller sees exactly the document Telegram will be sent.
pub(crate) fn markup_dto(markup: &ReplyMarkup) -> ReplyMarkupDto {
    reply_markup_dto(&markup.raw)
}

/// Projects a markup read off a message, which is the value `Message::reply_markup` returns.
pub(crate) fn reply_markup_dto(markup: &tl::enums::ReplyMarkup) -> ReplyMarkupDto {
    match markup {
        tl::enums::ReplyMarkup::ReplyInlineMarkup(markup) => ReplyMarkupDto {
            rows: inline_rows_dto(&markup.rows),
            ..ReplyMarkupDto::empty("inline")
        },
        tl::enums::ReplyMarkup::ReplyKeyboardMarkup(markup) => ReplyMarkupDto {
            rows: keyboard_rows_dto(&markup.rows),
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

/// Projects the rows of an inline markup, whose buttons are `KeyboardInlineButton`.
fn inline_rows_dto(rows: &[tl::enums::KeyboardInlineButtonRow]) -> Vec<Vec<ButtonDto>> {
    rows.iter()
        .map(|row| match row {
            tl::enums::KeyboardInlineButtonRow::Row(row) => {
                row.buttons.iter().map(inline_button_dto).collect()
            }
        })
        .collect()
}

/// Projects the rows of a custom keyboard, whose buttons are `KeyboardButton`.
fn keyboard_rows_dto(rows: &[tl::enums::KeyboardButtonRow]) -> Vec<Vec<ButtonDto>> {
    rows.iter()
        .map(|row| match row {
            tl::enums::KeyboardButtonRow::Row(row) => {
                row.buttons.iter().map(keyboard_button_dto).collect()
            }
        })
        .collect()
}

/// Projects one inline button, whose kind is the `InlineButtonType` the layer carries.
///
/// Every variant the layer generates is handled here, so a button type added by a later layer fails
/// the build rather than projecting as nothing.
pub(crate) fn inline_button_dto(button: &tl::enums::KeyboardInlineButton) -> ButtonDto {
    let tl::enums::KeyboardInlineButton::Button(button) = button;
    let text = &button.text;
    match &button.r#type {
        // grammers: `Button::url`
        tl::enums::InlineButtonType::Url(button) => ButtonDto {
            url: Some(button.url.clone()),
            ..ButtonDto::of("url", text)
        },
        // grammers: `Button::webview`
        tl::enums::InlineButtonType::WebView(button) => ButtonDto {
            url: Some(button.url.clone()),
            ..ButtonDto::of("webView", text)
        },
        // grammers: `Button::data`
        tl::enums::InlineButtonType::Callback(button) => ButtonDto {
            data: String::from_utf8(button.data.clone()).ok(),
            requires_password: Some(button.requires_password),
            ..ButtonDto::of("callback", text)
        },
        // grammers: `Button::switch` and `Button::switch_elsewhere`
        tl::enums::InlineButtonType::SwitchInline(button) => ButtonDto {
            query: Some(button.query.clone()),
            same_peer: Some(button.same_peer),
            peer_types: button
                .peer_types
                .as_ref()
                .map(|types| types.iter().map(peer_type_name).collect::<Vec<_>>()),
            ..ButtonDto::of("switchInline", text)
        },
        // No grammers builder: Telegram places this one itself for a game shortcut.
        tl::enums::InlineButtonType::Game => ButtonDto::of("game", text),
        // No grammers builder: the Bot API's pay button.
        tl::enums::InlineButtonType::Buy => ButtonDto::of("pay", text),
        tl::enums::InlineButtonType::UrlAuth(button) => ButtonDto {
            fwd_text: button.fwd_text.clone(),
            url: Some(button.url.clone()),
            button_id: Some(button.button_id),
            ..ButtonDto::of("urlAuth", text)
        },
        // The same button as the one above, as a bot would send it: it also names the bot asking
        // for authorization, which grammers exposes no accessor for.
        tl::enums::InlineButtonType::InputInlineButtonTypeUrlAuth(button) => ButtonDto {
            fwd_text: button.fwd_text.clone(),
            url: Some(button.url.clone()),
            request_write_access: Some(button.request_write_access),
            ..ButtonDto::of("inputUrlAuth", text)
        },
        tl::enums::InlineButtonType::UserProfile(button) => ButtonDto {
            user_id: Some(button.user_id),
            ..ButtonDto::of("userProfile", text)
        },
        tl::enums::InlineButtonType::InputInlineButtonTypeUserProfile(_) => {
            ButtonDto::of("inputUserProfile", text)
        }
        tl::enums::InlineButtonType::Copy(button) => ButtonDto {
            copy_text: Some(button.copy_text.clone()),
            ..ButtonDto::of("copy", text)
        },
        tl::enums::InlineButtonType::Disabled => ButtonDto::of("disabled", text),
    }
}

/// Projects one keyboard button, whose kind is the `ButtonType` the layer carries.
///
/// Every variant the layer generates is handled here, so a button type added by a later layer fails
/// the build rather than projecting as nothing.
pub(crate) fn keyboard_button_dto(button: &tl::enums::KeyboardButton) -> ButtonDto {
    let tl::enums::KeyboardButton::Button(button) = button;
    let text = &button.text;
    match &button.r#type {
        // grammers: `Key::text`
        tl::enums::ButtonType::Default => ButtonDto::of("text", text),
        // grammers: `Key::request_phone`
        tl::enums::ButtonType::RequestPhone => ButtonDto::of("requestPhone", text),
        // grammers: `Key::request_geo`
        tl::enums::ButtonType::RequestGeoLocation => ButtonDto::of("requestGeo", text),
        // grammers: `Key::request_poll` and `Key::request_quiz`
        tl::enums::ButtonType::RequestPoll(button) => ButtonDto {
            quiz: button.quiz,
            ..ButtonDto::of("requestPoll", text)
        },
        tl::enums::ButtonType::RequestPeer(button) => ButtonDto {
            button_id: Some(button.button_id),
            max_quantity: Some(button.max_quantity),
            ..ButtonDto::of("requestPeer", text)
        },
        tl::enums::ButtonType::InputButtonTypeRequestPeer(button) => ButtonDto {
            button_id: Some(button.button_id),
            max_quantity: Some(button.max_quantity),
            ..ButtonDto::of("inputRequestPeer", text)
        },
        // grammers: the simple in-app-browser button.
        tl::enums::ButtonType::SimpleWebView(button) => ButtonDto {
            url: Some(button.url.clone()),
            ..ButtonDto::of("simpleWebView", text)
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

    use grammers_client::message::{Button, Key, ReplyMarkup};
    use grammers_client::tl;
    use serde::Serialize;
    use serde_json::json;

    use crate::dto::markup::{
        inline_button_dto, keyboard_button_dto, reply_markup_dto, ButtonDto, ReplyMarkupDto,
    };

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

    /// Asserts that an inline button projects to [expected], which starts from [`bare`].
    fn assert_inline_button(button: &Button, expected: serde_json::Value) {
        assert_json(&inline_button_dto(&button.raw), expected);
    }

    /// Asserts that a keyboard button projects to [expected], which starts from [`bare`].
    fn assert_keyboard_button(key: &Key, expected: serde_json::Value) {
        assert_json(&keyboard_button_dto(&key.raw), expected);
    }

    /// Builds an inline button from its layer type, for the shapes grammers' builder cannot make.
    fn inline(
        button_type: tl::enums::InlineButtonType,
        text: &str,
    ) -> tl::enums::KeyboardInlineButton {
        tl::enums::KeyboardInlineButton::Button(tl::types::KeyboardInlineButton {
            text: text.to_owned(),
            r#type: button_type,
            style: None,
        })
    }

    /// Builds a keyboard button from its layer type.
    fn keyboard(button_type: tl::enums::ButtonType, text: &str) -> tl::enums::KeyboardButton {
        tl::enums::KeyboardButton::Button(tl::types::KeyboardButton {
            text: text.to_owned(),
            r#type: button_type,
            style: None,
        })
    }

    #[test]
    fn an_inline_markup_projects_its_rows() {
        let markup = ReplyMarkup::from_buttons(&[
            vec![Button::url("Open docs", "https://example.org/docs")],
            vec![Button::data("Vote yes", b"vote:yes".to_vec())],
        ]);
        assert_json(
            &reply_markup_dto(&markup.raw),
            json!({
                "kind": "inline",
                "rows": [
                    [with(bare("url", "Open docs"), "url", json!("https://example.org/docs"))],
                    [with(
                        with(bare("callback", "Vote yes"), "data", json!("vote:yes")),
                        "requiresPassword",
                        json!(false),
                    )],
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
    fn a_reply_keyboard_projects_the_flags_its_builder_offers() {
        let markup = ReplyMarkup::from_keys(&[vec![Key::request_quiz("Quiz")]])
            .fit_size()
            .single_use()
            .selective();
        assert_json(
            &reply_markup_dto(&markup.raw),
            json!({
                "kind": "keyboard",
                "rows": [[with(bare("requestPoll", "Quiz"), "quiz", json!(true))]],
                "fitSize": true,
                "singleUse": true,
                "selective": true,
                "persistent": false,
                "placeholder": null,
            }),
        );
    }

    #[test]
    fn a_force_reply_and_a_hide_carry_no_rows() {
        assert_json(
            &reply_markup_dto(&ReplyMarkup::force_reply().single_use().selective().raw),
            json!({
                "kind": "forceReply",
                "rows": [],
                "fitSize": false,
                "singleUse": true,
                "selective": true,
                "persistent": false,
                "placeholder": null,
            }),
        );

        assert_json(
            &reply_markup_dto(&ReplyMarkup::hide().raw),
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
        assert_inline_button(&Button::switch("Search here", "cats "), {
            let expected = bare("switchInline", "Search here");
            with(
                with(expected, "query", json!("cats ")),
                "samePeer",
                json!(true),
            )
        });

        // A button with peer types and no fixed peer, which only a received markup carries.
        let button = inline(
            tl::enums::InlineButtonType::SwitchInline(tl::types::InlineButtonTypeSwitchInline {
                query: String::new(),
                same_peer: false,
                peer_types: Some(vec![
                    tl::enums::InlineQueryPeerType::SameBotPm,
                    tl::enums::InlineQueryPeerType::Megagroup,
                    tl::enums::InlineQueryPeerType::BotPm,
                ]),
            }),
            "Search anywhere",
        );
        assert_json(&inline_button_dto(&button), {
            let expected = bare("switchInline", "Search anywhere");
            with(
                with(with(expected, "query", json!("")), "samePeer", json!(false)),
                "peerTypes",
                json!(["sameBotPm", "megagroup", "botPm"]),
            )
        });
    }

    #[test]
    fn a_callback_button_projects_its_payload_and_password_flag() {
        assert_inline_button(
            &Button::data("Confirm", b"{\"id\":7}".to_vec()),
            with(
                with(bare("callback", "Confirm"), "data", json!("{\"id\":7}")),
                "requiresPassword",
                json!(false),
            ),
        );
    }

    #[test]
    fn callback_data_that_is_not_text_projects_as_null() {
        assert_inline_button(
            &Button::data("Binary", vec![0xff, 0xfe]),
            with(bare("callback", "Binary"), "requiresPassword", json!(false)),
        );
    }

    #[test]
    fn the_game_and_pay_buttons_project_what_they_carry() {
        // `game` and `pay` are inline button types the layer has but grammers builds no button for.
        assert_json(
            &inline_button_dto(&inline(tl::enums::InlineButtonType::Game, "Play")),
            bare("game", "Play"),
        );
        assert_json(
            &inline_button_dto(&inline(tl::enums::InlineButtonType::Buy, "Pay")),
            bare("pay", "Pay"),
        );
    }

    #[test]
    fn the_auth_buttons_project_their_label_url_and_identifier() {
        assert_json(
            &inline_button_dto(&inline(
                tl::enums::InlineButtonType::UrlAuth(tl::types::InlineButtonTypeUrlAuth {
                    fwd_text: Some("Allow the bot?".to_owned()),
                    url: "https://example.org/auth".to_owned(),
                    button_id: 42,
                }),
                "Authorize",
            )),
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

        // The input variant also names the bot asking, which this bridge does not project.
        assert_json(
            &inline_button_dto(&inline(
                tl::enums::InlineButtonType::InputInlineButtonTypeUrlAuth(
                    tl::types::InputInlineButtonTypeUrlAuth {
                        request_write_access: true,
                        fwd_text: None,
                        url: "https://example.org/auth".to_owned(),
                        bot: Some(tl::enums::InputUser::UserSelf),
                    },
                ),
                "Authorize",
            )),
            with(
                with(
                    bare("inputUrlAuth", "Authorize"),
                    "url",
                    json!("https://example.org/auth"),
                ),
                "requestWriteAccess",
                json!(true),
            ),
        );
    }

    #[test]
    fn the_request_buttons_grammers_cannot_build_still_project() {
        assert_keyboard_button(
            &Key::request_phone("Share my number"),
            bare("requestPhone", "Share my number"),
        );
        assert_keyboard_button(
            &Key::request_geo("Share my location"),
            bare("requestGeo", "Share my location"),
        );
        assert_keyboard_button(
            &Key::request_poll("Poll"),
            with(bare("requestPoll", "Poll"), "quiz", json!(null)),
        );
    }

    #[test]
    fn the_user_profile_and_copy_buttons_project_their_fields() {
        assert_json(
            &inline_button_dto(&inline(
                tl::enums::InlineButtonType::UserProfile(tl::types::InlineButtonTypeUserProfile {
                    user_id: 99,
                }),
                "Open profile",
            )),
            with(bare("userProfile", "Open profile"), "userId", json!(99)),
        );

        // The input variant names an account and an access hash, neither of which this bridge
        // projects.
        assert_json(
            &inline_button_dto(&inline(
                tl::enums::InlineButtonType::InputInlineButtonTypeUserProfile(
                    tl::types::InputInlineButtonTypeUserProfile {
                        user_id: tl::enums::InputUser::UserSelf,
                    },
                ),
                "Open profile",
            )),
            bare("inputUserProfile", "Open profile"),
        );

        assert_json(
            &inline_button_dto(&inline(
                tl::enums::InlineButtonType::Copy(tl::types::InlineButtonTypeCopy {
                    copy_text: "kotlogramme".to_owned(),
                }),
                "Copy",
            )),
            with(bare("copy", "Copy"), "copyText", json!("kotlogramme")),
        );
    }

    #[test]
    fn the_web_view_buttons_project_their_url() {
        assert_inline_button(
            &Button::webview("Play", "https://example.org/game"),
            with(
                bare("webView", "Play"),
                "url",
                json!("https://example.org/game"),
            ),
        );

        assert_json(
            &keyboard_button_dto(&keyboard(
                tl::enums::ButtonType::SimpleWebView(tl::types::ButtonTypeSimpleWebView {
                    url: "https://example.org/doc".to_owned(),
                }),
                "Read",
            )),
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

        assert_json(
            &keyboard_button_dto(&keyboard(
                tl::enums::ButtonType::RequestPeer(tl::types::ButtonTypeRequestPeer {
                    button_id: 5,
                    peer_type: peer_type.clone(),
                    max_quantity: 3,
                }),
                "Suggest",
            )),
            with(
                with(bare("requestPeer", "Suggest"), "buttonId", json!(5)),
                "maxQuantity",
                json!(3),
            ),
        );

        assert_json(
            &keyboard_button_dto(&keyboard(
                tl::enums::ButtonType::InputButtonTypeRequestPeer(
                    tl::types::InputButtonTypeRequestPeer {
                        name_requested: true,
                        username_requested: true,
                        photo_requested: true,
                        button_id: 6,
                        peer_type,
                        max_quantity: 1,
                    },
                ),
                "Suggest",
            )),
            with(
                with(bare("inputRequestPeer", "Suggest"), "buttonId", json!(6)),
                "maxQuantity",
                json!(1),
            ),
        );
    }

    #[test]
    fn a_user_profile_input_button_keeps_only_its_label() {
        assert_json(
            &inline_button_dto(&inline(
                tl::enums::InlineButtonType::InputInlineButtonTypeUserProfile(
                    tl::types::InputInlineButtonTypeUserProfile {
                        user_id: tl::enums::InputUser::UserSelf,
                    },
                ),
                "Open profile",
            )),
            bare("inputUserProfile", "Open profile"),
        );
    }
}
