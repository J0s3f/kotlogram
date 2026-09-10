package org.kotlogramme

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Synchronous Kotlin/JVM facade over the grammers Telegram client.
 *
 * The native side owns the Tokio runtime and the grammers client. Calls are safe to make from
 * different JVM threads; a single client should still be closed exactly once.
 */
class TelegramClient private constructor(
    private val handle: Long,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    /** Returns whether this session is already authorized with Telegram. */
    fun isAuthorized(): Boolean = call {
        val result = Native.isAuthorized(handle)
        result.toBooleanStrictOrNull() ?: throw TelegramException(result)
    }

    /** Signs in a bot using a token created by BotFather. */
    fun signInBot(botToken: String, apiHash: String): User = call {
        decode(Native.signInBot(handle, botToken, apiHash))
    }

    /** Requests the code needed to sign in to a regular user account. */
    fun requestLoginCode(phone: String): LoginCodeSent = request("requestLoginCode", PhonePayload(phone))

    /** Completes the code step of a user login. */
    fun signIn(code: String): SignInResult {
        val response: SignInResponse = request("signIn", CodePayload(code))
        return when (response.status) {
            "authorized" -> SignInResult.Authorized(requireNotNull(response.user))
            "passwordRequired" -> SignInResult.PasswordRequired(response.hint)
            else -> throw TelegramException("Unexpected sign-in response: ${response.status}")
        }
    }

    /** Completes a two-factor-authentication login after [signIn] returned PasswordRequired. */
    fun checkPassword(password: String): User = request("checkPassword", PasswordPayload(password))

    /** Fetches the account associated with the current session. */
    fun getMe(): User = request("getMe", EmptyPayload)

    /** Resolves a public @username into a peer handle usable by the other operations. */
    fun resolveUsername(username: String): Peer =
        request("resolveUsername", PeerTarget(username = username.removePrefix("@")))

    /** Resolves a public username and sends a plain text message to it. */
    fun sendMessage(username: String, text: String): Message = call {
        decode(Native.sendMessage(handle, username.removePrefix("@"), text))
    }

    fun sendMessage(
        peer: Peer,
        text: String,
        replyToMessageId: Int? = null,
        silent: Boolean = false,
        linkPreview: Boolean = true,
    ): Message = request(
        "sendMessage",
        SendMessagePayload(PeerTarget(peer.nativeHandle), text, replyToMessageId, silent, linkPreview),
    )

    /** Uploads a local file and sends it as a document, or as a Telegram photo when [asPhoto] is true. */
    fun sendFile(
        peer: Peer,
        path: Path,
        caption: String = "",
        asPhoto: Boolean = false,
        replyToMessageId: Int? = null,
        silent: Boolean = false,
    ): Message = request(
        "sendFile",
        SendFilePayload(
            PeerTarget(peer.nativeHandle),
            path.toAbsolutePath().toString(),
            caption,
            asPhoto,
            replyToMessageId,
            silent,
        ),
    )

    /** Uploads one to ten files and sends them as a Telegram media album. */
    fun sendAlbum(peer: Peer, items: List<OutgoingMedia>): List<Message?> {
        require(items.size in 1..10) { "An album must contain between 1 and 10 media items" }
        return request(
            "sendAlbum",
            SendAlbumPayload(
                PeerTarget(peer.nativeHandle),
                items.map { AlbumItemPayload(it.path.toAbsolutePath().toString(), it.caption, it.asPhoto) },
            ),
        )
    }

    fun editMessage(peer: Peer, messageId: Int, text: String, linkPreview: Boolean = true) {
        request<EditMessagePayload, OperationResult>(
            "editMessage",
            EditMessagePayload(PeerTarget(peer.nativeHandle), messageId, text, linkPreview),
        )
    }

    fun deleteMessages(peer: Peer, messageIds: Collection<Int>): Int = request<MessageIdsPayload, DeleteResult>(
        "deleteMessages",
        MessageIdsPayload(PeerTarget(peer.nativeHandle), messageIds.toList()),
    ).deleted

    fun markAsRead(peer: Peer) {
        request<PeerTarget, OperationResult>("markAsRead", PeerTarget(peer.nativeHandle))
    }

    fun getHistory(peer: Peer, limit: Int = 50): List<Message> = request(
        "getHistory",
        HistoryPayload(PeerTarget(peer.nativeHandle), limit),
    )

    /** Loads selected messages; missing or inaccessible IDs are represented as null entries. */
    fun getMessages(peer: Peer, messageIds: Collection<Int>): List<Message?> = request(
        "getMessages",
        MessageIdsPayload(PeerTarget(peer.nativeHandle), messageIds.toList()),
    )

    /** Searches the text content of messages in a peer. */
    fun searchMessages(peer: Peer, query: String, limit: Int = 50): List<Message> = request(
        "searchMessages",
        SearchMessagesPayload(PeerTarget(peer.nativeHandle), query, limit),
    )

    /** Forwards selected messages and preserves the input order; unavailable results are null. */
    fun forwardMessages(destination: Peer, messageIds: Collection<Int>, source: Peer): List<Message?> = request(
        "forwardMessages",
        ForwardMessagesPayload(PeerTarget(destination.nativeHandle), PeerTarget(source.nativeHandle), messageIds.toList()),
    )

    fun getPinnedMessage(peer: Peer): Message? = request("getPinnedMessage", PeerTarget(peer.nativeHandle))

    fun pinMessage(peer: Peer, messageId: Int) {
        request<MessageIdPayload, OperationResult>("pinMessage", MessageIdPayload(PeerTarget(peer.nativeHandle), messageId))
    }

    fun unpinMessage(peer: Peer, messageId: Int) {
        request<MessageIdPayload, OperationResult>("unpinMessage", MessageIdPayload(PeerTarget(peer.nativeHandle), messageId))
    }

    fun unpinAllMessages(peer: Peer) {
        request<PeerTarget, OperationResult>("unpinAllMessages", PeerTarget(peer.nativeHandle))
    }

    /** Adds or replaces the account's emoji reaction on a message. */
    fun sendReaction(peer: Peer, messageId: Int, emoji: String, big: Boolean = false) {
        request<ReactionPayload, OperationResult>(
            "sendReaction",
            ReactionPayload(PeerTarget(peer.nativeHandle), messageId, emoji, remove = false, big),
        )
    }

    /** Removes the account's reaction from a message. */
    fun removeReaction(peer: Peer, messageId: Int) {
        request<ReactionPayload, OperationResult>(
            "sendReaction",
            ReactionPayload(PeerTarget(peer.nativeHandle), messageId, emoji = null, remove = true, big = false),
        )
    }

    fun getParticipants(peer: Peer, limit: Int = 100): List<Participant> = request(
        "getParticipants",
        ParticipantPayload(PeerTarget(peer.nativeHandle), limit),
    )

    fun kickParticipant(chat: Peer, user: Peer) {
        request<KickParticipantPayload, OperationResult>(
            "kickParticipant",
            KickParticipantPayload(PeerTarget(chat.nativeHandle), PeerTarget(user.nativeHandle)),
        )
    }

    fun getDialogs(limit: Int = 50): List<Dialog> = request("getDialogs", LimitPayload(limit))

    fun joinChat(peer: Peer): Peer? {
        val result: JoinResult = request("joinChat", PeerTarget(peer.nativeHandle))
        return result.peer
    }

    fun leaveChat(peer: Peer) {
        request<PeerTarget, OperationResult>("leaveChat", PeerTarget(peer.nativeHandle))
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            Native.close(handle)
        }
    }

    private fun <T> call(block: () -> T): T {
        check(!closed.get()) { "TelegramClient is already closed" }
        return block()
    }

    private inline fun <reified Payload : Any, reified Result> request(
        operation: String,
        payload: Payload,
    ): Result = call {
        decode(Native.request(handle, operation, json.encodeToString(payload)))
    }

    private inline fun <reified T> decode(payload: String): T {
        if (!payload.trimStart().startsWith('{') && !payload.trimStart().startsWith('[')) {
            throw TelegramException(payload)
        }
        return json.decodeFromString(payload)
    }

    @Serializable
    data class User(
        val id: Long,
        val username: String? = null,
        val firstName: String? = null,
        val lastName: String? = null,
    )

    @ConsistentCopyVisibility
    @Serializable
    data class Peer internal constructor(
        internal val nativeHandle: Long,
        val id: Long,
        val kind: String,
        val username: String? = null,
        val name: String? = null,
    )

    @Serializable
    data class Message(
        val id: Int,
        val text: String,
        val outgoing: Boolean,
        val replyToMessageId: Int? = null,
    )

    @Serializable
    data class Dialog(
        val peer: Peer,
        val lastMessage: Message? = null,
    )

    @Serializable
    data class Participant(
        val user: User,
        val role: String,
    )

    data class OutgoingMedia(
        val path: Path,
        val caption: String = "",
        val asPhoto: Boolean = false,
    )

    @Serializable
    data class LoginCodeSent(val phone: String)

    sealed interface SignInResult {
        data class Authorized(val user: User) : SignInResult
        data class PasswordRequired(val hint: String?) : SignInResult
    }

    @Serializable
    private data class SignInResponse(
        val status: String,
        val user: User? = null,
        val hint: String? = null,
    )

    @Serializable
    private data class PeerTarget(
        val peerHandle: Long? = null,
        val username: String? = null,
    )

    @Serializable
    private data class PhonePayload(val phone: String)

    @Serializable
    private data class CodePayload(val code: String)

    @Serializable
    private data class PasswordPayload(val password: String)

    @Serializable
    private data class SendMessagePayload(
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

    @Serializable
    private data class SendFilePayload(
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

    @Serializable
    private data class AlbumItemPayload(
        val path: String,
        val caption: String,
        val asPhoto: Boolean,
    )

    @Serializable
    private data class SendAlbumPayload(
        val peerHandle: Long? = null,
        val username: String? = null,
        val items: List<AlbumItemPayload>,
    ) {
        constructor(peer: PeerTarget, items: List<AlbumItemPayload>) : this(peer.peerHandle, peer.username, items)
    }

    @Serializable
    private data class EditMessagePayload(
        val peerHandle: Long? = null,
        val username: String? = null,
        val messageId: Int,
        val text: String,
        val linkPreview: Boolean = true,
    ) {
        constructor(peer: PeerTarget, messageId: Int, text: String, linkPreview: Boolean) :
            this(peer.peerHandle, peer.username, messageId, text, linkPreview)
    }

    @Serializable
    private data class MessageIdsPayload(
        val peerHandle: Long? = null,
        val username: String? = null,
        val messageIds: List<Int>,
    ) {
        constructor(peer: PeerTarget, messageIds: List<Int>) : this(peer.peerHandle, peer.username, messageIds)
    }

    @Serializable
    private data class HistoryPayload(
        val peerHandle: Long? = null,
        val username: String? = null,
        val limit: Int,
    ) {
        constructor(peer: PeerTarget, limit: Int) : this(peer.peerHandle, peer.username, limit)
    }

    @Serializable
    private data class SearchMessagesPayload(
        val peerHandle: Long? = null,
        val username: String? = null,
        val query: String,
        val limit: Int,
    ) {
        constructor(peer: PeerTarget, query: String, limit: Int) : this(peer.peerHandle, peer.username, query, limit)
    }

    @Serializable
    private data class ForwardMessagesPayload(
        val destination: PeerTarget,
        val source: PeerTarget,
        val messageIds: List<Int>,
    )

    @Serializable
    private data class MessageIdPayload(
        val peerHandle: Long? = null,
        val username: String? = null,
        val messageId: Int,
    ) {
        constructor(peer: PeerTarget, messageId: Int) : this(peer.peerHandle, peer.username, messageId)
    }

    @Serializable
    private data class ReactionPayload(
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

    @Serializable
    private data class ParticipantPayload(
        val peerHandle: Long? = null,
        val username: String? = null,
        val limit: Int,
    ) {
        constructor(peer: PeerTarget, limit: Int) : this(peer.peerHandle, peer.username, limit)
    }

    @Serializable
    private data class KickParticipantPayload(
        val chat: PeerTarget,
        val user: PeerTarget,
    )

    @Serializable
    private data class LimitPayload(val limit: Int)

    @Serializable
    private data class DeleteResult(val deleted: Int)

    @Serializable
    private data class OperationResult(val ok: Boolean)

    @Serializable
    private data class JoinResult(
        val nativeHandle: Long? = null,
        val id: Long? = null,
        val kind: String? = null,
        val username: String? = null,
        val name: String? = null,
        val joined: Boolean? = null,
    ) {
        val peer: Peer?
            get() = if (nativeHandle != null && id != null && kind != null) {
                Peer(nativeHandle, id, kind, username, name)
            } else {
                null
            }
    }

    @Serializable
    private object EmptyPayload

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Creates a client backed by a persistent grammers SQLite session. */
        @JvmStatic
        fun create(apiId: Int, apiHash: String, sessionPath: Path): TelegramClient {
            require(apiId > 0) { "apiId must be positive" }
            require(apiHash.isNotBlank()) { "apiHash must not be blank" }
            NativeLibraryLoader.ensureLoaded()
            val result = Native.create(apiId, apiHash, sessionPath.toAbsolutePath().toString())
            val handle = result.toLongOrNull() ?: throw TelegramException(result)
            return TelegramClient(handle)
        }

        @JvmStatic
        fun create(apiId: Int, apiHash: String, sessionPath: String): TelegramClient =
            create(apiId, apiHash, Paths.get(sessionPath))
    }

    private object Native {
        external fun create(apiId: Int, apiHash: String, sessionPath: String): String
        external fun close(handle: Long): String
        external fun isAuthorized(handle: Long): String
        external fun signInBot(handle: Long, token: String, apiHash: String): String
        external fun sendMessage(handle: Long, username: String, text: String): String
        external fun request(handle: Long, operation: String, payload: String): String
    }
}
