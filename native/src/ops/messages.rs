//! Message operations: sending, editing, deleting, reading, searching, forwarding, pinning and
//! reacting.

use grammers_client::media::InputMedia;
use grammers_client::message::InputMessage;
use grammers_client::message::InputReactions;
use grammers_client::tl;
use serde::Deserialize;
use serde_json::json;

use super::markup::reply_markup_from_spec;
use super::media::{apply_edit_media, EditMediaSpec};
use super::Handler;
use crate::client::{resolve_peer, resolve_upload, NativeClient};
use crate::dto::message::{message_dto, MessageCountDto};
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::{file_source, resolve_format, EntitySpec, MarkupSpec, PeerTarget};

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SendMessagePayload {
    #[serde(flatten)]
    peer: PeerTarget,
    text: String,
    reply_to_message_id: Option<i32>,
    silent: Option<bool>,
    link_preview: Option<bool>,
    /// `html`, `markdown` or `none` (the default): how the text is parsed into entities.
    parse_mode: Option<String>,
    /// Explicit formatting entities, which override the ones the parse mode derives.
    entities: Option<Vec<EntitySpec>>,
    markup: Option<MarkupSpec>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SendFilePayload {
    #[serde(flatten)]
    peer: PeerTarget,
    /// The local file to upload, when the send names a path rather than an upload handle.
    path: Option<String>,
    caption: Option<String>,
    as_photo: Option<bool>,
    reply_to_message_id: Option<i32>,
    silent: Option<bool>,
    markup: Option<MarkupSpec>,
    /// The handle of an upload that already ran, as an alternative to [Self::path]. Exactly one of
    /// the two must be set.
    file_handle: Option<i64>,
}

/// One item of an album.
///
/// An album item carries no markup: the `messages.SendMultiMedia` request the album is sent with
/// has no `reply_markup` field in the layer schema, so there is nowhere to attach one. A markup on
/// an album is refused by the layer rather than silently dropped, which is why the field is absent
/// here rather than ignored.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct AlbumItemPayload {
    /// The local file to upload, when the item names a path rather than an upload handle.
    path: Option<String>,
    caption: Option<String>,
    as_photo: Option<bool>,
    /// The handle of an upload that already ran, as an alternative to [Self::path]. Exactly one of
    /// the two must be set.
    file_handle: Option<i64>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SendAlbumPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    items: Vec<AlbumItemPayload>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct EditMessagePayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
    /// The new text. Absent keeps the message's current text, which is what a media-only edit
    /// sends; grammers drops an empty text from the request.
    text: Option<String>,
    /// `html`, `markdown` or `none` (the default): how [text] is parsed into entities. Ignored
    /// when [entities] is set.
    parse_mode: Option<String>,
    /// Explicit formatting entities, which override the ones the parse mode derives.
    entities: Option<Vec<EntitySpec>>,
    link_preview: Option<bool>,
    invert_media: Option<bool>,
    /// The media's time-to-live in seconds. Applied before the media, as grammers requires.
    ttl_seconds: Option<i32>,
    markup: Option<MarkupSpec>,
    /// Replaces the message's media; absent leaves it untouched.
    media: Option<EditMediaSpec>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct MessageIdsPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_ids: Vec<i32>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct HistoryPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    limit: Option<usize>,
    /// Paging cursor: return messages older than this ID. grammers' `MessageIter::offset_id`.
    offset_id: Option<i32>,
    /// Epoch milliseconds. grammers' `MessageIter::offset_date` takes an offset date in seconds, so
    /// this is the newest message the page may contain.
    max_date: Option<i64>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SearchMessagesPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    query: String,
    limit: Option<usize>,
    /// Paging cursor: return results older than this ID. grammers' `SearchIter::offset_id`.
    offset_id: Option<i32>,
    /// Restrict the result to the account's own messages. grammers' `SearchIter::sent_by_self`.
    sent_by_self: Option<bool>,
    /// Epoch milliseconds, inclusive lower bound. grammers' `SearchIter::min_date`.
    min_date: Option<i64>,
    /// Epoch milliseconds, exclusive upper bound. grammers' `SearchIter::max_date`.
    max_date: Option<i64>,
    /// One of the names [`messages_filter`] accepts.
    filter: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GlobalSearchPayload {
    query: String,
    limit: Option<usize>,
    /// Paging cursor: return results older than this ID. grammers' `GlobalSearchIter::offset_id`.
    offset_id: Option<i32>,
    /// One of the names [`messages_filter`] accepts.
    filter: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GlobalSearchTotalPayload {
    query: String,
    /// One of the names [`messages_filter`] accepts.
    filter: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ForwardMessagesPayload {
    destination: PeerTarget,
    source: PeerTarget,
    message_ids: Vec<i32>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct MessageIdPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ReactionPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
    emoji: Option<String>,
    remove: Option<bool>,
    big: Option<bool>,
}

#[cfg_attr(not(test), allow(dead_code))]
/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "sendMessage",
    "sendFile",
    "sendAlbum",
    "editMessage",
    "deleteMessages",
    "getHistory",
    "getHistoryTotal",
    "getChatPhotos",
    "getMessages",
    "getReplyToMessage",
    "searchMessages",
    "searchMessagesTotal",
    "searchAllMessages",
    "searchAllMessagesTotal",
    "forwardMessages",
    "getPinnedMessage",
    "pinMessage",
    "unpinMessage",
    "unpinAllMessages",
    "sendReaction",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "sendMessage" => send_message,
        "sendFile" => send_file,
        "sendAlbum" => send_album,
        "editMessage" => edit_message,
        "deleteMessages" => delete_messages,
        "getHistory" => get_history,
        "getHistoryTotal" => get_history_total,
        "getChatPhotos" => get_chat_photos,
        "getMessages" => get_messages,
        "getReplyToMessage" => get_reply_to_message,
        "searchMessages" => search_messages,
        "searchMessagesTotal" => search_messages_total,
        "searchAllMessages" => search_all_messages,
        "searchAllMessagesTotal" => search_all_messages_total,
        "forwardMessages" => forward_messages,
        "getPinnedMessage" => get_pinned_message,
        "pinMessage" => pin_message,
        "unpinMessage" => unpin_message,
        "unpinAllMessages" => unpin_all_messages,
        "sendReaction" => send_reaction,
        _ => return None,
    })
}

fn send_message(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SendMessagePayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let (text, entities) = resolve_format(
        &data.text,
        data.parse_mode.as_deref(),
        data.entities.as_deref(),
    )?;
    let mut message = InputMessage::new()
        .text(text)
        .fmt_entities(entities)
        .reply_to(data.reply_to_message_id)
        .silent(data.silent.unwrap_or(false))
        .link_preview(data.link_preview.unwrap_or(true));
    if let Some(markup) = &data.markup {
        message = message.reply_markup(reply_markup_from_spec(markup)?);
    }
    let message = native
        .runtime
        .block_on(native.client.send_message(peer, message))
        .map_err(invocation_error)?;
    json_string(message_dto(native, &message))
}

fn send_file(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SendFilePayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let uploaded = native.runtime.block_on(resolve_upload(
        native,
        file_source(data.path, data.file_handle)?,
        None,
    ))?;
    let mut message = InputMessage::new()
        .text(data.caption.unwrap_or_default())
        .reply_to(data.reply_to_message_id)
        .silent(data.silent.unwrap_or(false));
    if let Some(markup) = &data.markup {
        message = message.reply_markup(reply_markup_from_spec(markup)?);
    }
    let message = if data.as_photo.unwrap_or(false) {
        message.photo(uploaded)
    } else {
        message.file(uploaded)
    };
    let message = native
        .runtime
        .block_on(native.client.send_message(peer, message))
        .map_err(invocation_error)?;
    json_string(message_dto(native, &message))
}

fn send_album(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SendAlbumPayload = parse_payload(payload)?;
    if !(1..=10).contains(&data.items.len()) {
        return Err("an album must contain between 1 and 10 media items".to_owned());
    }
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let messages = native.runtime.block_on(async {
        let mut media = Vec::with_capacity(data.items.len());
        for item in data.items {
            let uploaded =
                resolve_upload(native, file_source(item.path, item.file_handle)?, None).await?;
            let input = InputMedia::new().caption(item.caption.unwrap_or_default());
            media.push(if item.as_photo.unwrap_or(false) {
                input.photo(uploaded)
            } else {
                input.file(uploaded)
            });
        }
        native
            .client
            .send_album(peer, media)
            .await
            .map_err(invocation_error)
    })?;
    json_string(
        messages
            .iter()
            .map(|message| message.as_ref().map(|message| message_dto(native, message)))
            .collect::<Vec<_>>(),
    )
}

fn edit_message(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: EditMessagePayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let text = data.text.as_deref().unwrap_or_default();
    let (text, entities) =
        resolve_format(text, data.parse_mode.as_deref(), data.entities.as_deref())?;
    let mut message = InputMessage::new()
        .text(text)
        .fmt_entities(entities)
        .link_preview(data.link_preview.unwrap_or(true))
        .invert_media(data.invert_media.unwrap_or(false));
    // grammers requires the TTL to be set before the media it applies to.
    if let Some(ttl_seconds) = data.ttl_seconds {
        message = message.media_ttl(ttl_seconds);
    }
    if let Some(markup) = &data.markup {
        message = message.reply_markup(reply_markup_from_spec(markup)?);
    }
    if let Some(media) = &data.media {
        message = native
            .runtime
            .block_on(apply_edit_media(native, message, media))?;
    }
    native
        .runtime
        .block_on(native.client.edit_message(peer, data.message_id, message))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

fn delete_messages(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: MessageIdsPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let deleted = native
        .runtime
        .block_on(native.client.delete_messages(peer, &data.message_ids))
        .map_err(invocation_error)?;
    json_string(json!({ "deleted": deleted }))
}

fn get_history(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: HistoryPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let limit = data.limit.unwrap_or(50).clamp(1, 100);
    let offset_id = data.offset_id;
    let max_date = data.max_date.map(epoch_seconds).transpose()?;
    let messages = native.runtime.block_on(async {
        let mut iterator = native.client.iter_messages(peer).limit(limit);
        if let Some(offset_id) = offset_id {
            iterator = iterator.offset_id(offset_id);
        }
        if let Some(max_date) = max_date {
            iterator = iterator.offset_date(max_date);
        }
        let mut result = Vec::new();
        while let Some(message) = iterator.next().await.map_err(invocation_error)? {
            result.push(message_dto(native, &message));
        }
        Ok::<_, String>(result)
    })?;
    json_string(messages)
}

/// Counts every message in a peer without returning them, through grammers' `MessageIter::total`.
fn get_history_total(native: &NativeClient, payload: &str) -> Result<String, String> {
    let target: PeerTarget = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &target))?;
    let total = native.runtime.block_on(async {
        native
            .client
            .iter_messages(peer)
            .total()
            .await
            .map_err(invocation_error)
    })?;
    json_string(MessageCountDto { total })
}

/// Lists the messages in a peer that carry a chat photo.
///
/// grammers 0.8.1 exposes no media filter on `MessageIter` (the history iterator); only a search
/// carries one, so this is a search restricted to the chat-photo filter rather than a filtered
/// history.
fn get_chat_photos(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: HistoryPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let limit = data.limit.unwrap_or(50).clamp(1, 100);
    let offset_id = data.offset_id;
    let messages = native.runtime.block_on(async {
        let mut iterator = native
            .client
            .search_messages(peer)
            .filter(tl::enums::MessagesFilter::InputMessagesFilterChatPhotos)
            .limit(limit);
        if let Some(offset_id) = offset_id {
            iterator = iterator.offset_id(offset_id);
        }
        let mut result = Vec::new();
        while let Some(message) = iterator.next().await.map_err(invocation_error)? {
            result.push(message_dto(native, &message));
        }
        Ok::<_, String>(result)
    })?;
    json_string(messages)
}

/// Resolves the message a given message replies to, through grammers' `get_reply_to_message`.
///
/// grammers fetches by a [`ClientMessage`], so the target is loaded by ID first; a message that no
/// longer exists, or is not in the peer, answers `null`.
fn get_reply_to_message(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: MessageIdPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let reply = native.runtime.block_on(async {
        let message = native
            .client
            .get_messages_by_id(peer, &[data.message_id])
            .await
            .map_err(invocation_error)?
            .into_iter()
            .flatten()
            .next();
        match message {
            Some(message) => native
                .client
                .get_reply_to_message(&message)
                .await
                .map_err(invocation_error),
            None => Ok(None),
        }
    })?;
    json_string(reply.as_ref().map(|message| message_dto(native, message)))
}

fn get_messages(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: MessageIdsPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    if data.message_ids.len() > 100 {
        return Err("at most 100 message IDs can be requested at once".to_owned());
    }
    let messages = native
        .runtime
        .block_on(native.client.get_messages_by_id(peer, &data.message_ids))
        .map_err(invocation_error)?
        .iter()
        .map(|message| message.as_ref().map(|message| message_dto(native, message)))
        .collect::<Vec<_>>();
    json_string(messages)
}

fn search_messages(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SearchMessagesPayload = parse_payload(payload)?;
    let limit = data.limit.unwrap_or(50).clamp(1, 100);
    let sent_by_self = data.sent_by_self.unwrap_or(false);
    let offset_id = data.offset_id;
    let filter = data.filter.as_deref().map(messages_filter).transpose()?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    // grammers takes the search date bounds as a `jiff::Timestamp`, which the bridge carries jiff
    // for; the bounds cross as epoch milliseconds.
    let messages = native.runtime.block_on(async {
        let mut iterator = native
            .client
            .search_messages(peer)
            .query(&data.query)
            .limit(limit);
        if let Some(offset_id) = offset_id {
            iterator = iterator.offset_id(offset_id);
        }
        if sent_by_self {
            iterator = iterator.sent_by_self();
        }
        if let Some(filter) = filter {
            iterator = iterator.filter(filter);
        }
        if let Some(min_date) = data.min_date {
            let min_date =
                jiff::Timestamp::from_millisecond(min_date).map_err(|e| e.to_string())?;
            iterator = iterator.min_date(min_date);
        }
        if let Some(max_date) = data.max_date {
            let max_date =
                jiff::Timestamp::from_millisecond(max_date).map_err(|e| e.to_string())?;
            iterator = iterator.max_date(max_date);
        }
        let mut result = Vec::new();
        while let Some(message) = iterator.next().await.map_err(invocation_error)? {
            result.push(message);
        }
        Ok::<_, String>(result)
    })?;
    json_string(
        messages
            .iter()
            .map(|message| message_dto(native, message))
            .collect::<Vec<_>>(),
    )
}

/// Counts the messages in a peer that match the same options [`search_messages`] accepts.
fn search_messages_total(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SearchMessagesPayload = parse_payload(payload)?;
    let sent_by_self = data.sent_by_self.unwrap_or(false);
    let filter = data.filter.as_deref().map(messages_filter).transpose()?;
    let query = data.query;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let total = native.runtime.block_on(async {
        let mut iterator = native.client.search_messages(peer).query(&query);
        if sent_by_self {
            iterator = iterator.sent_by_self();
        }
        if let Some(filter) = filter {
            iterator = iterator.filter(filter);
        }
        iterator.total().await.map_err(invocation_error)
    })?;
    json_string(MessageCountDto { total })
}

/// Searches the whole account, across peers, through grammers' `search_all_messages`.
fn search_all_messages(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GlobalSearchPayload = parse_payload(payload)?;
    let limit = data.limit.unwrap_or(50).clamp(1, 100);
    let offset_id = data.offset_id;
    let filter = data.filter.as_deref().map(messages_filter).transpose()?;
    let query = data.query;
    let messages = native.runtime.block_on(async {
        let mut iterator = native
            .client
            .search_all_messages()
            .query(&query)
            .limit(limit);
        if let Some(offset_id) = offset_id {
            iterator = iterator.offset_id(offset_id);
        }
        if let Some(filter) = filter {
            iterator = iterator.filter(filter);
        }
        let mut result = Vec::new();
        while let Some(message) = iterator.next().await.map_err(invocation_error)? {
            result.push(message_dto(native, &message));
        }
        Ok::<_, String>(result)
    })?;
    json_string(messages)
}

/// Counts the messages a global search matches, through `GlobalSearchIter::total`.
fn search_all_messages_total(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GlobalSearchTotalPayload = parse_payload(payload)?;
    let filter = data.filter.as_deref().map(messages_filter).transpose()?;
    let total = native.runtime.block_on(async {
        let mut iterator = native.client.search_all_messages().query(&data.query);
        if let Some(filter) = filter {
            iterator = iterator.filter(filter);
        }
        iterator.total().await.map_err(invocation_error)
    })?;
    json_string(MessageCountDto { total })
}

/// Maps a filter name to the grammers `MessagesFilter` variant it names.
///
/// The names are the Bot API spellings of the TL constructors, less the `inputMessagesFilter`
/// prefix, so `photoVideo` is `InputMessagesFilterPhotoVideo` and `chatPhotos` is
/// `InputMessagesFilterChatPhotos`.
fn messages_filter(name: &str) -> Result<tl::enums::MessagesFilter, String> {
    use tl::enums::MessagesFilter as Filter;
    Ok(match name {
        "empty" => Filter::InputMessagesFilterEmpty,
        "photos" => Filter::InputMessagesFilterPhotos,
        "video" => Filter::InputMessagesFilterVideo,
        "photoVideo" => Filter::InputMessagesFilterPhotoVideo,
        "document" => Filter::InputMessagesFilterDocument,
        "url" => Filter::InputMessagesFilterUrl,
        "gif" => Filter::InputMessagesFilterGif,
        "voice" => Filter::InputMessagesFilterVoice,
        "music" => Filter::InputMessagesFilterMusic,
        "chatPhotos" => Filter::InputMessagesFilterChatPhotos,
        "phoneCalls" => {
            Filter::InputMessagesFilterPhoneCalls(tl::types::InputMessagesFilterPhoneCalls {
                missed: false,
            })
        }
        "roundVoice" => Filter::InputMessagesFilterRoundVoice,
        "roundVideo" => Filter::InputMessagesFilterRoundVideo,
        "myMentions" => Filter::InputMessagesFilterMyMentions,
        "geo" => Filter::InputMessagesFilterGeo,
        "contacts" => Filter::InputMessagesFilterContacts,
        "pinned" => Filter::InputMessagesFilterPinned,
        other => return Err(format!("unknown message filter: {other}")),
    })
}

/// Converts an epoch-millisecond instant to the whole seconds the grammers paging requests use.
fn epoch_seconds(millis: i64) -> Result<i32, String> {
    i32::try_from(millis.div_euclid(1000))
        .map_err(|_| format!("date is outside the supported range: {millis}"))
}

fn forward_messages(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ForwardMessagesPayload = parse_payload(payload)?;
    if data.message_ids.len() > 100 {
        return Err("at most 100 messages can be forwarded at once".to_owned());
    }
    let destination = native
        .runtime
        .block_on(resolve_peer(native, &data.destination))?;
    let source = native
        .runtime
        .block_on(resolve_peer(native, &data.source))?;
    let messages = native
        .runtime
        .block_on(
            native
                .client
                .forward_messages(destination, &data.message_ids, source),
        )
        .map_err(invocation_error)?
        .iter()
        .map(|message| message.as_ref().map(|message| message_dto(native, message)))
        .collect::<Vec<_>>();
    json_string(messages)
}

fn get_pinned_message(native: &NativeClient, payload: &str) -> Result<String, String> {
    let target: PeerTarget = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &target))?;
    let message = native
        .runtime
        .block_on(native.client.get_pinned_message(peer))
        .map_err(invocation_error)?;
    json_string(message.as_ref().map(|message| message_dto(native, message)))
}

fn pin_message(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: MessageIdPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    native
        .runtime
        .block_on(native.client.pin_message(peer, data.message_id))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

fn unpin_message(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: MessageIdPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    native
        .runtime
        .block_on(native.client.unpin_message(peer, data.message_id))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

fn unpin_all_messages(native: &NativeClient, payload: &str) -> Result<String, String> {
    let target: PeerTarget = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &target))?;
    native
        .runtime
        .block_on(native.client.unpin_all_messages(peer))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

fn send_reaction(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ReactionPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let reactions = if data.remove.unwrap_or(false) {
        InputReactions::remove()
    } else {
        let emoji = data
            .emoji
            .filter(|emoji| !emoji.is_empty())
            .ok_or_else(|| "emoji is required unless remove is true".to_owned())?;
        let reactions = InputReactions::emoticon(emoji).add_to_recent();
        if data.big.unwrap_or(false) {
            reactions.big()
        } else {
            reactions
        }
    };
    native
        .runtime
        .block_on(
            native
                .client
                .send_reactions(peer, data.message_id, reactions),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

#[cfg(test)]
mod tests {
    use super::*;

    use crate::payload::{EntitySpec, MarkupSpec};

    /// The count operations answer this document, which Kotlin decodes as `MessageCount`.
    #[test]
    fn message_count_encodes_an_exact_document() {
        let encoded = json_string(MessageCountDto { total: 3 }).expect("a count encodes");
        assert_eq!(encoded, r#"{"total":3}"#);
    }

    #[test]
    fn history_paging_options_decode_from_the_payload() {
        let data: HistoryPayload =
            parse_payload(r#"{"peerHandle":7,"limit":20,"offsetId":31,"maxDate":1700000000000}"#)
                .expect("a history payload");

        assert_eq!(data.limit, Some(20));
        assert_eq!(data.offset_id, Some(31));
        assert_eq!(data.max_date, Some(1_700_000_000_000));
    }

    #[test]
    fn history_paging_options_default_to_absent() {
        let data: HistoryPayload =
            parse_payload(r#"{"username":"someone"}"#).expect("a history payload");

        assert_eq!(data.limit, None);
        assert_eq!(data.offset_id, None);
        assert_eq!(data.max_date, None);
    }

    #[test]
    fn search_options_decode_from_the_payload() {
        let data: SearchMessagesPayload = parse_payload(
            r#"{"peerHandle":1,"query":"hi","limit":10,"offsetId":5,"sentBySelf":true,
                "minDate":1000,"maxDate":2000,"filter":"photos"}"#,
        )
        .expect("a search payload");

        assert_eq!(data.query, "hi");
        assert_eq!(data.limit, Some(10));
        assert_eq!(data.offset_id, Some(5));
        assert_eq!(data.sent_by_self, Some(true));
        assert_eq!(data.min_date, Some(1000));
        assert_eq!(data.max_date, Some(2000));
        assert_eq!(data.filter.as_deref(), Some("photos"));
    }

    #[test]
    fn global_search_defaults_its_paging_options() {
        let data: GlobalSearchPayload =
            parse_payload(r#"{"query":"hi"}"#).expect("a global-search payload");

        assert_eq!(data.limit, None);
        assert_eq!(data.offset_id, None);
        assert_eq!(data.filter, None);

        let total: GlobalSearchTotalPayload =
            parse_payload(r#"{"query":"hi","filter":"pinned"}"#).expect("a total payload");
        assert_eq!(total.filter.as_deref(), Some("pinned"));
    }

    #[test]
    fn every_filter_name_maps_to_its_grammers_variant() {
        let cases: &[(&str, tl::enums::MessagesFilter)] = &[
            ("empty", tl::enums::MessagesFilter::InputMessagesFilterEmpty),
            (
                "photos",
                tl::enums::MessagesFilter::InputMessagesFilterPhotos,
            ),
            ("video", tl::enums::MessagesFilter::InputMessagesFilterVideo),
            (
                "photoVideo",
                tl::enums::MessagesFilter::InputMessagesFilterPhotoVideo,
            ),
            (
                "document",
                tl::enums::MessagesFilter::InputMessagesFilterDocument,
            ),
            ("url", tl::enums::MessagesFilter::InputMessagesFilterUrl),
            ("gif", tl::enums::MessagesFilter::InputMessagesFilterGif),
            ("voice", tl::enums::MessagesFilter::InputMessagesFilterVoice),
            ("music", tl::enums::MessagesFilter::InputMessagesFilterMusic),
            (
                "chatPhotos",
                tl::enums::MessagesFilter::InputMessagesFilterChatPhotos,
            ),
            (
                "phoneCalls",
                tl::enums::MessagesFilter::InputMessagesFilterPhoneCalls(
                    tl::types::InputMessagesFilterPhoneCalls { missed: false },
                ),
            ),
            (
                "roundVoice",
                tl::enums::MessagesFilter::InputMessagesFilterRoundVoice,
            ),
            (
                "roundVideo",
                tl::enums::MessagesFilter::InputMessagesFilterRoundVideo,
            ),
            (
                "myMentions",
                tl::enums::MessagesFilter::InputMessagesFilterMyMentions,
            ),
            ("geo", tl::enums::MessagesFilter::InputMessagesFilterGeo),
            (
                "contacts",
                tl::enums::MessagesFilter::InputMessagesFilterContacts,
            ),
            (
                "pinned",
                tl::enums::MessagesFilter::InputMessagesFilterPinned,
            ),
        ];

        for (name, expected) in cases {
            assert_eq!(
                messages_filter(name).unwrap_or_else(|_| panic!("{name} maps")),
                *expected,
                "{name} must map to its variant"
            );
        }
        assert_eq!(
            messages_filter("nope").unwrap_err(),
            "unknown message filter: nope"
        );
    }

    #[test]
    fn epoch_milliseconds_become_whole_seconds() {
        assert_eq!(epoch_seconds(1_700_000_000_999), Ok(1_700_000_000));
        assert_eq!(epoch_seconds(999), Ok(0));
        assert_eq!(epoch_seconds(-1), Ok(-1));
        assert_eq!(
            epoch_seconds(i64::MAX).unwrap_err(),
            format!("date is outside the supported range: {}", i64::MAX)
        );
    }

    #[test]
    fn every_declared_operation_routes_here() {
        for operation in OPERATIONS {
            assert!(
                route(operation).is_some(),
                "{operation} is declared by this module but not routed"
            );
        }
    }

    #[test]
    fn the_send_payloads_decode_a_markup_spec() {
        let data: SendMessagePayload = parse_payload(
            r#"{"peerHandle":1,"text":"hi","markup":{"kind":"hide","selective":true}}"#,
        )
        .expect("a send payload");
        assert!(matches!(
            data.markup,
            Some(MarkupSpec::Hide { selective: true })
        ));

        let data: SendFilePayload =
            parse_payload(r#"{"peerHandle":1,"path":"/tmp/a.pdf","markup":{"kind":"forceReply"}}"#)
                .expect("a send-file payload");
        assert!(matches!(data.markup, Some(MarkupSpec::ForceReply { .. })));

        // An absent markup is `None`, so a send without one is unchanged.
        let data: SendMessagePayload =
            parse_payload(r#"{"peerHandle":1,"text":"hi"}"#).expect("a send payload");
        assert!(data.markup.is_none());
    }

    #[test]
    fn the_send_payload_decodes_a_parse_mode_and_entities() {
        let data: SendMessagePayload = parse_payload(
            r#"{"peerHandle":1,"text":"<b>hi</b>","parseMode":"html",
                "entities":[{"offset":0,"length":2,"type":"bold"}]}"#,
        )
        .expect("a send payload");
        assert_eq!(data.parse_mode.as_deref(), Some("html"));
        assert_eq!(data.entities.as_ref().map(Vec::len), Some(1));
        assert_eq!(
            data.entities.as_ref().map(|e| e[0].entity_type.as_str()),
            Some("bold")
        );

        // Both are absent by default.
        let data: SendMessagePayload =
            parse_payload(r#"{"peerHandle":1,"text":"hi"}"#).expect("a send payload");
        assert!(data.parse_mode.is_none() && data.entities.is_none());
    }

    #[test]
    fn the_send_payloads_read_an_upload_handle_instead_of_a_path() {
        let data: SendFilePayload =
            parse_payload(r#"{"peerHandle":1,"fileHandle":7,"caption":"hi","asPhoto":true}"#)
                .expect("a send-file payload");
        assert_eq!(data.file_handle, Some(7));
        assert_eq!(data.path, None);

        let data: SendAlbumPayload = parse_payload(
            r#"{"peerHandle":1,"items":[{"fileHandle":7,"caption":"a"},{"path":"/tmp/b.pdf"}]}"#,
        )
        .expect("an album payload");
        assert_eq!(data.items[0].file_handle, Some(7));
        assert_eq!(data.items[0].path, None);
        assert_eq!(data.items[1].file_handle, None);
        assert_eq!(data.items[1].path.as_deref(), Some("/tmp/b.pdf"));
    }

    #[test]
    fn the_edit_payload_decodes_every_rich_option() {
        let data: EditMessagePayload = parse_payload(
            r#"{"peerHandle":1,"messageId":31,"text":"<b>hi</b>","parseMode":"html",
                "linkPreview":false,"invertMedia":true,"ttlSeconds":30,
                "markup":{"kind":"hide"},
                "media":{"path":"/tmp/a.pdf","kind":"file"}}"#,
        )
        .expect("an edit payload");
        assert_eq!(data.message_id, 31);
        assert_eq!(data.text.as_deref(), Some("<b>hi</b>"));
        assert_eq!(data.parse_mode.as_deref(), Some("html"));
        assert_eq!(data.link_preview, Some(false));
        assert_eq!(data.invert_media, Some(true));
        assert_eq!(data.ttl_seconds, Some(30));
        assert!(matches!(data.markup, Some(MarkupSpec::Hide { .. })));
        let media = data.media.expect("the media");
        assert_eq!(media.path.as_deref(), Some("/tmp/a.pdf"));
        assert_eq!(media.kind.as_deref(), Some("file"));

        // A copy-of media carries its source message.
        let data: EditMessagePayload = parse_payload(
            r#"{"username":"channel","messageId":31,
                "media":{"copyOf":{"peer":{"peerHandle":2},"messageId":7}}}"#,
        )
        .expect("an edit payload");
        let copy = data.media.expect("the media").copy_of.expect("a source");
        assert_eq!(copy.peer.peer_handle, Some(2));
        assert_eq!(copy.message_id, 7);
    }

    #[test]
    fn a_media_only_edit_leaves_the_text_absent() {
        let data: EditMessagePayload =
            parse_payload(r#"{"peerHandle":1,"messageId":31,"media":{"path":"/tmp/a.pdf"}}"#)
                .expect("an edit payload");
        assert!(data.text.is_none() && data.parse_mode.is_none() && data.entities.is_none());
        // The handler resolves an absent text to the empty string, which grammers drops.
        let (text, entities) = resolve_format("", None, None).expect("plain text");
        assert_eq!(text, "");
        assert!(entities.is_empty());
    }

    #[test]
    fn an_explicit_entity_list_overrides_the_parse_mode() {
        let entities =
            vec![
                parse_payload::<EntitySpec>(r#"{"offset":0,"length":2,"type":"italic"}"#)
                    .expect("an entity"),
            ];
        let (text, resolved) =
            resolve_format("<b>hi</b>", Some("html"), Some(&entities)).expect("a formatted text");
        assert_eq!(text, "hi");
        assert_eq!(
            resolved,
            vec![grammers_client::tl::enums::MessageEntity::Italic(
                grammers_client::tl::types::MessageEntityItalic {
                    offset: 0,
                    length: 2,
                }
            )]
        );

        // A bad parse mode is refused before any client call.
        assert_eq!(
            resolve_format("hi", Some("json"), None).unwrap_err(),
            "unsupported parse mode: json"
        );
    }
}
