//! Message projection.

use grammers_client::media::Media as ClientMedia;
use grammers_client::message::Message as ClientMessage;
use grammers_client::peer::{Peer, RestrictionReason as ClientRestrictionReason};
use grammers_client::tl;
use grammers_session::types::PeerId;
use serde::Serialize;

use crate::client::NativeClient;
use crate::dto::action::{message_action_dto, MessageActionDto};
use crate::dto::markup::{reply_markup_dto, ReplyMarkupDto};
use crate::dto::media::{media_dto, MediaDto};
use crate::dto::peer::{peer_dto, PeerDto};
use crate::dto::user::{restriction_reason_dto, user_dto, RestrictionReasonDto, UserDto};

/// A Telegram message, mirroring the accessors grammers exposes on [`ClientMessage`].
///
/// The formatting entities grammers reports for the text are projected as [MessageEntityDto] and
/// the rendered text grammers derives from them as [Self::html_text] and [Self::markdown_text].
/// The reply markup, the service action, the forward and reply headers and the restriction reasons
/// are projected here too.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct MessageDto {
    pub(crate) id: i32,
    pub(crate) text: String,
    pub(crate) outgoing: bool,
    pub(crate) reply_to_message_id: Option<i32>,
    /// The chat the message belongs to, or `None` for a message grammers could not place.
    pub(crate) peer_id: Option<i64>,
    pub(crate) sender_id: Option<i64>,
    /// Epoch milliseconds, from `Message::date()`.
    pub(crate) date: i64,
    /// Epoch milliseconds, from `Message::edit_date()`.
    pub(crate) edit_date: Option<i64>,
    pub(crate) mentioned: bool,
    pub(crate) media_unread: bool,
    pub(crate) silent: bool,
    pub(crate) pinned: bool,
    /// grammers' `Message::post()`: this message is a post in a broadcast channel.
    pub(crate) from_channel_post: bool,
    pub(crate) from_scheduled: bool,
    pub(crate) edit_hide: bool,
    pub(crate) via_bot_id: Option<i64>,
    pub(crate) post_author: Option<String>,
    /// The album this message belongs to, if it is part of one.
    pub(crate) grouped_id: Option<i64>,
    pub(crate) view_count: Option<i32>,
    pub(crate) forward_count: Option<i32>,
    pub(crate) reply_count: Option<i32>,
    pub(crate) reaction_count: Option<i32>,
    pub(crate) media: Option<MediaDto>,
    /// The forward header, if this message was forwarded from another.
    pub(crate) forward_header: Option<ForwardHeaderDto>,
    /// The reply header, if this message is a reply to another.
    pub(crate) reply_header: Option<ReplyHeaderDto>,
    /// The reasons this message is restricted, empty when it is not.
    pub(crate) restriction_reasons: Vec<RestrictionReasonDto>,
    /// The service action, if this is a service message.
    pub(crate) action: Option<MessageActionDto>,
    /// The reply markup, if this message carries one (bot messages).
    pub(crate) reply_markup: Option<ReplyMarkupDto>,
    /// The full peer object for [Self::peer_id], registered so Kotlin can send to it.
    pub(crate) peer: Option<PeerDto>,
    /// The full sender object for [Self::sender_id], when the sender is a user.
    pub(crate) sender: Option<UserDto>,
    /// The formatting entities on [Self::text], empty when the text is unformatted.
    pub(crate) entities: Vec<MessageEntityDto>,
    /// [Self::text] rendered as HTML from [Self::entities], as grammers computes it.
    pub(crate) html_text: String,
    /// [Self::text] rendered as CommonMark from [Self::entities], as grammers computes it.
    pub(crate) markdown_text: String,
}

/// One formatting entity on a message's text, flattened like [MediaDto].
///
/// [Self::entity_type] is the layer's entity constructor without its `messageEntity` prefix, in
/// lowerCamelCase — `bold`, `pre`, `textUrl`, `mentionName`, `customEmoji` — which is the name an
/// [EntitySpec](crate::payload::EntitySpec) reads back, so a received entity can be sent again.
/// The extra fields are always present in the JSON and `null` on the kinds that do not carry them.
/// [Self::custom_emoji_id] is the layer's `document_id` for a custom-emoji entity.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct MessageEntityDto {
    /// The layer's entity type without its `messageEntity` prefix, in lowerCamelCase.
    #[serde(rename = "type")]
    pub(crate) entity_type: &'static str,
    pub(crate) offset: i32,
    pub(crate) length: i32,
    /// The target of a `textUrl` entity.
    pub(crate) url: Option<String>,
    /// The target of a `mentionName` entity, as a Bot API dialog id.
    pub(crate) user_id: Option<i64>,
    /// The language tag of a `pre` entity, empty when the layer carries none.
    pub(crate) language: Option<String>,
    /// The document behind a `customEmoji` entity.
    pub(crate) custom_emoji_id: Option<i64>,
}

impl MessageEntityDto {
    /// A projection with nothing but the kind and its span. Every arm starts here and fills in the
    /// fields its kind actually carries, which keeps the field list in one place.
    pub(crate) fn plain(entity_type: &'static str, offset: i32, length: i32) -> Self {
        Self {
            entity_type,
            offset,
            length,
            url: None,
            user_id: None,
            language: None,
            custom_emoji_id: None,
        }
    }
}

/// The header of a forwarded message, mirroring grammers' `MessageFwdHeader`.
///
/// The peer references are Bot API dialog ids rather than full objects: only the message's own
/// [Self::peer] and [Self::sender] are registered for sending, and a forward header's peers are
/// historical references that cannot be addressed.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ForwardHeaderDto {
    /// True when the message was forwarded from a chat the account is not in.
    pub(crate) imported: bool,
    /// True when the message was forwarded from an outgoing message.
    pub(crate) saved_out: bool,
    /// The peer the message was originally sent by, as a Bot API dialog id.
    pub(crate) from_id: Option<i64>,
    /// The name Telegram reports for the sender, when it is not a peer the account knows.
    pub(crate) from_name: Option<String>,
    /// Epoch milliseconds, from the layer's `date`.
    pub(crate) date: i64,
    /// The channel post id, when the forward is from a channel post.
    pub(crate) channel_post: Option<i32>,
    /// The author name Telegram reports for a channel post.
    pub(crate) post_author: Option<String>,
    /// The peer the message was forwarded from, as a Bot API dialog id.
    pub(crate) saved_from_peer: Option<i64>,
    /// The message id within [Self::saved_from_peer].
    pub(crate) saved_from_msg_id: Option<i32>,
    /// The peer the message was originally sent by, as a Bot API dialog id.
    pub(crate) saved_from_id: Option<i64>,
    /// The name Telegram reports for the original sender.
    pub(crate) saved_from_name: Option<String>,
    /// Epoch milliseconds, when the forward was saved.
    pub(crate) saved_date: Option<i64>,
    /// The public-service-announcement type Telegram reports for the forward.
    pub(crate) psa_type: Option<String>,
}

/// The header of a reply, mirroring grammers' `MessageReplyHeader`.
///
/// The layer has two reply-header constructors: a normal reply and a reply to a story. They share
/// no fields, so this is the same flat shape [MediaDto] uses: [kind] names the constructor and says
/// which of the other fields are populated, the rest being the defaults. Every field is always
/// present in the JSON.
///
/// `quote_entities` is absent: the message's own entities are projected, but the entities on the
/// quoted text are not yet.
///
/// [MediaDto]: crate::dto::media::MediaDto
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ReplyHeaderDto {
    /// `header` for a normal reply, `storyHeader` for a reply to a story.
    pub(crate) kind: &'static str,
    // Normal reply fields.
    /// True when the reply targets a scheduled message.
    pub(crate) reply_to_scheduled: bool,
    /// True when the reply belongs to a forum topic.
    pub(crate) forum_topic: bool,
    /// True when the reply quotes part of the original message.
    pub(crate) quote: bool,
    /// True when the reply targets an ephemeral message.
    pub(crate) reply_to_ephemeral: bool,
    /// The id of the message being replied to.
    pub(crate) reply_to_msg_id: Option<i32>,
    /// The peer the replied message belongs to, as a Bot API dialog id.
    pub(crate) reply_to_peer_id: Option<i64>,
    /// The forward header of the replied message, if it was itself forwarded.
    pub(crate) reply_from: Option<ForwardHeaderDto>,
    /// The media of the replied message, if it carried any.
    pub(crate) reply_media: Option<MediaDto>,
    /// The id of the top message in the thread being replied to.
    pub(crate) reply_to_top_id: Option<i32>,
    /// The quoted text, when [Self::quote] is set.
    pub(crate) quote_text: Option<String>,
    /// The offset of the quote within the original message's text.
    pub(crate) quote_offset: Option<i32>,
    /// The id of the to-do item being replied to.
    pub(crate) todo_item_id: Option<i32>,
    /// The poll option bytes, base64 because JSON has no byte string.
    pub(crate) poll_option: Option<String>,
    // Story reply fields.
    /// The peer the story belongs to, as a Bot API dialog id.
    pub(crate) story_peer: Option<i64>,
    /// The id of the story being replied to.
    pub(crate) story_id: Option<i32>,
}

/// How many messages an operation counted.
///
/// This is all grammers' `total()` reports: the size of the history or of the search result,
/// without the messages themselves. It is its own projection because the count operations answer
/// with it alone, and a Kotlin caller reads the one field.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct MessageCountDto {
    pub(crate) total: usize,
}

/// Projects a grammers message. Used by the message results, the dialogs and the updates.
///
/// [native] is needed to register the message's peer and sender handles, so Kotlin can send
/// messages back to them.
pub(crate) fn message_dto(native: &NativeClient, message: &ClientMessage) -> MessageDto {
    // grammers derives the peer, and through it the sender, from the chat a message was fetched
    // in. An empty message carries neither, so asking for them panics rather than returning none.
    let placed = !matches!(message.raw, tl::enums::Message::Empty(_));

    MessageDto {
        id: message.id(),
        text: message.text().to_owned(),
        outgoing: message.outgoing(),
        reply_to_message_id: message.reply_to_message_id(),
        peer_id: placed
            .then(|| message.peer_id().bot_api_dialog_id())
            .flatten(),
        sender_id: placed
            .then(|| message.sender())
            .flatten()
            .map(|sender| sender.id().bot_api_dialog_id())
            .flatten(),
        date: message.date().as_millisecond(),
        edit_date: message.edit_date().map(|date| date.as_millisecond()),
        mentioned: message.mentioned(),
        media_unread: message.media_unread(),
        silent: message.silent(),
        pinned: message.pinned(),
        from_channel_post: message.post(),
        from_scheduled: message.from_scheduled(),
        edit_hide: message.edit_hide(),
        via_bot_id: message.via_bot_id(),
        post_author: message.post_author().map(ToOwned::to_owned),
        grouped_id: message.grouped_id(),
        view_count: message.view_count(),
        forward_count: message.forward_count(),
        reply_count: message.reply_count(),
        reaction_count: message.reaction_count(),
        media: message.media().as_ref().map(media_dto),
        forward_header: message.forward_header().as_ref().map(forward_header_dto),
        reply_header: message.reply_header().as_ref().map(reply_header_dto),
        restriction_reasons: message
            .restriction_reason()
            .map(|reasons| {
                reasons
                    .iter()
                    .map(|reason| {
                        restriction_reason_dto(&ClientRestrictionReason::from_raw(reason))
                    })
                    .collect()
            })
            .unwrap_or_default(),
        action: message_action_dto(message),
        reply_markup: message.reply_markup().as_ref().map(reply_markup_dto),
        // Registering a peer can only fail when the registry is poisoned, which would fail every
        // operation; the message projection stays total and reports no peer in that case.
        peer: message.peer().and_then(|peer| peer_dto(native, peer).ok()),
        sender: message.sender().and_then(|peer| match peer {
            Peer::User(user) => Some(user_dto(user)),
            _ => None,
        }),
        // `fmt_entities` is `None` for an empty or service message and `Some` for a text message
        // with no formatting, so the absence is folded to the empty list a Kotlin caller reads.
        entities: message
            .fmt_entities()
            .map(|entities| entities.iter().map(message_entity_dto).collect())
            .unwrap_or_default(),
        html_text: message.html_text(),
        markdown_text: message.markdown_text(),
    }
}

/// Projects one formatting entity of a message's text.
///
/// The variants without an extra share [`MessageEntityDto::plain`]; the four that carry one fill
/// in exactly that field, and the rest of the layer's variants (a blockquote, a formatted date and
/// the diff markers) are named but carry no extra this bridge projects.
pub(crate) fn message_entity_dto(entity: &tl::enums::MessageEntity) -> MessageEntityDto {
    use tl::enums::MessageEntity as Entity;
    match entity {
        Entity::Unknown(entity) => MessageEntityDto::plain("unknown", entity.offset, entity.length),
        Entity::Mention(entity) => MessageEntityDto::plain("mention", entity.offset, entity.length),
        Entity::Hashtag(entity) => MessageEntityDto::plain("hashtag", entity.offset, entity.length),
        Entity::BotCommand(entity) => {
            MessageEntityDto::plain("botCommand", entity.offset, entity.length)
        }
        Entity::Url(entity) => MessageEntityDto::plain("url", entity.offset, entity.length),
        Entity::Email(entity) => MessageEntityDto::plain("email", entity.offset, entity.length),
        Entity::Bold(entity) => MessageEntityDto::plain("bold", entity.offset, entity.length),
        Entity::Italic(entity) => MessageEntityDto::plain("italic", entity.offset, entity.length),
        Entity::Code(entity) => MessageEntityDto::plain("code", entity.offset, entity.length),
        Entity::Pre(entity) => MessageEntityDto {
            language: Some(entity.language.clone()),
            ..MessageEntityDto::plain("pre", entity.offset, entity.length)
        },
        Entity::TextUrl(entity) => MessageEntityDto {
            url: Some(entity.url.clone()),
            ..MessageEntityDto::plain("textUrl", entity.offset, entity.length)
        },
        Entity::MentionName(entity) => MessageEntityDto {
            user_id: Some(entity.user_id),
            ..MessageEntityDto::plain("mentionName", entity.offset, entity.length)
        },
        // The input-only mention carries a full `InputUser`, which has no Bot API id to project,
        // so only its kind and span travel. The layer would not send one on a received message.
        Entity::InputMessageEntityMentionName(entity) => MessageEntityDto::plain(
            "inputMessageEntityMentionName",
            entity.offset,
            entity.length,
        ),
        Entity::Phone(entity) => MessageEntityDto::plain("phone", entity.offset, entity.length),
        Entity::Cashtag(entity) => MessageEntityDto::plain("cashtag", entity.offset, entity.length),
        Entity::Underline(entity) => {
            MessageEntityDto::plain("underline", entity.offset, entity.length)
        }
        Entity::Strike(entity) => MessageEntityDto::plain("strike", entity.offset, entity.length),
        Entity::BankCard(entity) => {
            MessageEntityDto::plain("bankCard", entity.offset, entity.length)
        }
        Entity::Spoiler(entity) => MessageEntityDto::plain("spoiler", entity.offset, entity.length),
        Entity::CustomEmoji(entity) => MessageEntityDto {
            custom_emoji_id: Some(entity.document_id),
            ..MessageEntityDto::plain("customEmoji", entity.offset, entity.length)
        },
        Entity::Blockquote(entity) => {
            MessageEntityDto::plain("blockquote", entity.offset, entity.length)
        }
        Entity::FormattedDate(entity) => {
            MessageEntityDto::plain("formattedDate", entity.offset, entity.length)
        }
        Entity::DiffInsert(entity) => {
            MessageEntityDto::plain("diffInsert", entity.offset, entity.length)
        }
        Entity::DiffReplace(entity) => {
            MessageEntityDto::plain("diffReplace", entity.offset, entity.length)
        }
        Entity::DiffDelete(entity) => {
            MessageEntityDto::plain("diffDelete", entity.offset, entity.length)
        }
    }
}

/// Projects the forward header of a message.
fn forward_header_dto(header: &tl::enums::MessageFwdHeader) -> ForwardHeaderDto {
    let tl::enums::MessageFwdHeader::Header(header) = header;
    ForwardHeaderDto {
        imported: header.imported,
        saved_out: header.saved_out,
        from_id: peer_dialog_id(&header.from_id),
        from_name: header.from_name.clone(),
        date: i64::from(header.date) * 1_000,
        channel_post: header.channel_post,
        post_author: header.post_author.clone(),
        saved_from_peer: peer_dialog_id(&header.saved_from_peer),
        saved_from_msg_id: header.saved_from_msg_id,
        saved_from_id: peer_dialog_id(&header.saved_from_id),
        saved_from_name: header.saved_from_name.clone(),
        saved_date: header.saved_date.map(|date| i64::from(date) * 1_000),
        psa_type: header.psa_type.clone(),
    }
}

/// Projects the reply header of a message.
fn reply_header_dto(header: &tl::enums::MessageReplyHeader) -> ReplyHeaderDto {
    match header {
        tl::enums::MessageReplyHeader::Header(header) => ReplyHeaderDto {
            kind: "header",
            reply_to_scheduled: header.reply_to_scheduled,
            forum_topic: header.forum_topic,
            quote: header.quote,
            reply_to_ephemeral: header.reply_to_ephemeral,
            reply_to_msg_id: header.reply_to_msg_id,
            reply_to_peer_id: peer_dialog_id(&header.reply_to_peer_id),
            reply_from: header.reply_from.as_ref().map(forward_header_dto),
            reply_media: header
                .reply_media
                .clone()
                .and_then(ClientMedia::from_raw)
                .as_ref()
                .map(media_dto),
            reply_to_top_id: header.reply_to_top_id,
            quote_text: header.quote_text.clone(),
            quote_offset: header.quote_offset,
            todo_item_id: header.todo_item_id,
            poll_option: header.poll_option.as_ref().map(|bytes| base64(bytes)),
            story_peer: None,
            story_id: None,
        },
        tl::enums::MessageReplyHeader::MessageReplyStoryHeader(header) => ReplyHeaderDto {
            kind: "storyHeader",
            reply_to_scheduled: false,
            forum_topic: false,
            quote: false,
            reply_to_ephemeral: false,
            reply_to_msg_id: None,
            reply_to_peer_id: None,
            reply_from: None,
            reply_media: None,
            reply_to_top_id: None,
            quote_text: None,
            quote_offset: None,
            todo_item_id: None,
            poll_option: None,
            story_peer: peer_dialog_id(&Some(header.peer.clone())),
            story_id: Some(header.story_id),
        },
    }
}

/// The Bot API dialog id of a layer peer, or `None` when the peer has no identifier.
fn peer_dialog_id(peer: &Option<tl::enums::Peer>) -> Option<i64> {
    peer.as_ref()
        .map(|peer| PeerId::from(peer).bot_api_dialog_id())
        .flatten()
}

/// Encodes [bytes] as base64, which is how a byte string travels through a JSON document.
///
/// The bridge may not take a dependency for it, and the encoding is the one
/// `java.util.Base64.getDecoder()` reads back on the other side of the JNI boundary.
fn base64(bytes: &[u8]) -> String {
    const ALPHABET: &[u8; 64] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let mut encoded = String::with_capacity(bytes.len().div_ceil(3) * 4);
    for chunk in bytes.chunks(3) {
        // The three bytes of a chunk are one 24-bit group, of which the last one may be missing;
        // the bits that are not there are written as zero and cut off again by the padding.
        let group = chunk.iter().enumerate().fold(0u32, |group, (index, byte)| {
            group | u32::from(*byte) << (16 - 8 * index)
        });
        for index in 0..=chunk.len() {
            let shift = 18 - 6 * index;
            encoded.push(char::from(ALPHABET[((group >> shift) & 0x3f) as usize]));
            if index == chunk.len() {
                // A short chunk is padded to a four-character group.
                for _ in chunk.len()..3 {
                    encoded.push('=');
                }
                break;
            }
        }
    }
    encoded
}
