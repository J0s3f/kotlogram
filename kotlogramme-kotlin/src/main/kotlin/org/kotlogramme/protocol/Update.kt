package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/**
 * One entry of the ordered grammers update stream.
 *
 * grammers splits its update stream into a handful of typed payloads and reports everything
 * Telegram sends that it does not model as a raw update. Those payloads share almost no accessors:
 * only the two message updates carry a message, only a deletion carries message ids, only a
 * callback query carries button data. This is therefore the same flat shape the bridge sends for
 * media — [kind] names the variant, the other fields are filled in for that variant and `null` for
 * the rest, and every field is always present.
 *
 * [kind] is the grammers `Update` variant in lowerCamelCase. A variant this build cannot name
 * reports `unknown` and still carries [rawUpdate], so an event the bridge cannot type stays
 * distinguishable from one that has no payload.
 *
 * [state] is set for every update: it is what the client resumes its stream from.
 */
@Serializable
data class Update(
    val kind: String,
    /** Set for the two message updates, which are the only ones that carry a message. */
    val message: Message? = null,
    val state: UpdateState? = null,
    /** The messages a deletion removed. */
    val deletedMessageIds: List<Int>? = null,
    /** The channel a deletion happened in, which only a channel deletion names. */
    val deletedChannelId: Long? = null,
    val callbackQuery: CallbackQueryUpdate? = null,
    val inlineQuery: InlineQueryUpdate? = null,
    val inlineSend: InlineSendUpdate? = null,
    val guestChatQuery: GuestChatQueryUpdate? = null,
    /** The update as Telegram sent it, for the variants that carry no typed payload. */
    val rawUpdate: RawUpdate? = null,
)

/**
 * The state grammers attaches to every update.
 *
 * [date] is epoch milliseconds, like every other date on the wire, while the state itself is the
 * pair of counters Telegram tracks the stream by. [messageBox] is only set for a message-related
 * sequence, and carries the sequence's own timestamp in [UpdateMessageBox.pts].
 */
@Serializable
data class UpdateState(
    val date: Long,
    val seq: Int,
    val messageBox: UpdateMessageBox? = null,
)

/**
 * The message box a state belongs to; grammers splits it into one per update sequence.
 *
 * [kind] is `common`, `secondary` or `channel`, and [channelId] is set for the channel sequence
 * only.
 */
@Serializable
data class UpdateMessageBox(
    val kind: String,
    val pts: Int,
    val channelId: Long? = null,
)

/**
 * A pressed inline button, mirroring grammers' `CallbackQuery`.
 *
 * [data] is the binary payload of the button as base64, because JSON has no byte string. grammers
 * exposes no accessor for [queryId] or for the message the button belongs to, so both are read
 * off the payload the update travelled in: [messageId] is `null` for an inline callback query and
 * [inlineMessageId] is `null` for a chat one, and exactly one of them is set.
 */
@Serializable
data class CallbackQueryUpdate(
    val data: String,
    val isFromInline: Boolean = false,
    val queryId: Long,
    val messageId: Int? = null,
    val inlineMessageId: InlineMessageId? = null,
    val peer: Peer,
    val sender: User,
)

/**
 * The identifier of an inline message, which has to be sent back to edit it.
 *
 * grammers keeps the 32-bit and the 64-bit constructor of it in one enum and only exposes the
 * access hash and the data centre, so [id] is read per constructor and widened here. The owner the
 * 64-bit constructor carries has no accessor and is absent.
 */
@Serializable
data class InlineMessageId(
    val dcId: Int,
    val accessHash: Long,
    val id: Long,
)

/**
 * An inline query a user sent to the bot, mirroring grammers' `InlineQuery`.
 *
 * [queryId] is what an inline answer is sent to, [offset] is the offset to answer the next page of
 * results with, and [peerType] names the kind of peer the query came from, for example `pm` or
 * `broadcast`.
 */
@Serializable
data class InlineQueryUpdate(
    val sender: User,
    val text: String,
    val offset: String,
    val queryId: Long,
    val peerType: String? = null,
)

/** A chosen inline result, mirroring grammers' `InlineSend`. [messageId] is set for a result with a keyboard. */
@Serializable
data class InlineSendUpdate(
    val sender: User,
    val text: String,
    val resultId: String,
    val messageId: InlineMessageId? = null,
)

/**
 * A guest-chat query, mirroring grammers' `GuestChatQuery`.
 *
 * [queryId] is what an answer is sent to, [message] is the message that mentioned the bot, and
 * [referenceMessages] are the messages the update carried, which the layer only sends when the
 * mention is a reply or a forwarded message.
 */
@Serializable
data class GuestChatQueryUpdate(
    val queryId: Long,
    val message: Message,
    val referenceMessages: List<Message> = emptyList(),
)

/**
 * The update exactly as Telegram sent it, which is all grammers exposes for an event it does not
 * type.
 *
 * [name] is the TL constructor name, for example `updateUserTyping`, so the variant is readable
 * without decoding the payload. [data] is the TL encoding of the update as base64; the
 * constructor id it starts with is the one [name] was read from. The generated TL types carry no
 * serde derives, so the bytes travel as a string rather than as an object.
 */
@Serializable
data class RawUpdate(
    val name: String,
    val data: String,
)
