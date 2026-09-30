package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/**
 * A Telegram message, mirroring the accessors grammers exposes on a message.
 *
 * [entities] are the formatting entities on [text], empty when it is unformatted, and [htmlText]
 * and [markdownText] are the same text rendered from those entities by grammers. A media caption
 * is not among them; grammers has no caption accessor at all, and the text of a captioned
 * attachment is [text].
 *
 * [date] and [editDate] are epoch milliseconds. [peerId] and [senderId] are `null` for a message
 * grammers could not place, which happens for an empty message with no chat behind it.
 * [fromChannelPost] is grammers' `Message::post()`: the message is a post in a broadcast channel.
 *
 * [peer] and [sender] are the full objects for [peerId] and [senderId], registered so Kotlin can
 * send messages back to them. [sender] is `null` when the sender is not a user (a group or channel
 * posting anonymously).
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
    /** The forward header, if this message was forwarded from another. */
    val forwardHeader: ForwardHeader? = null,
    /** The reply header, if this message is a reply to another. */
    val replyHeader: ReplyHeader? = null,
    /** The reasons this message is restricted, empty when it is not. */
    val restrictionReasons: List<RestrictionReason> = emptyList(),
    /** The service action, if this is a service message. */
    val action: MessageAction? = null,
    /** The reply markup, if this message carries one (bot messages). */
    val replyMarkup: ReplyMarkup? = null,
    /** The full peer object for [peerId], registered so Kotlin can send to it. */
    val peer: Peer? = null,
    /** The full sender object for [senderId], when the sender is a user. */
    val sender: User? = null,
    /** The formatting entities on [text], empty when the text is unformatted. */
    val entities: List<MessageEntity> = emptyList(),
    /** [text] rendered as HTML from [entities], as grammers computes it. */
    val htmlText: String = "",
    /** [text] rendered as CommonMark from [entities], as grammers computes it. */
    val markdownText: String = "",
)

/**
 * One formatting entity on a message's text.
 *
 * [type] is the layer's entity constructor without its `messageEntity` prefix, in lowerCamelCase:
 * `bold`, `pre`, `textUrl`, `mentionName`, `customEmoji`. It is also the name an [EntitySpec]
 * reads, so a received entity can be handed back to a send. The extra fields are `null` on the
 * kinds that do not carry them, and every field travels for every kind.
 */
@Serializable
data class MessageEntity(
    val type: String,
    val offset: Int,
    val length: Int,
    /** The target of a `textUrl` entity. */
    val url: String? = null,
    /** The target of a `mentionName` entity, as a Bot API dialog id. */
    val userId: Long? = null,
    /** The language tag of a `pre` entity, empty when the layer carries none. */
    val language: String? = null,
    /** The document behind a `customEmoji` entity. */
    val customEmojiId: Long? = null,
)

/**
 * The header of a forwarded message, mirroring grammers' `MessageFwdHeader`.
 *
 * The peer references are Bot API dialog ids rather than full objects: only the message's own
 * [Message.peer] and [Message.sender] are registered for sending, and a forward header's peers are
 * historical references that cannot be addressed.
 *
 * [date] and [savedDate] are epoch milliseconds.
 */
@Serializable
data class ForwardHeader(
    val imported: Boolean = false,
    val savedOut: Boolean = false,
    val fromId: Long? = null,
    val fromName: String? = null,
    val date: Long = 0,
    val channelPost: Int? = null,
    val postAuthor: String? = null,
    val savedFromPeer: Long? = null,
    val savedFromMsgId: Int? = null,
    val savedFromId: Long? = null,
    val savedFromName: String? = null,
    val savedDate: Long? = null,
    val psaType: String? = null,
)

/**
 * The header of a reply, mirroring grammers' `MessageReplyHeader`.
 *
 * The layer has two reply-header constructors: a normal reply and a reply to a story. They share
 * no fields, so this is the same flat shape [Media] uses: [kind] names the constructor and says
 * which of the other fields are populated, the rest being the defaults. Every field is always
 * present in the JSON.
 *
 * `quoteEntities` is absent: the message's own entities are projected, but the entities on the
 * quoted text are not yet. [pollOption] is the poll option bytes, base64 because JSON has no byte
 * string.
 */
@Serializable
data class ReplyHeader(
    /** `header` for a normal reply, `storyHeader` for a reply to a story. */
    val kind: String,
    // Normal reply fields.
    val replyToScheduled: Boolean = false,
    val forumTopic: Boolean = false,
    val quote: Boolean = false,
    val replyToEphemeral: Boolean = false,
    val replyToMsgId: Int? = null,
    val replyToPeerId: Long? = null,
    val replyFrom: ForwardHeader? = null,
    val replyMedia: Media? = null,
    val replyToTopId: Int? = null,
    val quoteText: String? = null,
    val quoteOffset: Int? = null,
    val todoItemId: Int? = null,
    val pollOption: String? = null,
    // Story reply fields.
    val storyPeer: Long? = null,
    val storyId: Int? = null,
)


