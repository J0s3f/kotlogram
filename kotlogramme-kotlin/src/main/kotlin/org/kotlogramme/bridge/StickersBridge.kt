package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.AllStickers
import org.kotlogramme.protocol.FavedStickers
import org.kotlogramme.protocol.GetRecentStickersPayload
import org.kotlogramme.protocol.GetStickerSetPayload
import org.kotlogramme.protocol.GetStickersPayload
import org.kotlogramme.protocol.Message
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.RecentStickers
import org.kotlogramme.protocol.SendStickerPayload
import org.kotlogramme.protocol.StickerSetResult

/**
 * The typed sticker family, built on grammers' TL layer because it exposes no high-level stickers
 * API.
 *
 * Every operation here is get-only; installing, archiving and editing sets stay behind
 * `invokeRaw`.
 */
internal interface StickersBridge {
    val transport: Transport

    /**
     * Reads one sticker set.
     *
     * A non-empty [shortName] wins over the [id]/[accessHash] pair; one of the two is required.
     * A non-zero [hash] asks Telegram for the incremental answer, which reports
     * [StickerSetResult.notModified] when nothing changed.
     */
    @Operation("messagesGetStickerSet")
    fun messagesGetStickerSet(
        id: Long? = null,
        accessHash: Long? = null,
        shortName: String? = null,
        hash: Int = 0,
    ): StickerSetResult =
        transport.request("messagesGetStickerSet", GetStickerSetPayload(id, accessHash, shortName, hash))

    /** Lists the account's sticker sets. */
    @Operation("messagesGetAllStickers")
    fun messagesGetAllStickers(hash: Long = 0): AllStickers =
        transport.request("messagesGetAllStickers", GetStickersPayload(hash))

    /** Lists the stickers the account recently used. */
    @Operation("messagesGetRecentStickers")
    fun messagesGetRecentStickers(attached: Boolean = false, hash: Long = 0): RecentStickers =
        transport.request("messagesGetRecentStickers", GetRecentStickersPayload(attached, hash))

    /** Lists the stickers the account has favourited. */
    @Operation("messagesGetFavedStickers")
    fun messagesGetFavedStickers(hash: Long = 0): FavedStickers =
        transport.request("messagesGetFavedStickers", GetStickersPayload(hash))

    /**
     * Sends one sticker of a set.
     *
     * The set is named as in [messagesGetStickerSet] and [index] is the zero-based position of the
     * sticker in the set's document list. The document's file reference never crosses this
     * boundary: the native side reads it from the set it fetches first.
     */
    @Operation("sendSticker")
    fun sendSticker(
        peer: Peer,
        shortName: String? = null,
        id: Long? = null,
        accessHash: Long? = null,
        index: Int,
        replyToMessageId: Int? = null,
        silent: Boolean = false,
    ): Message = transport.request(
        "sendSticker",
        SendStickerPayload(PeerTarget(peer.nativeHandle), shortName, id, accessHash, index, replyToMessageId, silent),
    )
}

/** The [StickersBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class StickersOperations(override val transport: Transport) : StickersBridge
