package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.AlbumItemPayload
import org.kotlogramme.protocol.DeleteResult
import org.kotlogramme.protocol.EditMediaSpec
import org.kotlogramme.protocol.EditMessagePayload
import org.kotlogramme.protocol.EntitySpec
import org.kotlogramme.protocol.ForwardMessagesPayload
import org.kotlogramme.protocol.GlobalSearchPayload
import org.kotlogramme.protocol.GlobalSearchTotalPayload
import org.kotlogramme.protocol.HistoryPayload
import org.kotlogramme.protocol.MarkupSpec
import org.kotlogramme.protocol.Message
import org.kotlogramme.protocol.MessageCount
import org.kotlogramme.protocol.MessageIdPayload
import org.kotlogramme.protocol.MessageIdsPayload
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.OutgoingMedia
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.ReactionPayload
import org.kotlogramme.protocol.SearchMessagesPayload
import org.kotlogramme.protocol.SendAlbumPayload
import org.kotlogramme.protocol.SendFilePayload
import org.kotlogramme.protocol.SendMessagePayload
import java.nio.file.Path

/** Sending, reading and curating the messages of a chat. */
internal interface MessagesBridge {
    val transport: Transport

    @Operation("sendMessage")
    fun sendMessage(
        peer: Peer,
        text: String,
        replyToMessageId: Int? = null,
        silent: Boolean = false,
        linkPreview: Boolean = true,
        replyMarkup: MarkupSpec? = null,
        parseMode: String? = null,
        entities: List<EntitySpec>? = null,
    ): Message = transport.request(
        "sendMessage",
        SendMessagePayload(
            PeerTarget(peer.nativeHandle),
            text,
            replyToMessageId,
            silent,
            linkPreview,
            replyMarkup,
            parseMode,
            entities,
        ),
    )

    /**
     * Sends a file as a document, or as a Telegram photo when [asPhoto] is true.
     *
     * Exactly one of [path] and [fileHandle] is set: a path uploads the local file now, a handle
     * reuses an upload that already ran.
     */
    @Operation("sendFile")
    fun sendFile(
        peer: Peer,
        path: Path? = null,
        caption: String = "",
        asPhoto: Boolean = false,
        replyToMessageId: Int? = null,
        silent: Boolean = false,
        replyMarkup: MarkupSpec? = null,
        fileHandle: Long? = null,
    ): Message = transport.request(
        "sendFile",
        SendFilePayload(
            PeerTarget(peer.nativeHandle),
            path?.toAbsolutePath()?.toString(),
            caption,
            asPhoto,
            replyToMessageId,
            silent,
            replyMarkup,
            fileHandle,
        ),
    )

    /** Sends one to ten files as a Telegram media album; each item names a path or a handle. */
    @Operation("sendAlbum")
    fun sendAlbum(peer: Peer, items: List<OutgoingMedia>): List<Message?> {
        require(items.size in 1..10) { "An album must contain between 1 and 10 media items" }
        return transport.request(
            "sendAlbum",
            SendAlbumPayload(
                PeerTarget(peer.nativeHandle),
                items.map {
                    AlbumItemPayload(
                        it.path?.toAbsolutePath()?.toString(),
                        it.caption,
                        it.asPhoto,
                        it.fileHandle,
                    )
                },
            ),
        )
    }

    @Operation("editMessage")
    fun editMessage(
        peer: Peer,
        messageId: Int,
        text: String? = null,
        linkPreview: Boolean = true,
        parseMode: String? = null,
        entities: List<EntitySpec>? = null,
        invertMedia: Boolean = false,
        ttlSeconds: Int? = null,
        replyMarkup: MarkupSpec? = null,
        media: EditMediaSpec? = null,
    ) {
        transport.request<EditMessagePayload, OperationResult>(
            "editMessage",
            EditMessagePayload(
                PeerTarget(peer.nativeHandle),
                messageId,
                text,
                linkPreview,
                parseMode,
                entities,
                invertMedia,
                ttlSeconds,
                replyMarkup,
                media,
            ),
        )
    }

    @Operation("deleteMessages")
    fun deleteMessages(peer: Peer, messageIds: Collection<Int>): Int =
        transport.request<MessageIdsPayload, DeleteResult>(
            "deleteMessages",
            MessageIdsPayload(PeerTarget(peer.nativeHandle), messageIds.toList()),
        ).deleted

    /**
     * Loads a page of the peer's history, newest first.
     *
     * [offsetId] continues from the oldest message of the previous page, and [maxDate] (epoch
     * milliseconds) bounds the page to messages no newer than it.
     */
    @Operation("getHistory")
    fun getHistory(peer: Peer, limit: Int = 50, offsetId: Int? = null, maxDate: Long? = null): List<Message> =
        transport.request(
            "getHistory",
            HistoryPayload(PeerTarget(peer.nativeHandle), limit, offsetId, maxDate),
        )

    /** Counts every message in the peer's history; grammers' `MessageIter::total`. */
    @Operation("getHistoryTotal")
    fun getHistoryTotal(peer: Peer): Int =
        transport.request<PeerTarget, MessageCount>("getHistoryTotal", PeerTarget(peer.nativeHandle)).total

    /**
     * Lists the peer's messages that carry a chat photo.
     *
     * grammers 0.8.1 exposes no media filter on the history iterator, so this is a search with the
     * chat-photo filter; [offsetId] continues from the oldest message of the previous page.
     */
    @Operation("getChatPhotos")
    fun getChatPhotos(peer: Peer, limit: Int = 50, offsetId: Int? = null): List<Message> =
        transport.request(
            "getChatPhotos",
            HistoryPayload(PeerTarget(peer.nativeHandle), limit, offsetId),
        )

    /** Resolves the message [messageId] replies to, or null when it replies to nothing. */
    @Operation("getReplyToMessage")
    fun getReplyToMessage(peer: Peer, messageId: Int): Message? =
        transport.request<MessageIdPayload, Message?>(
            "getReplyToMessage",
            MessageIdPayload(PeerTarget(peer.nativeHandle), messageId),
        )

    /** Loads selected messages; missing or inaccessible IDs are represented as null entries. */
    @Operation("getMessages")
    fun getMessages(peer: Peer, messageIds: Collection<Int>): List<Message?> = transport.request(
        "getMessages",
        MessageIdsPayload(PeerTarget(peer.nativeHandle), messageIds.toList()),
    )

    /**
     * Searches the text content of messages in a peer.
     *
     * [sentBySelf] restricts the result to the account's own messages, [minDate] and [maxDate] bound
     * it in epoch milliseconds, and [filter] is one of the names a `MessageSearchFilter` carries.
     */
    @Operation("searchMessages")
    fun searchMessages(
        peer: Peer,
        query: String,
        limit: Int = 50,
        offsetId: Int? = null,
        sentBySelf: Boolean = false,
        minDate: Long? = null,
        maxDate: Long? = null,
        filter: String? = null,
    ): List<Message> = transport.request(
        "searchMessages",
        SearchMessagesPayload(
            PeerTarget(peer.nativeHandle),
            query,
            limit,
            offsetId,
            sentBySelf,
            minDate,
            maxDate,
            filter,
        ),
    )

    /** Counts the messages in a peer that match the same options [searchMessages] accepts. */
    @Operation("searchMessagesTotal")
    fun searchMessagesTotal(peer: Peer, query: String, sentBySelf: Boolean = false, filter: String? = null): Int =
        transport.request<SearchMessagesPayload, MessageCount>(
            "searchMessagesTotal",
            SearchMessagesPayload(
                PeerTarget(peer.nativeHandle),
                query,
                1,
                sentBySelf = sentBySelf,
                filter = filter,
            ),
        ).total

    /** Searches the whole account for [query], across peers. */
    @Operation("searchAllMessages")
    fun searchAllMessages(query: String, limit: Int = 50, offsetId: Int? = null, filter: String? = null): List<Message> =
        transport.request("searchAllMessages", GlobalSearchPayload(query, limit, offsetId, filter))

    /** Counts the messages a global search matches. */
    @Operation("searchAllMessagesTotal")
    fun searchAllMessagesTotal(query: String, filter: String? = null): Int =
        transport.request<GlobalSearchTotalPayload, MessageCount>(
            "searchAllMessagesTotal",
            GlobalSearchTotalPayload(query, filter),
        ).total

    /** Forwards selected messages and preserves the input order; unavailable results are null. */
    @Operation("forwardMessages")
    fun forwardMessages(destination: Peer, messageIds: Collection<Int>, source: Peer): List<Message?> =
        transport.request(
            "forwardMessages",
            ForwardMessagesPayload(
                PeerTarget(destination.nativeHandle),
                PeerTarget(source.nativeHandle),
                messageIds.toList(),
            ),
        )

    @Operation("getPinnedMessage")
    fun getPinnedMessage(peer: Peer): Message? = transport.request<PeerTarget, Message?>(
        "getPinnedMessage",
        PeerTarget(peer.nativeHandle),
    )

    @Operation("pinMessage")
    fun pinMessage(peer: Peer, messageId: Int) {
        transport.request<MessageIdPayload, OperationResult>(
            "pinMessage",
            MessageIdPayload(PeerTarget(peer.nativeHandle), messageId),
        )
    }

    @Operation("unpinMessage")
    fun unpinMessage(peer: Peer, messageId: Int) {
        transport.request<MessageIdPayload, OperationResult>(
            "unpinMessage",
            MessageIdPayload(PeerTarget(peer.nativeHandle), messageId),
        )
    }

    @Operation("unpinAllMessages")
    fun unpinAllMessages(peer: Peer) {
        transport.request<PeerTarget, OperationResult>("unpinAllMessages", PeerTarget(peer.nativeHandle))
    }

    /** Adds or replaces the account's emoji reaction on a message. */
    @Operation("sendReaction")
    fun sendReaction(peer: Peer, messageId: Int, emoji: String, big: Boolean = false) {
        transport.request<ReactionPayload, OperationResult>(
            "sendReaction",
            ReactionPayload(PeerTarget(peer.nativeHandle), messageId, emoji, remove = false, big),
        )
    }

    /** Removes the account's reaction from a message. */
    @Operation("sendReaction")
    fun removeReaction(peer: Peer, messageId: Int) {
        transport.request<ReactionPayload, OperationResult>(
            "sendReaction",
            ReactionPayload(PeerTarget(peer.nativeHandle), messageId, emoji = null, remove = true, big = false),
        )
    }
}

/** The [MessagesBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class MessagesOperations(override val transport: Transport) : MessagesBridge
