//! Message projection.

use grammers_client::message::Message as ClientMessage;
use grammers_client::tl;
use serde::Serialize;

use crate::dto::media::{media_dto, MediaDto};

/// A Telegram message, mirroring the accessors grammers exposes on [`ClientMessage`].
///
/// The raw-layer-only accessors — the format entities, the reply markup, the service action, the
/// forward header and the restriction reason — are left for a later phase: they hand out
/// `grammers-tl-types` values, which the bridge does not yet have a JSON shape for.
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
pub(crate) fn message_dto(message: &ClientMessage) -> MessageDto {
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
    }
}
