package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the message operations. */

/**
 * Payload of `sendMessage`.
 *
 * The peer fields are flattened into the payload rather than nested, matching the native
 * [PeerTarget] shape.
 */
@Serializable
internal data class SendMessagePayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val text: String,
    val replyToMessageId: Int? = null,
    val silent: Boolean = false,
    val linkPreview: Boolean = true,
) {
    constructor(
        peer: PeerTarget,
        text: String,
        replyToMessageId: Int?,
        silent: Boolean,
        linkPreview: Boolean,
    ) : this(peer.peerHandle, peer.username, text, replyToMessageId, silent, linkPreview)
}

/** Payload of `sendFile`. */
@Serializable
internal data class SendFilePayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val path: String,
    val caption: String,
    val asPhoto: Boolean,
    val replyToMessageId: Int? = null,
    val silent: Boolean = false,
) {
    constructor(
        peer: PeerTarget,
        path: String,
        caption: String,
        asPhoto: Boolean,
        replyToMessageId: Int?,
        silent: Boolean,
    ) : this(peer.peerHandle, peer.username, path, caption, asPhoto, replyToMessageId, silent)
}

/** One entry of the `sendAlbum` media list. */
@Serializable
internal data class AlbumItemPayload(
    val path: String,
    val caption: String,
    val asPhoto: Boolean,
)

/** Payload of `sendAlbum`. */
@Serializable
internal data class SendAlbumPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val items: List<AlbumItemPayload>,
) {
    constructor(peer: PeerTarget, items: List<AlbumItemPayload>) : this(peer.peerHandle, peer.username, items)
}

/** Payload of `editMessage`. */
@Serializable
internal data class EditMessagePayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val messageId: Int,
    val text: String,
    val linkPreview: Boolean = true,
) {
    constructor(peer: PeerTarget, messageId: Int, text: String, linkPreview: Boolean) :
        this(peer.peerHandle, peer.username, messageId, text, linkPreview)
}

/** Payload of `deleteMessages` and `getMessages`. */
@Serializable
internal data class MessageIdsPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val messageIds: List<Int>,
) {
    constructor(peer: PeerTarget, messageIds: List<Int>) : this(peer.peerHandle, peer.username, messageIds)
}

/**
 * Payload of `getHistory` and `getChatPhotos`.
 *
 * [offsetId] is the paging cursor: only messages older than it are returned. [maxDate] is epoch
 * milliseconds and bounds the page to messages no newer than it; `getChatPhotos` ignores it.
 */
@Serializable
internal data class HistoryPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val limit: Int,
    val offsetId: Int? = null,
    val maxDate: Long? = null,
) {
    constructor(peer: PeerTarget, limit: Int, offsetId: Int? = null, maxDate: Long? = null) :
        this(peer.peerHandle, peer.username, limit, offsetId, maxDate)
}

/**
 * Payload of `searchMessages` and `searchMessagesTotal`.
 *
 * [sentBySelf] restricts the result to the account's own messages, [minDate] and [maxDate] bound it
 * in epoch milliseconds, and [filter] is one of the names a [MessageSearchFilter] carries. The
 * count operation ignores the paging fields.
 */
@Serializable
internal data class SearchMessagesPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val query: String,
    val limit: Int,
    val offsetId: Int? = null,
    val sentBySelf: Boolean = false,
    val minDate: Long? = null,
    val maxDate: Long? = null,
    val filter: String? = null,
) {
    constructor(
        peer: PeerTarget,
        query: String,
        limit: Int,
        offsetId: Int? = null,
        sentBySelf: Boolean = false,
        minDate: Long? = null,
        maxDate: Long? = null,
        filter: String? = null,
    ) : this(peer.peerHandle, peer.username, query, limit, offsetId, sentBySelf, minDate, maxDate, filter)
}

/** Payload of `searchAllMessages`. */
@Serializable
internal data class GlobalSearchPayload(
    val query: String,
    val limit: Int,
    val offsetId: Int? = null,
    val filter: String? = null,
)

/** Payload of `searchAllMessagesTotal`. */
@Serializable
internal data class GlobalSearchTotalPayload(
    val query: String,
    val filter: String? = null,
)

/** Result of the message-count operations: the size of the history or of the search result. */
@Serializable
internal data class MessageCount(val total: Int)

/** Payload of `forwardMessages`. */
@Serializable
internal data class ForwardMessagesPayload(
    val destination: PeerTarget,
    val source: PeerTarget,
    val messageIds: List<Int>,
)

/** Payload of `pinMessage` and `unpinMessage`. */
@Serializable
internal data class MessageIdPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val messageId: Int,
) {
    constructor(peer: PeerTarget, messageId: Int) : this(peer.peerHandle, peer.username, messageId)
}

/** Payload of `sendReaction`; [remove] takes precedence over [emoji]. */
@Serializable
internal data class ReactionPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val messageId: Int,
    val emoji: String? = null,
    val remove: Boolean = false,
    val big: Boolean = false,
) {
    constructor(peer: PeerTarget, messageId: Int, emoji: String?, remove: Boolean, big: Boolean) :
        this(peer.peerHandle, peer.username, messageId, emoji, remove, big)
}

/** Result of `deleteMessages`. */
@Serializable
internal data class DeleteResult(val deleted: Int)
