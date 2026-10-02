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
    val markup: MarkupSpec? = null,
    val parseMode: String? = null,
    val entities: List<EntitySpec>? = null,
) {
    constructor(
        peer: PeerTarget,
        text: String,
        replyToMessageId: Int?,
        silent: Boolean,
        linkPreview: Boolean,
        markup: MarkupSpec? = null,
        parseMode: String? = null,
        entities: List<EntitySpec>? = null,
    ) : this(peer.peerHandle, peer.username, text, replyToMessageId, silent, linkPreview, markup, parseMode, entities)
}

/**
 * Payload of `sendFile`.
 *
 * Exactly one of [path] and [fileHandle] is set: a path uploads the local file now, a handle
 * reuses an upload a `uploadBytes` or `uploadStreamFinish` already produced.
 */
@Serializable
internal data class SendFilePayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val path: String? = null,
    val caption: String,
    val asPhoto: Boolean,
    val replyToMessageId: Int? = null,
    val silent: Boolean = false,
    val markup: MarkupSpec? = null,
    val fileHandle: Long? = null,
) {
    constructor(
        peer: PeerTarget,
        path: String?,
        caption: String,
        asPhoto: Boolean,
        replyToMessageId: Int?,
        silent: Boolean,
        markup: MarkupSpec? = null,
        fileHandle: Long? = null,
    ) : this(peer.peerHandle, peer.username, path, caption, asPhoto, replyToMessageId, silent, markup, fileHandle)
}

/**
 * One entry of the `sendAlbum` media list.
 *
 * As with `sendFile`, exactly one of [path] and [fileHandle] is set.
 */
@Serializable
internal data class AlbumItemPayload(
    val path: String? = null,
    val caption: String,
    val asPhoto: Boolean,
    val fileHandle: Long? = null,
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

/**
 * One formatting entity an outgoing message carries, in the request direction.
 *
 * [type] names the layer's entity constructor without its `messageEntity` prefix, for example
 * `bold`, `pre` or `textUrl`. The variant-specific fields are absent on the types that cannot
 * answer them: this is the shape a future projection of the received entities will serialize, so a
 * message read back can be sent again without a second model.
 */
@Serializable
internal data class EntitySpec(
    val offset: Int,
    val length: Int,
    val type: String,
    val url: String? = null,
    val userId: Long? = null,
    val language: String? = null,
    val customEmojiId: Long? = null,
)

/**
 * The media an edit replaces the message's media with.
 *
 * Exactly one of [path], [url] or [copyOf] is set: a local file is uploaded, a URL is handed to
 * Telegram to download, and `copyOf` reuses the media of an existing message without a re-upload.
 * [kind] is `photo`, `document` (the default), `file` or `video`, as on the send side; the video
 * metadata only applies to a `video` upload.
 */
@Serializable
internal data class EditMediaSpec(
    val path: String? = null,
    val kind: String? = null,
    val url: String? = null,
    val copyOf: CopyOfSpec? = null,
    /** Video duration in seconds; only meaningful for [kind] = `video`. */
    val durationSeconds: Double? = null,
    /** Video width in pixels; only meaningful for [kind] = `video`. */
    val width: Int? = null,
    /** Video height in pixels; only meaningful for [kind] = `video`. */
    val height: Int? = null,
)

/** The message whose media an edit reuses. */
@Serializable
internal data class CopyOfSpec(val peer: PeerTarget, val messageId: Int)

/** Payload of `editMessage`. */
@Serializable
internal data class EditMessagePayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val messageId: Int,
    val text: String? = null,
    val linkPreview: Boolean = true,
    val parseMode: String? = null,
    val entities: List<EntitySpec>? = null,
    val invertMedia: Boolean = false,
    val ttlSeconds: Int? = null,
    val markup: MarkupSpec? = null,
    val media: EditMediaSpec? = null,
) {
    constructor(
        peer: PeerTarget,
        messageId: Int,
        text: String? = null,
        linkPreview: Boolean = true,
        parseMode: String? = null,
        entities: List<EntitySpec>? = null,
        invertMedia: Boolean = false,
        ttlSeconds: Int? = null,
        markup: MarkupSpec? = null,
        media: EditMediaSpec? = null,
    ) : this(
        peer.peerHandle,
        peer.username,
        messageId,
        text,
        linkPreview,
        parseMode,
        entities,
        invertMedia,
        ttlSeconds,
        markup,
        media,
    )
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

/**
 * Payload of `getChatPhotos`.
 *
 * Separate from [HistoryPayload] so that `getHistory`, which does not offer `--all`, keeps its
 * exact wire contract.
 *
 * [all] returns every chat photo in one unbounded walk. It wins over [limit] and [offsetId].
 */
@Serializable
internal data class ChatPhotosPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val limit: Int = 50,
    val offsetId: Int? = null,
    val all: Boolean = false,
) {
    constructor(peer: PeerTarget, limit: Int, offsetId: Int? = null, all: Boolean = false) :
        this(peer.peerHandle, peer.username, limit, offsetId, all)
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
