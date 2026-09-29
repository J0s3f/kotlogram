package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.OutgoingMedia as BridgeOutgoingMedia
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
    ): Message = bridge.sendMessage(
        peer.native,
        message,
        replyToMessageId = replyToMsgId,
        silent = silent,
        linkPreview = !noWebpage,
    ).toCompatibility()

    /** Uploads and sends a local file; set [asPhoto] to let Telegram compress it as a photo. */
    fun messagesSendFile(
        peer: TelegramPeer,
        path: Path,
        caption: String = "",
        asPhoto: Boolean = false,
        replyToMsgId: Int? = null,
        silent: Boolean = false,
    ): Message = bridge.sendFile(peer.native, path, caption, asPhoto, replyToMsgId, silent).toCompatibility()

    fun messagesSendAlbum(peer: TelegramPeer, items: List<OutgoingMedia>): List<Message?> =
        bridge.sendAlbum(peer.native, items.map { BridgeOutgoingMedia(it.path, it.caption, it.asPhoto) })
            .map { it?.toCompatibility() }

    fun messagesEditMessage(peer: TelegramPeer, id: Int, message: String, noWebpage: Boolean = false) {
        bridge.editMessage(peer.native, id, message, linkPreview = !noWebpage)
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
