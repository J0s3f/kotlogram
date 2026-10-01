package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the typed sticker operations. */

/**
 * Payload of `messagesGetStickerSet`.
 *
 * A non-empty [shortName] wins over the [id]/[accessHash] pair; one of the two is required.
 */
@Serializable
internal data class GetStickerSetPayload(
    val id: Long? = null,
    val accessHash: Long? = null,
    val shortName: String? = null,
    val hash: Int = 0,
)

/** Payload of `messagesGetAllStickers` and `messagesGetFavedStickers`. */
@Serializable
internal data class GetStickersPayload(val hash: Long = 0)

/** Payload of `messagesGetRecentStickers`. */
@Serializable
internal data class GetRecentStickersPayload(
    /** When set, the answer holds the stickers attached to saved messages. */
    val attached: Boolean = false,
    val hash: Long = 0,
)

/**
 * Payload of `sendSticker`.
 *
 * The peer fields sit at the top level, which is the flattened shape the native side reads. The set
 * is named exactly as `messagesGetStickerSet` names it: a non-empty [shortName] wins over the
 * [id]/[accessHash] pair. [index] is the zero-based position of the sticker in the set's document
 * list, which is the order `messagesGetStickerSet` reports.
 */
@Serializable
internal data class SendStickerPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val shortName: String? = null,
    val id: Long? = null,
    val accessHash: Long? = null,
    val index: Int,
    val replyToMessageId: Int? = null,
    val silent: Boolean = false,
) {
    constructor(
        peer: PeerTarget,
        shortName: String?,
        id: Long?,
        accessHash: Long?,
        index: Int,
        replyToMessageId: Int?,
        silent: Boolean,
    ) : this(peer.peerHandle, peer.username, shortName, id, accessHash, index, replyToMessageId, silent)
}

/**
 * Payload of `messagesInstallStickerSet`.
 *
 * The set is named exactly as `messagesGetStickerSet` names it: a non-empty [shortName] wins over
 * the [id]/[accessHash] pair. [archived] makes the same call archive an installed set instead of
 * installing one, which is how the layer toggles the two directions.
 */
@Serializable
internal data class InstallStickerSetPayload(
    val shortName: String? = null,
    val id: Long? = null,
    val accessHash: Long? = null,
    val archived: Boolean = false,
)

/** Payload of `messagesUninstallStickerSet`, naming the set the same way again. */
@Serializable
internal data class UninstallStickerSetPayload(
    val shortName: String? = null,
    val id: Long? = null,
    val accessHash: Long? = null,
)

/** One sticker pack: an emoticon and the ids of the documents it groups. */
@Serializable
internal data class StickerPack(val emoticon: String, val documents: List<Long> = emptyList())

/**
 * The `stickerSet` summary.
 *
 * [installedDate] is epoch milliseconds, and the thumbnail travels as the identifiers the layer
 * reports rather than as bytes.
 */
@Serializable
internal data class StickerSet(
    val archived: Boolean = false,
    val official: Boolean = false,
    val masks: Boolean = false,
    val emojis: Boolean = false,
    val textColor: Boolean = false,
    val channelEmojiStatus: Boolean = false,
    val creator: Boolean = false,
    val installedDate: Long? = null,
    val id: Long,
    val accessHash: Long,
    val title: String,
    val shortName: String,
    val thumbDocumentId: Long? = null,
    val thumbDcId: Int? = null,
    val thumbVersion: Int? = null,
    val count: Int,
    val hash: Int,
    /** The packs the answer carried; empty for the list operations. */
    val packs: List<StickerPack> = emptyList(),
    /** The ids of the documents (stickers) the answer carried; empty for the list operations. */
    val documents: List<Long> = emptyList(),
)

/**
 * Result of `messagesGetStickerSet`.
 *
 * [notModified] is the layer's `messagesStickerSetNotModified`, which carries no set at all.
 */
@Serializable
internal data class StickerSetResult(val notModified: Boolean = false, val set: StickerSet? = null)

/** Result of `messagesGetAllStickers`. */
@Serializable
internal data class AllStickers(
    val notModified: Boolean = false,
    val hash: Long = 0,
    val sets: List<StickerSet> = emptyList(),
)

/** Result of `messagesGetRecentStickers`; [dates] matches [stickers] by index. */
@Serializable
internal data class RecentStickers(
    val notModified: Boolean = false,
    val hash: Long = 0,
    val packs: List<StickerPack> = emptyList(),
    val stickers: List<Long> = emptyList(),
    val dates: List<Long> = emptyList(),
)

/** Result of `messagesGetFavedStickers`. */
@Serializable
internal data class FavedStickers(
    val notModified: Boolean = false,
    val hash: Long = 0,
    val packs: List<StickerPack> = emptyList(),
    val stickers: List<Long> = emptyList(),
)

/** One set `messagesInstallStickerSet` archived instead of installing one. */
@Serializable
internal data class ArchivedStickerSet(
    val set: StickerSet,
    /** Absent when the layer's constructor names no cover. */
    val coverDocumentId: Long? = null,
)

/**
 * Result of `messagesInstallStickerSet`.
 *
 * The layer's success constructor is empty, so [installed] is true and [archivedSets] is empty
 * after a plain install: this layer has no answer naming the set that was just installed.
 */
@Serializable
internal data class StickerSetInstallResult(
    val installed: Boolean = false,
    val archivedSets: List<ArchivedStickerSet> = emptyList(),
)
