package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.CopyOfSpec
import org.kotlogramme.protocol.EditMediaSpec
import org.kotlogramme.protocol.EntitySpec
import org.kotlogramme.protocol.OutgoingMedia as BridgeOutgoingMedia
import org.kotlogramme.protocol.PeerTarget
import java.nio.file.Path

/** Sending, editing, deleting, reading, searching, forwarding, pinning and reacting to messages. */
interface MessagesApi : BridgeApi {
    @Suppress("UNUSED_PARAMETER")
    fun messagesSendMessage(
        peer: TelegramPeer,
        message: String,
        randomId: Long = 0L,
        replyToMsgId: Int? = null,
        silent: Boolean = false,
        noWebpage: Boolean = false,
        replyMarkup: ReplyMarkup? = null,
        parseMode: CaptionParseMode = CaptionParseMode.NONE,
        entities: List<MessageEntity>? = null,
    ): Message = bridge.sendMessage(
        peer.native,
        message,
        replyToMessageId = replyToMsgId,
        silent = silent,
        linkPreview = !noWebpage,
        replyMarkup = replyMarkup?.asSpec(),
        parseMode = parseMode.wireName,
        entities = entities?.map { it.asSpec() },
    ).toCompatibility()

    /** Uploads and sends a local file; set [asPhoto] to let Telegram compress it as a photo. */
    fun messagesSendFile(
        peer: TelegramPeer,
        path: Path,
        caption: String = "",
        asPhoto: Boolean = false,
        replyToMsgId: Int? = null,
        silent: Boolean = false,
        replyMarkup: ReplyMarkup? = null,
    ): Message = bridge.sendFile(
        peer.native,
        path,
        caption,
        asPhoto,
        replyToMsgId,
        silent,
        replyMarkup = replyMarkup?.asSpec(),
    ).toCompatibility()

    /**
     * Sends a file already uploaded by this client, referencing it by [UploadedFile.handle] so it
     * is not uploaded a second time. The rest of the options are those [messagesSendFile] takes.
     */
    fun messagesSendFile(
        peer: TelegramPeer,
        file: UploadedFile,
        caption: String = "",
        asPhoto: Boolean = false,
        replyToMsgId: Int? = null,
        silent: Boolean = false,
        replyMarkup: ReplyMarkup? = null,
    ): Message = bridge.sendFile(
        peer.native,
        path = null,
        caption = caption,
        asPhoto = asPhoto,
        replyToMessageId = replyToMsgId,
        silent = silent,
        replyMarkup = replyMarkup?.asSpec(),
        fileHandle = requireNotNull(file.handle) { "the uploaded file carries no handle" },
    ).toCompatibility()

    /**
     * Sends one to ten files as an album. Each [OutgoingMedia] names a local path to upload or an
     * already-uploaded file's handle.
     */
    fun messagesSendAlbum(peer: TelegramPeer, items: List<OutgoingMedia>): List<Message?> =
        bridge.sendAlbum(
            peer.native,
            items.map { BridgeOutgoingMedia(it.path, it.caption, it.asPhoto, it.fileHandle) },
        ).map { it?.toCompatibility() }

    /**
     * Edits the message [id], replacing its text, its reply markup or its media.
     *
     * Every option defaults to "leave the message's own", so an edit that only changes one of them
     * carries nothing else. [message] is null on a media-only edit; the native side drops an empty
     * text, exactly as grammers does. [media] replaces the message's media and may name a local
     * file, a URL or another message's media.
     */
    fun messagesEditMessage(
        peer: TelegramPeer,
        id: Int,
        message: String? = null,
        noWebpage: Boolean = false,
        parseMode: CaptionParseMode = CaptionParseMode.NONE,
        entities: List<MessageEntity>? = null,
        invertMedia: Boolean = false,
        ttlSeconds: Int? = null,
        replyMarkup: ReplyMarkup? = null,
        media: EditMedia? = null,
    ) {
        bridge.editMessage(
            peer.native,
            id,
            text = message,
            linkPreview = !noWebpage,
            parseMode = parseMode.wireName,
            entities = entities?.map { it.asSpec() },
            invertMedia = invertMedia,
            ttlSeconds = ttlSeconds,
            replyMarkup = replyMarkup?.asSpec(),
            media = media?.asSpec(),
        )
    }

    fun messagesDeleteMessages(peer: TelegramPeer, ids: Collection<Int>): Int =
        bridge.deleteMessages(peer.native, ids)

    /** Loads a page of the peer's history. [offsetId] continues from the previous page's last id. */
    fun messagesGetHistory(
        peer: TelegramPeer,
        limit: Int = 50,
        offsetId: Int? = null,
        maxDate: Long? = null,
    ): List<Message> = bridge.getHistory(peer.native, limit, offsetId, maxDate).map { it.toCompatibility() }

    /** Counts every message in the peer's history. */
    fun messagesGetHistoryTotal(peer: TelegramPeer): Int = bridge.getHistoryTotal(peer.native)

    /** Lists the peer's messages that carry a chat photo. */
    fun messagesGetChatPhotos(peer: TelegramPeer, limit: Int = 50, offsetId: Int? = null): List<Message> =
        bridge.getChatPhotos(peer.native, limit, offsetId).map { it.toCompatibility() }

    /** Resolves the message [id] replies to, or null when it replies to nothing. */
    fun messagesGetReplyToMessage(peer: TelegramPeer, id: Int): Message? =
        bridge.getReplyToMessage(peer.native, id)?.toCompatibility()

    fun messagesGetMessages(peer: TelegramPeer, ids: Collection<Int>): List<Message?> =
        bridge.getMessages(peer.native, ids).map { it?.toCompatibility() }

    /**
     * Searches the text content of messages in a peer.
     *
     * [sentBySelf], [minDate] and [maxDate] (epoch milliseconds) and [filter] restrict the result.
     */
    fun messagesSearch(
        peer: TelegramPeer,
        query: String,
        limit: Int = 50,
        offsetId: Int? = null,
        sentBySelf: Boolean = false,
        minDate: Long? = null,
        maxDate: Long? = null,
        filter: MessageSearchFilter? = null,
    ): List<Message> = bridge.searchMessages(
        peer.native,
        query,
        limit,
        offsetId,
        sentBySelf,
        minDate,
        maxDate,
        filter?.wire,
    ).map { it.toCompatibility() }

    /** Counts the messages in a peer that match [messagesSearch]'s [sentBySelf] and [filter]. */
    fun messagesSearchTotal(
        peer: TelegramPeer,
        query: String,
        sentBySelf: Boolean = false,
        filter: MessageSearchFilter? = null,
    ): Int = bridge.searchMessagesTotal(peer.native, query, sentBySelf, filter?.wire)

    /** Searches the whole account for [query], across peers. */
    fun messagesSearchGlobal(
        query: String,
        limit: Int = 50,
        offsetId: Int? = null,
        filter: MessageSearchFilter? = null,
    ): List<Message> =
        bridge.searchAllMessages(query, limit, offsetId, filter?.wire).map { it.toCompatibility() }

    /** Counts the messages a global search matches. */
    fun messagesSearchGlobalTotal(query: String, filter: MessageSearchFilter? = null): Int =
        bridge.searchAllMessagesTotal(query, filter?.wire)

    fun messagesForwardMessages(toPeer: TelegramPeer, ids: Collection<Int>, fromPeer: TelegramPeer): List<Message?> =
        bridge.forwardMessages(toPeer.native, ids, fromPeer.native).map { it?.toCompatibility() }

    fun messagesGetPinnedMessage(peer: TelegramPeer): Message? =
        bridge.getPinnedMessage(peer.native)?.toCompatibility()

    fun messagesPinMessage(peer: TelegramPeer, id: Int) {
        bridge.pinMessage(peer.native, id)
    }

    fun messagesUnpinMessage(peer: TelegramPeer, id: Int) {
        bridge.unpinMessage(peer.native, id)
    }

    fun messagesUnpinAllMessages(peer: TelegramPeer) {
        bridge.unpinAllMessages(peer.native)
    }

    fun messagesSendReaction(peer: TelegramPeer, id: Int, emoji: String, big: Boolean = false) {
        bridge.sendReaction(peer.native, id, emoji, big)
    }

    fun messagesRemoveReaction(peer: TelegramPeer, id: Int) {
        bridge.removeReaction(peer.native, id)
    }
}

/** Converts this entity to the wire spec a send or edit payload carries. */
internal fun MessageEntity.asSpec(): EntitySpec =
    EntitySpec(
        offset = offset,
        length = length,
        type = type,
        url = url,
        userId = userId,
        language = language,
        customEmojiId = customEmojiId,
    )

/** Converts this media source to the wire spec an edit payload carries. */
internal fun EditMedia.asSpec(): EditMediaSpec = when (this) {
    is EditMedia.File -> EditMediaSpec(path = path.toAbsolutePath().toString(), kind = kind.wireName)
    is EditMedia.Url -> EditMediaSpec(url = url, kind = kind.wireName)
    is EditMedia.CopyOf ->
        EditMediaSpec(copyOf = CopyOfSpec(PeerTarget(peer.native.nativeHandle), messageId))
}
