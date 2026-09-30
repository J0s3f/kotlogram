//! Request payload shapes shared by more than one operation module.

use grammers_client::tl;
use serde::Deserialize;

/// Peer selector: either a handle handed out by a previous result, or a public username.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PeerTarget {
    pub(crate) peer_handle: Option<i64>,
    pub(crate) username: Option<String>,
}

/// Result-limit selector used by the paged listings.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct LimitPayload {
    pub(crate) limit: Option<usize>,
}

/// The file a send attaches: a local path to upload now, or the handle of an upload that has
/// already run.
///
/// A send names exactly one of the two; [`file_source`] is the one place that check lives, so
/// `sendFile`, `sendMedia` and every album item enforce it the same way.
#[derive(Debug, PartialEq, Eq)]
pub(crate) enum FileSource {
    Path(String),
    Handle(i64),
}

/// Picks the file a send names, refusing a payload that sets neither source or both.
pub(crate) fn file_source(path: Option<String>, handle: Option<i64>) -> Result<FileSource, String> {
    match (path, handle) {
        (Some(path), None) => Ok(FileSource::Path(path)),
        (None, Some(handle)) => Ok(FileSource::Handle(handle)),
        _ => Err("exactly one of path or fileHandle must be set".to_owned()),
    }
}

/// A button an inline markup may carry, one variant per grammers `button` function usable there.
///
/// grammers' `button::Inline` covers `inline` (the callback button), `switch_inline`,
/// `switch_inline_elsewhere`, `url` and `webview`, and nothing else: `Key::text` returns a
/// `button::Keyboard`, so a plain label cannot go in an inline markup at all.
#[derive(Debug, Deserialize)]
#[serde(
    tag = "type",
    rename_all = "camelCase",
    rename_all_fields = "camelCase"
)]
pub(crate) enum InlineButtonSpec {
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

/// A button a custom reply keyboard may carry, one variant per grammers `button` function usable
/// there. The inline kinds are absent because grammers' `reply_markup::keyboard` takes a matrix of
/// `button::Keyboard`, which they are not.
#[derive(Debug, Deserialize)]
#[serde(
    tag = "type",
    rename_all = "camelCase",
    rename_all_fields = "camelCase"
)]
pub(crate) enum KeyboardButtonSpec {
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

/// A reply markup a send or edit payload may carry, tagged by the grammers `reply_markup` function
/// that builds it.
///
/// This is the request-side shape of the four markups [`super::markup`] builds: the same fields
/// the four build payloads accept, so a markup read off one message can be sent on another. The
/// boolean options default to `false`, exactly as the build payloads do.
#[derive(Debug, Deserialize)]
#[serde(
    tag = "kind",
    rename_all = "camelCase",
    rename_all_fields = "camelCase"
)]
pub(crate) enum MarkupSpec {
    Inline {
        rows: Vec<Vec<InlineButtonSpec>>,
    },
    Keyboard {
        rows: Vec<Vec<KeyboardButtonSpec>>,
        #[serde(default)]
        fit_size: bool,
        #[serde(default)]
        single_use: bool,
        #[serde(default)]
        selective: bool,
    },
    ForceReply {
        #[serde(default)]
        single_use: bool,
        #[serde(default)]
        selective: bool,
    },
    Hide {
        #[serde(default)]
        selective: bool,
    },
}

/// A formatting entity an outgoing message carries, in the request direction.
///
/// `entityType` names the layer's entity constructor without its `messageEntity` prefix, for
/// example `bold`, `pre` or `textUrl`, and the variant-specific fields are absent on the types
/// that cannot answer them. This is the same shape a projection of the received entities would
/// serialize, so a message read back can be sent again once projection lands.
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct EntitySpec {
    pub(crate) offset: i32,
    pub(crate) length: i32,
    /// The layer's entity type, without the `messageEntity` prefix (e.g. `bold`, `textUrl`). The
    /// wire carries it as `type`.
    #[serde(rename = "type")]
    pub(crate) entity_type: String,
    pub(crate) url: Option<String>,
    pub(crate) user_id: Option<i64>,
    pub(crate) language: Option<String>,
    pub(crate) custom_emoji_id: Option<i64>,
}

impl EntitySpec {
    /// Builds the layer entity this spec names.
    pub(crate) fn to_message_entity(&self) -> Result<tl::enums::MessageEntity, String> {
        use tl::enums::MessageEntity as Entity;
        let (offset, length) = (self.offset, self.length);
        Ok(match self.entity_type.to_ascii_lowercase().as_str() {
            // The plain mention types carry no extra field.
            "mention" => Entity::Mention(tl::types::MessageEntityMention { offset, length }),
            "hashtag" => Entity::Hashtag(tl::types::MessageEntityHashtag { offset, length }),
            "botcommand" => {
                Entity::BotCommand(tl::types::MessageEntityBotCommand { offset, length })
            }
            "url" => Entity::Url(tl::types::MessageEntityUrl { offset, length }),
            "email" => Entity::Email(tl::types::MessageEntityEmail { offset, length }),
            "bold" => Entity::Bold(tl::types::MessageEntityBold { offset, length }),
            "italic" => Entity::Italic(tl::types::MessageEntityItalic { offset, length }),
            "code" => Entity::Code(tl::types::MessageEntityCode { offset, length }),
            "phone" => Entity::Phone(tl::types::MessageEntityPhone { offset, length }),
            "cashtag" => Entity::Cashtag(tl::types::MessageEntityCashtag { offset, length }),
            "underline" => Entity::Underline(tl::types::MessageEntityUnderline { offset, length }),
            "strike" => Entity::Strike(tl::types::MessageEntityStrike { offset, length }),
            "bankcard" => Entity::BankCard(tl::types::MessageEntityBankCard { offset, length }),
            "spoiler" => Entity::Spoiler(tl::types::MessageEntitySpoiler { offset, length }),
            "blockquote" => Entity::Blockquote(tl::types::MessageEntityBlockquote {
                collapsed: false,
                offset,
                length,
            }),
            // The types with one extra field: an absent one is refused rather than guessed.
            "pre" => Entity::Pre(tl::types::MessageEntityPre {
                offset,
                length,
                language: self.language.clone().unwrap_or_default(),
            }),
            "texturl" => Entity::TextUrl(tl::types::MessageEntityTextUrl {
                offset,
                length,
                url: self.required("url", self.url.as_ref())?,
            }),
            "mentionname" => Entity::MentionName(tl::types::MessageEntityMentionName {
                offset,
                length,
                user_id: self.required("userId", self.user_id.as_ref())?,
            }),
            "customemoji" => Entity::CustomEmoji(tl::types::MessageEntityCustomEmoji {
                offset,
                length,
                document_id: self.required("customEmojiId", self.custom_emoji_id.as_ref())?,
            }),
            other => return Err(format!("unknown entity type: {other}")),
        })
    }

    /// The field an entity of this type requires, which the layer has no sensible default for.
    fn required<T: Clone>(&self, name: &str, value: Option<&T>) -> Result<T, String> {
        value
            .cloned()
            .ok_or_else(|| format!("a {} entity requires {name}", self.entity_type))
    }
}

/// Builds the entity list a request carries.
pub(crate) fn message_entities(
    specs: &[EntitySpec],
) -> Result<Vec<tl::enums::MessageEntity>, String> {
    specs.iter().map(EntitySpec::to_message_entity).collect()
}

/// Resolves the text and formatting entities an outgoing message carries.
///
/// A parse mode derives the entities from the markup — `html` and `markdown`, or `none` (and
/// absent) for plain text — and an explicit entity list overrides the derived ones. The text the
/// parser produces is returned too, because parsing strips the markup.
pub(crate) fn resolve_format(
    text: &str,
    parse_mode: Option<&str>,
    entities: Option<&[EntitySpec]>,
) -> Result<(String, Vec<tl::enums::MessageEntity>), String> {
    let (text, mut resolved) = match parse_mode.map(str::to_ascii_lowercase).as_deref() {
        None | Some("") | Some("none") => (text.to_owned(), Vec::new()),
        Some("html") => grammers_client::parsers::parse_html_message(text),
        Some("markdown") => grammers_client::parsers::parse_markdown_message(text),
        Some(other) => return Err(format!("unsupported parse mode: {other}")),
    };
    if let Some(specs) = entities {
        resolved = message_entities(specs)?;
    }
    Ok((text, resolved))
}

#[cfg(test)]
mod tests {
    use grammers_client::tl;
    use serde_json::json;

    use crate::error::parse_payload;

    use super::{resolve_format, EntitySpec};

    fn entity(spec: serde_json::Value) -> EntitySpec {
        parse_payload(&spec.to_string()).expect("an entity spec")
    }

    #[test]
    fn every_entity_type_maps_to_its_layer_constructor() {
        let cases: &[(&str, tl::enums::MessageEntity)] = &[
            (
                "mention",
                tl::enums::MessageEntity::Mention(tl::types::MessageEntityMention {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "bold",
                tl::enums::MessageEntity::Bold(tl::types::MessageEntityBold {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "italic",
                tl::enums::MessageEntity::Italic(tl::types::MessageEntityItalic {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "underline",
                tl::enums::MessageEntity::Underline(tl::types::MessageEntityUnderline {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "strike",
                tl::enums::MessageEntity::Strike(tl::types::MessageEntityStrike {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "spoiler",
                tl::enums::MessageEntity::Spoiler(tl::types::MessageEntitySpoiler {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "code",
                tl::enums::MessageEntity::Code(tl::types::MessageEntityCode {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "url",
                tl::enums::MessageEntity::Url(tl::types::MessageEntityUrl {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "email",
                tl::enums::MessageEntity::Email(tl::types::MessageEntityEmail {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "phone",
                tl::enums::MessageEntity::Phone(tl::types::MessageEntityPhone {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "cashtag",
                tl::enums::MessageEntity::Cashtag(tl::types::MessageEntityCashtag {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "hashtag",
                tl::enums::MessageEntity::Hashtag(tl::types::MessageEntityHashtag {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "botCommand",
                tl::enums::MessageEntity::BotCommand(tl::types::MessageEntityBotCommand {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "bankCard",
                tl::enums::MessageEntity::BankCard(tl::types::MessageEntityBankCard {
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "blockquote",
                tl::enums::MessageEntity::Blockquote(tl::types::MessageEntityBlockquote {
                    collapsed: false,
                    offset: 1,
                    length: 2,
                }),
            ),
            (
                "pre",
                tl::enums::MessageEntity::Pre(tl::types::MessageEntityPre {
                    offset: 1,
                    length: 2,
                    language: "rust".to_owned(),
                }),
            ),
            (
                "textUrl",
                tl::enums::MessageEntity::TextUrl(tl::types::MessageEntityTextUrl {
                    offset: 1,
                    length: 2,
                    url: "https://example.org".to_owned(),
                }),
            ),
            (
                "mentionName",
                tl::enums::MessageEntity::MentionName(tl::types::MessageEntityMentionName {
                    offset: 1,
                    length: 2,
                    user_id: 7,
                }),
            ),
            (
                "customEmoji",
                tl::enums::MessageEntity::CustomEmoji(tl::types::MessageEntityCustomEmoji {
                    offset: 1,
                    length: 2,
                    document_id: 5150,
                }),
            ),
        ];

        for (entity_type, expected) in cases {
            let spec = match *entity_type {
                "pre" => entity(json!({
                    "offset": 1, "length": 2, "type": entity_type, "language": "rust",
                })),
                "textUrl" => entity(json!({
                    "offset": 1, "length": 2, "type": entity_type, "url": "https://example.org",
                })),
                "mentionName" => entity(json!({
                    "offset": 1, "length": 2, "type": entity_type, "userId": 7,
                })),
                "customEmoji" => entity(json!({
                    "offset": 1, "length": 2, "type": entity_type, "customEmojiId": 5150,
                })),
                _ => entity(json!({ "offset": 1, "length": 2, "type": entity_type })),
            };
            assert_eq!(
                spec.to_message_entity()
                    .unwrap_or_else(|_| panic!("{entity_type} maps")),
                *expected,
                "{entity_type} must map to its variant"
            );
        }
    }

    #[test]
    fn an_entity_type_without_its_required_field_is_refused() {
        assert_eq!(
            entity(json!({ "offset": 0, "length": 1, "type": "textUrl" }))
                .to_message_entity()
                .unwrap_err(),
            "a textUrl entity requires url"
        );
        assert_eq!(
            entity(json!({ "offset": 0, "length": 1, "type": "mentionName" }))
                .to_message_entity()
                .unwrap_err(),
            "a mentionName entity requires userId"
        );
        assert_eq!(
            entity(json!({ "offset": 0, "length": 1, "type": "customEmoji" }))
                .to_message_entity()
                .unwrap_err(),
            "a customEmoji entity requires customEmojiId"
        );
        assert_eq!(
            entity(json!({ "offset": 0, "length": 1, "type": "spoil" }))
                .to_message_entity()
                .unwrap_err(),
            "unknown entity type: spoil"
        );
    }

    #[test]
    fn a_parse_mode_derives_entities_and_an_explicit_list_overrides_them() {
        let (text, entities) = resolve_format("<b>hi</b>", Some("html"), None).expect("html");
        assert_eq!(text, "hi");
        assert_eq!(
            entities,
            vec![tl::enums::MessageEntity::Bold(
                tl::types::MessageEntityBold {
                    offset: 0,
                    length: 2,
                }
            )]
        );

        // An explicit list replaces whatever the parse mode produced.
        let spec = entity(json!({ "offset": 0, "length": 2, "type": "italic" }));
        let (text, entities) =
            resolve_format("<b>hi</b>", Some("html"), Some(&[spec])).expect("an explicit list");
        assert_eq!(text, "hi");
        assert_eq!(
            entities,
            vec![tl::enums::MessageEntity::Italic(
                tl::types::MessageEntityItalic {
                    offset: 0,
                    length: 2,
                }
            )]
        );

        // `none` and absent leave the text plain.
        assert_eq!(
            resolve_format("hi", None, None).expect("plain"),
            ("hi".to_owned(), Vec::new())
        );
        assert_eq!(
            resolve_format("hi", Some("none"), None).expect("none"),
            ("hi".to_owned(), Vec::new())
        );
        assert_eq!(
            resolve_format("hi", Some("json"), None).unwrap_err(),
            "unsupported parse mode: json"
        );
    }

    #[test]
    fn a_file_source_is_exactly_one_of_a_path_or_a_handle() {
        use super::{file_source, FileSource};

        assert_eq!(
            file_source(Some("/tmp/a.pdf".to_owned()), None),
            Ok(FileSource::Path("/tmp/a.pdf".to_owned()))
        );
        assert_eq!(file_source(None, Some(12)), Ok(FileSource::Handle(12)));
        // Neither source, and both at once, are both refused with the same message.
        for (path, handle) in [(None, None), (Some("/tmp/a.pdf".to_owned()), Some(12))] {
            assert_eq!(
                file_source(path, handle).unwrap_err(),
                "exactly one of path or fileHandle must be set"
            );
        }
    }
}
