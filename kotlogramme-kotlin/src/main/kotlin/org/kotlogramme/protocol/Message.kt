package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/**
 * A Telegram message, mirroring the accessors grammers exposes on a message.
 *
 * The raw-layer-only accessors — the format entities, the reply markup, the service action, the
 * forward header and the restriction reason — are left for a later phase: they hand out raw layer
 * values, which the bridge does not yet have a JSON shape for. A media caption is not among them;
 * grammers has no caption accessor at all, and the text of a captioned attachment is [text].
 *
 * [date] and [editDate] are epoch milliseconds. [peerId] and [senderId] are `null` for a message
 * grammers could not place, which happens for an empty message with no chat behind it.
 * [fromChannelPost] is grammers' `Message::post()`: the message is a post in a broadcast channel.
 */
@Serializable
data class Message(
    val id: Int,
    val text: String,
    val outgoing: Boolean,
    val replyToMessageId: Int? = null,
    val peerId: Long? = null,
    val senderId: Long? = null,
    val date: Long = 0,
    val editDate: Long? = null,
    val mentioned: Boolean = false,
    val mediaUnread: Boolean = false,
    val silent: Boolean = false,
    val pinned: Boolean = false,
    val fromChannelPost: Boolean = false,
    val fromScheduled: Boolean = false,
    val editHide: Boolean = false,
    val viaBotId: Long? = null,
    val postAuthor: String? = null,
    /** The album this message belongs to, if it is part of one. */
    val groupedId: Long? = null,
    val viewCount: Int? = null,
    val forwardCount: Int? = null,
    val replyCount: Int? = null,
    val reactionCount: Int? = null,
    val media: Media? = null,
)
