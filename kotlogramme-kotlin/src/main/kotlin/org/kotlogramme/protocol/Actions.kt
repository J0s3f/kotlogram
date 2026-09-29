package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the chat-action operations. */

/**
 * Payload of `sendChatAction`.
 *
 * [progress] is the percentage an uploading status reports and is left out for the statuses that
 * carry none; [topicId] targets one forum topic instead of the whole chat.
 */
@Serializable
internal data class SendChatActionPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val action: String,
    val progress: Int? = null,
    val topicId: Int? = null,
) {
    constructor(peer: PeerTarget, action: String, progress: Int?, topicId: Int?) :
        this(peer.peerHandle, peer.username, action, progress, topicId)
}

/** Payload of `getMessageAction`. */
@Serializable
internal data class GetMessageActionPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val messageId: Int,
) {
    constructor(peer: PeerTarget, messageId: Int) : this(peer.peerHandle, peer.username, messageId)
}

/** Result of `getMessageAction`; an ordinary message reports no action at all. */
@Serializable
internal data class GetMessageActionResult(val action: MessageAction? = null)

/**
 * The service action of a message.
 *
 * grammers reports a service action as a raw layer value with no accessors, so [kind] is the whole
 * of the action: the lowerCamelCase name of the layer variant, for example `chatCreate` or
 * `pinMessage`. Every variant is named, so there is no `unknown` here.
 *
 * [messageId] is echoed from the message the action was read off, and [senderId] is the user who
 * caused the service message, or `null` when grammers placed no sender.
 */
@Serializable
data class MessageAction(
    val messageId: Int,
    val senderId: Long? = null,
    val kind: String,
)
