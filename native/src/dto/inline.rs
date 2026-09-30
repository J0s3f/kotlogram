//! Inline-query result projection.
//!
//! The results of a query to an inline bot are a `messages.botResults`, whose page of
//! [`BotInlineResult`]s grammers types as an enum of the layer's two constructors. The two
//! variants share an id, a type name and an optional title and description, and differ in how they
//! point at their content: the plain result carries web documents for its thumbnail and content,
//! the media one carries a photo or a document. This is therefore the same flat `kind` shape the
//! media projection uses — one object whose `kind` names the variant, with the fields the other
//! variant cannot answer left null.
//!
//! The message a result sends is projected as its plain text (`sendMessageText`): a caller picks a
//! result by its metadata, and the text it would post is the one thing about that message an inline
//! answer needs, so a caller can read it without rebuilding the message. The reply markup the
//! message may carry is still not projected — handing a message back to an inline answer is a
//! separate request. The page itself carries the query id and the offset that asks for the next
//! page, both of which a caller needs to page and to answer, and the switch-to-PM and
//! switch-to-webview prompts the bot answered with.
//!
//! [`BotInlineResult`]: grammers_client::tl::enums::BotInlineResult

use grammers_client::tl;
use serde::Serialize;

use crate::dto::message::MessageDto;

/// One page of results an inline bot answered a query with.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InlineQueryResultsDto {
    /// The identifier an inline answer or a chosen result is sent with.
    pub(crate) query_id: i64,
    /// The offset that asks for the next page, or `None` when this page is the last.
    pub(crate) next_offset: Option<String>,
    /// Whether the results should be shown as a gallery rather than a list.
    pub(crate) gallery: bool,
    /// The prompt that offers to move the query to the bot's private chat, when the bot sent one.
    pub(crate) switch_pm: Option<InlineSwitchPmDto>,
    /// The prompt that offers to move the query to a webview, when the bot sent one.
    pub(crate) switch_webview: Option<InlineSwitchWebviewDto>,
    pub(crate) results: Vec<InlineResultDto>,
}

/// The switch-to-private-chat prompt a page may carry, mirroring `InlineBotSwitchPM`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InlineSwitchPmDto {
    pub(crate) text: String,
    pub(crate) start_param: String,
}

/// The switch-to-webview prompt a page may carry, mirroring `InlineBotWebView`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InlineSwitchWebviewDto {
    pub(crate) text: String,
    pub(crate) url: String,
}

/// What `sendInlineBotResult` produced: the sent message when it could be identified, and a bare
/// acknowledgement otherwise.
///
/// The layer answers the send with an `Updates` bundle rather than the message itself. When the
/// bundle names the message it produced, it is fetched and projected as [Self::message]. When it
/// does not (a scheduled send, or a bundle that carries no message update), the send still
/// succeeded and the update stream delivers the message asynchronously, so only [Self::ok] is set.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SentInlineResultDto {
    pub(crate) ok: bool,
    pub(crate) message: Option<MessageDto>,
}

/// One result of an inline query, flattened over the layer's two result constructors.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InlineResultDto {
    /// `result` for a plain result, `mediaResult` for one that carries a photo or a document.
    pub(crate) kind: &'static str,
    pub(crate) id: String,
    /// The layer's result type, for example `article`, which is called `type` on the wire.
    #[serde(rename = "type")]
    pub(crate) result_type: String,
    pub(crate) title: Option<String>,
    pub(crate) description: Option<String>,
    /// The click-through URL, which only a plain result carries.
    pub(crate) url: Option<String>,
    /// The thumbnail web document, which only a plain result carries.
    pub(crate) thumb: Option<WebDocumentDto>,
    /// The inline content web document, which only a plain result carries.
    pub(crate) content: Option<WebDocumentDto>,
    /// The photo a media result points at, if it is one.
    pub(crate) photo_id: Option<i64>,
    /// The document a media result points at, if it is one.
    pub(crate) document_id: Option<i64>,
    /// The text the result's `send_message` would post, when it sends a text message. It is null
    /// for a result whose message carries media rather than text.
    pub(crate) send_message_text: Option<String>,
}

/// A web document a result points at. grammers only exposes the URL, the size and the MIME type of
/// the layer's two web-document constructors, so the access hash and the attributes are absent.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct WebDocumentDto {
    pub(crate) url: String,
    pub(crate) size: i32,
    pub(crate) mime_type: String,
}

/// Projects one page of inline results.
pub(crate) fn inline_query_results_dto(
    results: &tl::types::messages::BotResults,
) -> InlineQueryResultsDto {
    InlineQueryResultsDto {
        query_id: results.query_id,
        next_offset: results.next_offset.clone(),
        gallery: results.gallery,
        switch_pm: results.switch_pm.as_ref().map(|prompt| {
            let tl::enums::InlineBotSwitchPm::Pm(prompt) = prompt;
            InlineSwitchPmDto {
                text: prompt.text.clone(),
                start_param: prompt.start_param.clone(),
            }
        }),
        switch_webview: results.switch_webview.as_ref().map(|prompt| {
            let tl::enums::InlineBotWebView::View(prompt) = prompt;
            InlineSwitchWebviewDto {
                text: prompt.text.clone(),
                url: prompt.url.clone(),
            }
        }),
        results: results.results.iter().map(inline_result_dto).collect(),
    }
}

/// Projects one inline result, whichever of the two constructors it is.
pub(crate) fn inline_result_dto(result: &tl::enums::BotInlineResult) -> InlineResultDto {
    match result {
        tl::enums::BotInlineResult::Result(result) => InlineResultDto {
            kind: "result",
            id: result.id.clone(),
            result_type: result.r#type.clone(),
            title: result.title.clone(),
            description: result.description.clone(),
            url: result.url.clone(),
            thumb: result.thumb.as_ref().map(web_document_dto),
            content: result.content.as_ref().map(web_document_dto),
            photo_id: None,
            document_id: None,
            send_message_text: send_message_text(&result.send_message),
        },
        tl::enums::BotInlineResult::BotInlineMediaResult(result) => InlineResultDto {
            kind: "mediaResult",
            id: result.id.clone(),
            result_type: result.r#type.clone(),
            title: result.title.clone(),
            description: result.description.clone(),
            url: None,
            thumb: None,
            content: None,
            photo_id: result.photo.as_ref().map(tl::enums::Photo::id),
            document_id: result.document.as_ref().map(tl::enums::Document::id),
            send_message_text: send_message_text(&result.send_message),
        },
    }
}

/// The text a bot inline message would post, when it is a text message. Every other constructor
/// carries media rather than the plain text this projection reports, so those answer `None`.
pub(crate) fn send_message_text(message: &tl::enums::BotInlineMessage) -> Option<String> {
    match message {
        tl::enums::BotInlineMessage::Text(text) => Some(text.message.clone()),
        _ => None,
    }
}

/// Projects the web document behind a result's thumbnail or content.
fn web_document_dto(document: &tl::enums::WebDocument) -> WebDocumentDto {
    WebDocumentDto {
        url: document.url(),
        size: document.size(),
        mime_type: document.mime_type(),
    }
}

#[cfg(test)]
mod tests {
    //! Wire-contract tests for the inline-result projection.
    //!
    //! Each document below is exactly what [`inline_query_results_dto`] emits, so a field renamed
    //! here, a variant dropped from the match, or a web document left unprojected breaks a test
    //! rather than a live session.

    use grammers_client::tl;
    use serde_json::json;

    use super::{inline_query_results_dto, InlineResultDto, WebDocumentDto};

    /// Asserts that [dto] encodes to exactly [expected].
    fn assert_json(dto: &impl serde::Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    /// A text inline message, which every result has to carry.
    fn text_message() -> tl::enums::BotInlineMessage {
        tl::enums::BotInlineMessage::Text(tl::types::BotInlineMessageText {
            no_webpage: true,
            invert_media: false,
            message: "hello".to_owned(),
            entities: None,
            reply_markup: None,
        })
    }

    /// A plain result with a thumbnail web document.
    fn plain_result() -> tl::enums::BotInlineResult {
        tl::enums::BotInlineResult::Result(tl::types::BotInlineResult {
            id: "result-1".to_owned(),
            r#type: "article".to_owned(),
            title: Some("An article".to_owned()),
            description: Some("It describes things".to_owned()),
            url: Some("https://example.org/article".to_owned()),
            thumb: Some(tl::enums::WebDocument::Document(tl::types::WebDocument {
                url: "https://example.org/thumb.jpg".to_owned(),
                access_hash: 77,
                size: 1024,
                mime_type: "image/jpeg".to_owned(),
                attributes: Vec::new(),
            })),
            content: None,
            send_message: text_message(),
        })
    }

    /// A media result that points at a document.
    fn media_result() -> tl::enums::BotInlineResult {
        tl::enums::BotInlineResult::BotInlineMediaResult(tl::types::BotInlineMediaResult {
            id: "result-2".to_owned(),
            r#type: "document".to_owned(),
            photo: None,
            document: Some(tl::enums::Document::Empty(tl::types::DocumentEmpty {
                id: 9_001,
            })),
            title: None,
            description: None,
            send_message: text_message(),
        })
    }

    #[test]
    fn a_page_carries_its_query_id_next_offset_and_results() {
        let page = inline_query_results_dto(&tl::types::messages::BotResults {
            gallery: true,
            query_id: 901,
            next_offset: Some("10".to_owned()),
            switch_pm: Some(tl::enums::InlineBotSwitchPm::Pm(
                tl::types::InlineBotSwitchPm {
                    text: "Open the bot".to_owned(),
                    start_param: "start".to_owned(),
                },
            )),
            switch_webview: Some(tl::enums::InlineBotWebView::View(
                tl::types::InlineBotWebView {
                    text: "Open the page".to_owned(),
                    url: "https://example.org/page".to_owned(),
                },
            )),
            results: vec![plain_result(), media_result()],
            cache_time: 0,
            users: Vec::new(),
        });
        assert_json(
            &page,
            json!({
                "queryId": 901,
                "nextOffset": "10",
                "gallery": true,
                "switchPm": { "text": "Open the bot", "startParam": "start" },
                "switchWebview": { "text": "Open the page", "url": "https://example.org/page" },
                "results": [
                    {
                        "kind": "result",
                        "id": "result-1",
                        "type": "article",
                        "title": "An article",
                        "description": "It describes things",
                        "url": "https://example.org/article",
                        "thumb": {
                            "url": "https://example.org/thumb.jpg",
                            "size": 1024,
                            "mimeType": "image/jpeg",
                        },
                        "content": null,
                        "photoId": null,
                        "documentId": null,
                        "sendMessageText": "hello",
                    },
                    {
                        "kind": "mediaResult",
                        "id": "result-2",
                        "type": "document",
                        "title": null,
                        "description": null,
                        "url": null,
                        "thumb": null,
                        "content": null,
                        "photoId": null,
                        "documentId": 9_001,
                        "sendMessageText": "hello",
                    },
                ],
            }),
        );
    }

    #[test]
    fn the_last_page_carries_no_offset() {
        let page = inline_query_results_dto(&tl::types::messages::BotResults {
            gallery: false,
            query_id: 2,
            next_offset: None,
            switch_pm: None,
            switch_webview: None,
            results: Vec::new(),
            cache_time: 0,
            users: Vec::new(),
        });
        assert_json(
            &page,
            json!({
                "queryId": 2,
                "nextOffset": null,
                "gallery": false,
                "switchPm": null,
                "switchWebview": null,
                "results": [],
            }),
        );
    }

    #[test]
    fn a_media_result_reports_the_photo_it_points_at() {
        let result =
            tl::enums::BotInlineResult::BotInlineMediaResult(tl::types::BotInlineMediaResult {
                id: "result-3".to_owned(),
                r#type: "photo".to_owned(),
                photo: Some(tl::enums::Photo::Empty(tl::types::PhotoEmpty { id: 4_242 })),
                document: None,
                title: Some("A photo".to_owned()),
                description: None,
                send_message: text_message(),
            });
        assert_json(
            &super::inline_result_dto(&result),
            json!({
                "kind": "mediaResult",
                "id": "result-3",
                "type": "photo",
                "title": "A photo",
                "description": null,
                "url": null,
                "thumb": null,
                "content": null,
                "photoId": 4_242,
                "documentId": null,
                "sendMessageText": "hello",
            }),
        );
    }

    #[test]
    fn a_media_carrying_message_reports_no_text() {
        let media = tl::enums::BotInlineMessage::MediaAuto(tl::types::BotInlineMessageMediaAuto {
            invert_media: false,
            message: "caption".to_owned(),
            entities: None,
            reply_markup: None,
        });
        assert_eq!(super::send_message_text(&media), None);
    }

    #[test]
    fn every_field_an_inline_result_can_emit_is_present() {
        let names: Vec<String> = serde_json::to_value(InlineResultDto {
            kind: "result",
            id: String::new(),
            result_type: String::new(),
            title: None,
            description: None,
            url: None,
            thumb: None,
            content: None,
            photo_id: None,
            document_id: None,
            send_message_text: None,
        })
        .expect("a projection encodes")
        .as_object()
        .expect("an object")
        .keys()
        .cloned()
        .collect();
        let mut expected = vec![
            "kind",
            "id",
            "type",
            "title",
            "description",
            "url",
            "thumb",
            "content",
            "photoId",
            "documentId",
            "sendMessageText",
        ];
        let mut names = names;
        names.sort();
        expected.sort();
        assert_eq!(names, expected);
    }

    #[test]
    fn a_web_document_is_projected_as_far_as_grammers_exposes_it() {
        assert_json(
            &WebDocumentDto {
                url: "https://example.org/thumb.jpg".to_owned(),
                size: 512,
                mime_type: "image/jpeg".to_owned(),
            },
            json!({
                "url": "https://example.org/thumb.jpg",
                "size": 512,
                "mimeType": "image/jpeg",
            }),
        );
    }
}
