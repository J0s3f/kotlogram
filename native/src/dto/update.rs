//! Update projection.
//!
//! grammers splits its ordered update stream into a handful of typed payloads under
//! `grammers_client::update` and reports everything Telegram sends that it does not model
//! as [`Raw`](grammers_client::update::Raw). The typed payloads share almost no accessors:
//! only the two message updates carry a message, only a deletion carries message ids, only a
//! callback query carries button data. So this is the same flat shape [`MediaDto`] uses — one
//! object whose `kind` names the variant and whose other fields are filled in for that variant and
//! `null` for the rest. Every field is always present in the JSON.
//!
//! The state grammers attaches to every update, and the TL bytes behind a raw update, are
//! projected here as well, because they are the two parts of an update no typed payload describes
//! and the two `nextRawUpdate` and `syncUpdateState` exist to expose.
//!
//! [`MediaDto`]: crate::dto::media::MediaDto

use std::panic::{catch_unwind, AssertUnwindSafe};

use grammers_client::peer::{Peer, User as ClientUser};
use grammers_client::tl;
use grammers_client::tl::Serializable;
use grammers_client::update::Update;
use grammers_client::update::{CallbackQuery, GuestChatQuery, InlineQuery, InlineSend};
use grammers_session::updates::MessageBox;
use grammers_session::updates::State;
use serde::Serialize;

use crate::client::NativeClient;
use crate::dto::message::{message_dto, MessageDto};
use crate::dto::peer::{peer_dto, PeerDto};
use crate::dto::user::{user_dto, UserDto};

/// One entry of the ordered grammers update stream, flattened as described in the module docs.
///
/// [Self::kind] is the grammers `Update` variant in lowerCamelCase, or `unknown` for a variant this
/// build does not name; such an update still carries [Self::raw_update], so an event the bridge
/// cannot type stays distinguishable from one that has no payload.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UpdateDto {
    pub(crate) kind: &'static str,

    /// Set for the two message updates, which are the only ones that carry a message.
    pub(crate) message: Option<MessageDto>,

    /// The update state grammers attached, which is what a client resumes its stream from.
    pub(crate) state: Option<UpdateStateDto>,

    // Message deletions.
    pub(crate) deleted_message_ids: Option<Vec<i32>>,
    /// The channel the messages were deleted from, which only a channel deletion names.
    pub(crate) deleted_channel_id: Option<i64>,

    // Bot updates.
    pub(crate) callback_query: Option<CallbackQueryDto>,
    pub(crate) inline_query: Option<InlineQueryDto>,
    pub(crate) inline_send: Option<InlineSendDto>,
    pub(crate) guest_chat_query: Option<GuestChatQueryDto>,

    /// The TL update itself, for the variants that carry no typed payload.
    pub(crate) raw_update: Option<RawUpdateDto>,
}

impl UpdateDto {
    /// A projection with nothing but [kind] set. Every variant arm starts here and fills in the
    /// accessors its own kind has, which keeps the field list in one place.
    pub(crate) fn empty(kind: &'static str) -> Self {
        Self {
            kind,
            message: None,
            state: None,
            deleted_message_ids: None,
            deleted_channel_id: None,
            callback_query: None,
            inline_query: None,
            inline_send: None,
            guest_chat_query: None,
            raw_update: None,
        }
    }
}

/// The `State` grammers reports with an update.
///
/// [Self::date] is epoch milliseconds, like every other date on the wire, while the state itself is
/// the pair of counters Telegram tracks the stream by.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UpdateStateDto {
    pub(crate) date: i64,
    pub(crate) seq: i32,
    /// The message box this update belongs to, which the layer only sets for a message-related
    /// sequence.
    pub(crate) message_box: Option<UpdateMessageBoxDto>,
}

/// The message box a state belongs to. grammers splits it into three, one per update sequence.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UpdateMessageBoxDto {
    /// `common`, `secondary` or `channel`.
    pub(crate) kind: &'static str,
    pub(crate) pts: i32,
    /// The channel, for the channel sequence only.
    pub(crate) channel_id: Option<i64>,
}

/// A pressed inline button, mirroring grammers' `CallbackQuery`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct CallbackQueryDto {
    /// The binary payload of the pressed button, base64 because JSON has no byte string.
    pub(crate) data: String,
    /// True when the button belongs to an inline message rather than to a chat message.
    pub(crate) is_from_inline: bool,
    /// The identifier a callback answer is sent to.
    pub(crate) query_id: i64,
    /// The message the button belongs to, which an inline callback query does not have.
    pub(crate) message_id: Option<i32>,
    /// The inline message the button belongs to, which only an inline callback query has.
    pub(crate) inline_message_id: Option<InlineMessageIdDto>,
    pub(crate) peer: PeerDto,
    pub(crate) sender: UserDto,
}

/// An inline query a user sent to the bot, mirroring grammers' `InlineQuery`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InlineQueryDto {
    pub(crate) sender: UserDto,
    /// The text of the query.
    pub(crate) text: String,
    /// The offset to answer the next page of results with.
    pub(crate) offset: String,
    /// The identifier an inline answer is sent to.
    pub(crate) query_id: i64,
    /// The kind of peer the query came from, for example `pm` or `broadcast`.
    pub(crate) peer_type: Option<&'static str>,
}

/// A chosen inline result, mirroring grammers' `InlineSend`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InlineSendDto {
    pub(crate) sender: UserDto,
    /// The query the result was chosen from.
    pub(crate) text: String,
    /// The identifier of the chosen result.
    pub(crate) result_id: String,
    /// The identifier of the sent inline message, which only a result with a keyboard has.
    pub(crate) message_id: Option<InlineMessageIdDto>,
}

/// A guest-chat query, mirroring grammers' `GuestChatQuery`.
///
/// grammers exposes the query id, the message that mentioned the bot and the reference messages
/// the update carried; the raw update and the state are already covered by [UpdateDto]'s own
/// fields.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GuestChatQueryDto {
    /// The identifier an answer is sent to.
    pub(crate) query_id: i64,
    /// The message that mentioned the bot.
    pub(crate) message: MessageDto,
    /// The reference messages the update carried, which the layer only sends when the mention is
    /// a reply or a forwarded message.
    pub(crate) reference_messages: Vec<MessageDto>,
}

/// The identifier of an inline message, which has to be sent back to edit it.
///
/// grammers keeps both the 32-bit and the 64-bit constructor in one enum and only exposes the
/// access hash and the data centre, so the id is read per constructor and widened here; the owner
/// the 64-bit one carries has no accessor and is absent.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InlineMessageIdDto {
    pub(crate) dc_id: i32,
    pub(crate) access_hash: i64,
    pub(crate) id: i64,
}

/// The update exactly as Telegram sent it, which is all grammers exposes for an event it does not
/// type. The layer only generates TL types without their serde derives, so the update travels as
/// the bytes [`Serializable::to_bytes`] produces, base64 because JSON has no byte string.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RawUpdateDto {
    /// The TL constructor name, for example `updateNewMessage`, so the variant is readable without
    /// decoding the payload.
    pub(crate) name: &'static str,
    pub(crate) data: String,
}

/// The failure a bot update reports when grammers could not resolve a peer its payload names.
const MISSING_PEER: &str = "the update did not carry the peer grammers resolved for it";

/// Projects one entry of the ordered update stream.
pub(crate) fn update_dto(native: &NativeClient, update: &Update) -> Result<UpdateDto, String> {
    let mut dto = UpdateDto::empty(update_kind(update));
    // Every variant carries a state, so it is filled in before the payload is.
    dto.state = Some(update_state_dto(update.state()));
    match update {
        Update::NewMessage(message) => dto.message = Some(message_dto(native, message)),
        Update::MessageEdited(message) => dto.message = Some(message_dto(native, message)),
        Update::MessageDeleted(deletion) => {
            dto.deleted_message_ids = Some(deletion.messages().to_vec());
            dto.deleted_channel_id = deletion.channel_id();
        }
        Update::CallbackQuery(query) => {
            dto.callback_query = Some(callback_query_dto(native, query)?)
        }
        Update::InlineQuery(query) => dto.inline_query = Some(inline_query_dto(query)?),
        Update::InlineSend(send) => dto.inline_send = Some(inline_send_dto(send)?),
        Update::GuestChatQuery(query) => {
            dto.guest_chat_query = Some(guest_chat_query_dto(native, query)?)
        }
        // A raw update has no typed payload, so the TL bytes are all it is. `Update` is
        // `#[non_exhaustive]`, so a variant grammers adds after this bridge was written lands
        // here too, reporting `unknown` and carrying the same bytes rather than turning into an
        // update with nothing in it.
        _ => dto.raw_update = Some(raw_update_dto(update.raw())),
    }
    Ok(dto)
}

/// The grammers `Update` variant in lowerCamelCase, or `unknown` for one this build cannot name.
fn update_kind(update: &Update) -> &'static str {
    match update {
        Update::NewMessage(_) => "newMessage",
        Update::MessageEdited(_) => "messageEdited",
        Update::MessageDeleted(_) => "messageDeleted",
        Update::CallbackQuery(_) => "callbackQuery",
        Update::InlineQuery(_) => "inlineQuery",
        Update::InlineSend(_) => "inlineSend",
        Update::GuestChatQuery(_) => "guestChatQuery",
        Update::Raw(_) => "raw",
        _ => "unknown",
    }
}

/// Projects the state grammers reports with an update.
pub(crate) fn update_state_dto(state: &State) -> UpdateStateDto {
    UpdateStateDto {
        date: i64::from(state.date) * 1_000,
        seq: state.seq,
        message_box: state.message_box.as_ref().map(message_box_dto),
    }
}

/// Projects the update exactly as Telegram sent it.
pub(crate) fn raw_update_dto(update: &tl::enums::Update) -> RawUpdateDto {
    let bytes = update.to_bytes();
    RawUpdateDto {
        // Every TL value starts with its constructor id, and that id is what the schema names it
        // by, so the first four bytes are all the name costs. `(unknown)` is the layer's own
        // answer for a constructor this schema does not have.
        name: bytes
            .get(..4)
            .and_then(|head| <[u8; 4]>::try_from(head).ok())
            .map_or("(unknown)", |id| tl::name_for_id(u32::from_le_bytes(id))),
        data: base64(&bytes),
    }
}

fn message_box_dto(message_box: &MessageBox) -> UpdateMessageBoxDto {
    let (kind, pts, channel_id) = match message_box {
        MessageBox::Common { pts } => ("common", *pts, None),
        MessageBox::Secondary { qts } => ("secondary", *qts, None),
        MessageBox::Channel { channel_id, pts } => ("channel", *pts, Some(*channel_id)),
    };
    UpdateMessageBoxDto {
        kind,
        pts,
        channel_id,
    }
}

fn callback_query_dto(
    native: &NativeClient,
    query: &CallbackQuery,
) -> Result<CallbackQueryDto, String> {
    // grammers exposes no accessor for the identifier an answer is sent to or for the message the
    // button belongs to, so both are read off the payload's public `raw` field.
    let (query_id, message_id, inline_message_id) = match &query.raw {
        tl::enums::Update::BotCallbackQuery(raw) => (raw.query_id, Some(raw.msg_id), None),
        tl::enums::Update::InlineBotCallbackQuery(raw) => {
            (raw.query_id, None, Some(inline_message_id_dto(&raw.msg_id)))
        }
        // grammers only builds a `CallbackQuery` from the two variants above, so anything else
        // means the payload and the type it was built from disagree.
        _ => {
            return Err(
                "the callback query is neither a bot nor an inline callback query".to_owned(),
            )
        }
    };
    Ok(CallbackQueryDto {
        data: base64(query.data()),
        is_from_inline: query.is_from_inline(),
        query_id,
        message_id,
        inline_message_id,
        peer: peer_dto(
            native,
            resolved(|| query.peer())
                .flatten()
                .ok_or_else(missing_peer)?,
        )?,
        sender: peer_sender_dto(resolved(|| query.sender()).flatten())?,
    })
}

fn inline_query_dto(query: &InlineQuery) -> Result<InlineQueryDto, String> {
    Ok(InlineQueryDto {
        sender: sender_dto(resolved(|| query.sender()).flatten())?,
        text: query.text().to_owned(),
        offset: query.offset().to_owned(),
        query_id: query.query_id(),
        peer_type: query.peer_type().as_ref().map(peer_type_name),
    })
}

fn inline_send_dto(send: &InlineSend) -> Result<InlineSendDto, String> {
    Ok(InlineSendDto {
        sender: sender_dto(resolved(|| send.sender()).flatten())?,
        text: send.text().to_owned(),
        result_id: send.result_id().to_owned(),
        message_id: send.message_id().as_ref().map(inline_message_id_dto),
    })
}

fn guest_chat_query_dto(
    native: &NativeClient,
    query: &GuestChatQuery,
) -> Result<GuestChatQueryDto, String> {
    Ok(GuestChatQueryDto {
        query_id: query.query_id(),
        message: message_dto(native, &query.message),
        reference_messages: query
            .reference_messages
            .iter()
            .map(|message| message_dto(native, message))
            .collect(),
    })
}

/// The id of an inline message, which is an enum over the two TL constructors of it.
fn inline_message_id_dto(message_id: &tl::enums::InputBotInlineMessageId) -> InlineMessageIdDto {
    let id = match message_id {
        tl::enums::InputBotInlineMessageId::Id(id) => id.id,
        tl::enums::InputBotInlineMessageId::Id64(id) => i64::from(id.id),
    };
    InlineMessageIdDto {
        dc_id: message_id.dc_id(),
        access_hash: message_id.access_hash(),
        id,
    }
}

/// The kind of peer an inline query came from, which is how grammers names the layer's own
/// constructors. [tl::enums::InlineQueryPeerType] is exhaustive, so a constructor added after this
/// bridge was written fails the build here rather than being reported under a wrong name.
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

/// The user an inline update reports as its sender. grammers hands the user over directly here.
fn sender_dto(user: Option<&ClientUser>) -> Result<UserDto, String> {
    user.map(user_dto).ok_or_else(missing_peer)
}

/// The user a callback query reports as its sender. grammers hands over the peer, so a sender that
/// is a group or a channel is not one and is reported as missing.
fn peer_sender_dto(peer: Option<&Peer>) -> Result<UserDto, String> {
    match peer {
        Some(Peer::User(user)) => Ok(user_dto(user)),
        _ => Err(missing_peer()),
    }
}

fn missing_peer() -> String {
    MISSING_PEER.to_owned()
}

/// Runs a grammers accessor that resolves a peer out of the map that travelled with the update.
///
/// `CallbackQuery::sender`, `CallbackQuery::peer`, `InlineQuery::sender` and `InlineSend::sender`
/// all `unwrap` that lookup, so an update naming a peer it does not carry would abort the process
/// instead of failing one operation. The lookup is caught here and reported to the caller. The
/// panic message still reaches stderr on the way out, because the default hook runs before the
/// unwind does; silencing that would mean replacing the process-wide hook, which is not this
/// bridge's to replace.
fn resolved<T>(accessor: impl FnOnce() -> T) -> Option<T> {
    catch_unwind(AssertUnwindSafe(accessor)).ok()
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

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;

    /// Asserts that [dto] encodes to exactly [expected].
    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    fn full_update() -> UpdateDto {
        UpdateDto {
            kind: "callbackQuery",
            message: None,
            state: Some(UpdateStateDto {
                date: 1_700_000_000_000,
                seq: 12,
                message_box: Some(UpdateMessageBoxDto {
                    kind: "channel",
                    pts: 3,
                    channel_id: Some(-1_000_007),
                }),
            }),
            deleted_message_ids: None,
            deleted_channel_id: None,
            callback_query: Some(CallbackQueryDto {
                data: "ZGF0YQ==".to_owned(),
                is_from_inline: false,
                query_id: 900,
                message_id: Some(31),
                inline_message_id: None,
                peer: PeerDto {
                    native_handle: 12,
                    id: -1_000_007,
                    kind: "channel",
                    username: Some("channel".to_owned()),
                    name: Some("A Channel".to_owned()),
                    usernames: vec!["channel_alt".to_owned()],
                    is_megagroup: None,
                    has_photo: true,
                    permissions: None,
                },
                sender: full_user(),
            }),
            inline_query: Some(InlineQueryDto {
                sender: full_user(),
                text: "where is".to_owned(),
                offset: "10".to_owned(),
                query_id: 901,
                peer_type: Some("broadcast"),
            }),
            inline_send: Some(InlineSendDto {
                sender: full_user(),
                text: "where is".to_owned(),
                result_id: "result-1".to_owned(),
                message_id: Some(InlineMessageIdDto {
                    dc_id: 2,
                    access_hash: 77,
                    id: 88,
                }),
            }),
            guest_chat_query: None,
            raw_update: Some(RawUpdateDto {
                name: "updateUserTyping",
                data: "SQkAGg==".to_owned(),
            }),
        }
    }

    fn full_user() -> UserDto {
        UserDto {
            id: 7,
            username: Some("someone".to_owned()),
            first_name: Some("Some".to_owned()),
            last_name: None,
            full_name: "Some".to_owned(),
            usernames: Vec::new(),
            phone: None,
            photo_id: None,
            status: "offline",
            status_expires: None,
            last_seen: None,
            status_by_me: false,
            lang_code: None,
            is_self: false,
            contact: false,
            mutual_contact: false,
            deleted: false,
            is_bot: true,
            bot_privacy: false,
            bot_supports_chats: false,
            bot_inline_geo: false,
            bot_inline_placeholder: None,
            verified: false,
            restricted: false,
            support: false,
            scam: false,
            restriction_reasons: Vec::new(),
        }
    }

    /// Every field a [UpdateDto] can emit, which the tests below compare the encoding against.
    const UPDATE_FIELDS: &[&str] = &[
        "callbackQuery",
        "deletedChannelId",
        "deletedMessageIds",
        "guestChatQuery",
        "inlineQuery",
        "inlineSend",
        "kind",
        "message",
        "rawUpdate",
        "state",
    ];

    #[test]
    fn update_encodes_every_projected_field() {
        assert_json(
            &full_update(),
            json!({
                "kind": "callbackQuery",
                "message": null,
                "state": {
                    "date": 1_700_000_000_000i64,
                    "seq": 12,
                    "messageBox": {"kind": "channel", "pts": 3, "channelId": -1_000_007},
                },
                "deletedMessageIds": null,
                "deletedChannelId": null,
                "callbackQuery": {
                    "data": "ZGF0YQ==",
                    "isFromInline": false,
                    "queryId": 900,
                    "messageId": 31,
                    "inlineMessageId": null,
                    "peer": {
                        "nativeHandle": 12,
                        "id": -1_000_007,
                        "kind": "channel",
                        "username": "channel",
                        "name": "A Channel",
                        "usernames": ["channel_alt"],
                        "isMegagroup": null,
                        "hasPhoto": true,
                        "permissions": null,
                    },
                    "sender": json!(full_user()),
                },
                "inlineQuery": {
                    "sender": json!(full_user()),
                    "text": "where is",
                    "offset": "10",
                    "queryId": 901,
                    "peerType": "broadcast",
                },
                "inlineSend": {
                    "sender": json!(full_user()),
                    "text": "where is",
                    "resultId": "result-1",
                    "messageId": {"dcId": 2, "accessHash": 77, "id": 88},
                },
                "guestChatQuery": null,
                "rawUpdate": {"name": "updateUserTyping", "data": "SQkAGg=="},
            }),
        );
    }

    #[test]
    fn update_declares_exactly_the_projected_fields() {
        let mut names: Vec<String> = serde_json::to_value(UpdateDto::empty("unknown"))
            .expect("an update encodes")
            .as_object()
            .expect("an object")
            .keys()
            .cloned()
            .collect();
        names.sort();
        let mut expected: Vec<String> = UPDATE_FIELDS
            .iter()
            .map(|name| (*name).to_owned())
            .collect();
        expected.sort();
        assert_eq!(names, expected);
    }

    #[test]
    fn a_message_update_populates_only_its_message_and_state() {
        // Every field is always present, so a kind is distinguished by which ones stop being null.
        let update = UpdateDto {
            message: Some(MessageDto {
                id: 31,
                text: "hello".to_owned(),
                outgoing: false,
                reply_to_message_id: None,
                peer_id: Some(7),
                sender_id: Some(7),
                date: 1_700_000_000_000,
                edit_date: None,
                mentioned: false,
                media_unread: false,
                silent: false,
                pinned: false,
                from_channel_post: false,
                from_scheduled: false,
                edit_hide: false,
                via_bot_id: None,
                post_author: None,
                grouped_id: None,
                view_count: None,
                forward_count: None,
                reply_count: None,
                reaction_count: None,
                media: None,
                forward_header: None,
                reply_header: None,
                restriction_reasons: Vec::new(),
                action: None,
                reply_markup: None,
                peer: None,
                sender: None,
                entities: Vec::new(),
                html_text: "hello".to_owned(),
                markdown_text: "hello".to_owned(),
                quote: None,
            }),
            state: Some(update_state_dto(&State {
                date: 1_700_000_000,
                seq: 1,
                message_box: Some(MessageBox::Common { pts: 7 }),
            })),
            ..UpdateDto::empty("newMessage")
        };
        let encoded = serde_json::to_value(update).expect("an update encodes");
        let populated: serde_json::Map<String, serde_json::Value> = encoded
            .as_object()
            .expect("an object")
            .iter()
            .filter(|(name, value)| !value.is_null() && *name != "message" && *name != "state")
            .map(|(name, value)| (name.clone(), value.clone()))
            .collect();
        assert_eq!(
            serde_json::Value::Object(populated),
            json!({"kind": "newMessage"})
        );
        assert_eq!(encoded["message"]["id"], json!(31));
        assert_eq!(
            encoded["state"]["messageBox"],
            json!({"kind": "common", "pts": 7, "channelId": null})
        );
        // The date crosses as milliseconds, like every other date on the wire.
        assert_eq!(encoded["state"]["date"], json!(1_700_000_000_000i64));
    }

    #[test]
    fn a_channel_state_names_its_channel() {
        let state = update_state_dto(&State {
            date: 1_700_000_000,
            seq: 12,
            message_box: Some(MessageBox::Channel {
                channel_id: -1_000_007,
                pts: 3,
            }),
        });
        assert_json(
            &state,
            json!({
                "date": 1_700_000_000_000i64,
                "seq": 12,
                "messageBox": {"kind": "channel", "pts": 3, "channelId": -1_000_007},
            }),
        );
    }

    #[test]
    fn a_secondary_state_has_no_channel() {
        let state = update_state_dto(&State {
            date: 1_700_000_000,
            seq: 0,
            message_box: Some(MessageBox::Secondary { qts: 5 }),
        });
        assert_json(
            &state,
            json!({
                "date": 1_700_000_000_000i64,
                "seq": 0,
                "messageBox": {"kind": "secondary", "pts": 5, "channelId": null},
            }),
        );
    }

    #[test]
    fn a_state_without_a_message_box_carries_none() {
        let state = update_state_dto(&State {
            date: 1_700_000_000,
            seq: 4,
            message_box: None,
        });
        assert_json(
            &state,
            json!({"date": 1_700_000_000_000i64, "seq": 4, "messageBox": null}),
        );
    }

    #[test]
    fn a_raw_update_is_named_by_its_constructor() {
        let update = || {
            tl::enums::Update::UserTyping(tl::types::UpdateUserTyping {
                user_id: 7,
                top_msg_id: None,
                action: tl::enums::SendMessageAction::SendMessageTypingAction,
            })
        };
        let dto = raw_update_dto(&update());
        // The name is read from the constructor id the TL encoding starts with.
        assert_eq!(dto.name, "updateUserTyping");
        assert_eq!(constructor_id(&update().to_bytes()), Some(0x2A17_BF5C));
        // The data is that same encoding, base64 because JSON has no byte string.
        assert_eq!(dto.data, base64(&update().to_bytes()));
    }

    /// The constructor id a TL encoding starts with, or `None` when it is too short to hold one.
    fn constructor_id(bytes: &[u8]) -> Option<u32> {
        Some(u32::from_le_bytes(bytes.get(..4)?.try_into().ok()?))
    }

    #[test]
    fn a_deleted_messages_update_is_named_by_its_constructor() {
        let dto = raw_update_dto(&tl::enums::Update::DeleteMessages(
            tl::types::UpdateDeleteMessages {
                messages: vec![1, 2],
                pts: 3,
                pts_count: 2,
            },
        ));
        assert_eq!(dto.name, "updateDeleteMessages");
    }

    #[test]
    fn base64_matches_the_reference_vectors() {
        // The test vectors of RFC 4648, which is what `java.util.Base64` reads and writes.
        for (bytes, expected) in [
            (&b""[..], ""),
            (b"f", "Zg=="),
            (b"fo", "Zm8="),
            (b"foo", "Zm9v"),
            (b"foob", "Zm9vYg=="),
            (b"fooba", "Zm9vYmE="),
            (b"foobar", "Zm9vYmFy"),
        ] {
            assert_eq!(
                base64(bytes),
                expected,
                "{bytes:?} does not encode as {expected}"
            );
        }
        // A payload of every byte value, which is what a button's data can be.
        let every: Vec<u8> = (0..=u8::MAX).collect();
        assert_eq!(base64(&every), "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+/w==");
    }
}
