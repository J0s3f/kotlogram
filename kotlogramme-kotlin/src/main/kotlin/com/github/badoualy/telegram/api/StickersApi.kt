package com.github.badoualy.telegram.api

/**
 * The typed sticker family, mapped onto grammers' TL layer.
 *
 * These operations have no high-level grammers equivalent, so the bridge builds the raw
 * `messages.*` requests; the facade keeps the same Kotlogram-shaped surface as the other domains.
 * Installing, archiving and removing a set are covered; editing a set's metadata is not.
 */
interface StickersApi : BridgeApi {
    /**
     * Reads one sticker set.
     *
     * A non-empty [shortName] wins over the [id]/[accessHash] pair. A non-zero [hash] asks for the
     * incremental answer, which reports [StickerSetResult.notModified] when nothing changed.
     */
    fun messagesGetStickerSet(
        id: Long? = null,
        accessHash: Long? = null,
        shortName: String? = null,
        hash: Int = 0,
    ): StickerSetResult =
        bridge.messagesGetStickerSet(id, accessHash, shortName, hash).toCompatibility()

    /** Lists the account's sticker sets. */
    fun messagesGetAllStickers(hash: Long = 0): AllStickers =
        bridge.messagesGetAllStickers(hash).toCompatibility()

    /** Lists the stickers the account recently used. */
    fun messagesGetRecentStickers(attached: Boolean = false, hash: Long = 0): RecentStickers =
        bridge.messagesGetRecentStickers(attached, hash).toCompatibility()

    /** Lists the stickers the account has favourited. */
    fun messagesGetFavedStickers(hash: Long = 0): FavedStickers =
        bridge.messagesGetFavedStickers(hash).toCompatibility()

    /**
     * Installs a sticker set, or archives an installed one when [archived] is set.
     *
     * The set is named as in [messagesGetStickerSet]. Telegram answers an install with no set at
     * all on this layer, so the result reports that the install happened; only an archive names the
     * sets it archived.
     */
    fun messagesInstallStickerSet(
        id: Long? = null,
        accessHash: Long? = null,
        shortName: String? = null,
        archived: Boolean = false,
    ): StickerSetInstall = bridge.installStickerSet(shortName, id, accessHash, archived)
        .toCompatibility()

    /** Removes an installed sticker set, named as in [messagesGetStickerSet]. */
    fun messagesUninstallStickerSet(
        id: Long? = null,
        accessHash: Long? = null,
        shortName: String? = null,
    ) {
        bridge.uninstallStickerSet(shortName, id, accessHash)
    }

    /**
     * Sends one sticker of a set.
     *
     * The set is named as in [messagesGetStickerSet] and [index] is the zero-based position of the
     * sticker in the set's document list.
     */
    fun messagesSendSticker(
        peer: TelegramPeer,
        shortName: String? = null,
        id: Long? = null,
        accessHash: Long? = null,
        index: Int,
        replyToMsgId: Int? = null,
        silent: Boolean = false,
    ): Message = bridge.sendSticker(
        peer.native,
        shortName,
        id,
        accessHash,
        index,
        replyToMsgId,
        silent,
    ).toCompatibility()
}
