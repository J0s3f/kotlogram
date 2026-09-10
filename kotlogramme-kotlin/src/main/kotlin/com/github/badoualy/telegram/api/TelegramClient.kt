package com.github.badoualy.telegram.api

import org.kotlogramme.TelegramClient as GrammersClient
import org.kotlogramme.TelegramException
import java.nio.file.Path

/** Kotlogram-shaped API mapped onto grammers' current high-level client operations. */
interface TelegramClient : AutoCloseable {
    fun isAuthorized(): Boolean
    fun isClosed(): Boolean

    /** Retained for source compatibility; grammers manages request timeouts internally. */
    fun setTimeout(timeout: Long) = Unit

    /** Retained for source compatibility; grammers manages exported DC connections internally. */
    fun setExportedClientTimeout(timeout: Long) = Unit

    /** Retained for source compatibility with Kotlogram's shutdown flag. */
    fun close(shutdown: Boolean) = close()

    /** grammers' file operations use the same client/session, so no secondary client is needed. */
    fun getDownloaderClient(): TelegramClient = this

    fun authSendCode(
        allowFlashcall: Boolean = false,
        phoneNumber: String,
        currentNumber: Boolean = false,
    ): SentCode

    fun authSignIn(phoneNumber: String, phoneCodeHash: String, phoneCode: String): Authorization
    fun authCheckPassword(password: String): Authorization
    fun authImportBotAuthorization(botAuthToken: String): Authorization

    fun getMe(): User
    fun contactsResolveUsername(username: String): TelegramPeer

    fun messagesSendMessage(
        peer: TelegramPeer,
        message: String,
        randomId: Long = 0L,
        replyToMsgId: Int? = null,
        silent: Boolean = false,
        noWebpage: Boolean = false,
    ): Message

    /** Uploads and sends a local file; set [asPhoto] to let Telegram compress it as a photo. */
    fun messagesSendFile(
        peer: TelegramPeer,
        path: Path,
        caption: String = "",
        asPhoto: Boolean = false,
        replyToMsgId: Int? = null,
        silent: Boolean = false,
    ): Message

    fun messagesSendAlbum(peer: TelegramPeer, items: List<OutgoingMedia>): List<Message?>

    fun messagesEditMessage(peer: TelegramPeer, id: Int, message: String, noWebpage: Boolean = false)
    fun messagesDeleteMessages(peer: TelegramPeer, ids: Collection<Int>): Int
    fun messagesGetHistory(peer: TelegramPeer, limit: Int = 50): List<Message>
    fun messagesGetMessages(peer: TelegramPeer, ids: Collection<Int>): List<Message?>
    fun messagesSearch(peer: TelegramPeer, query: String, limit: Int = 50): List<Message>
    fun messagesForwardMessages(toPeer: TelegramPeer, ids: Collection<Int>, fromPeer: TelegramPeer): List<Message?>
    fun messagesGetPinnedMessage(peer: TelegramPeer): Message?
    fun messagesPinMessage(peer: TelegramPeer, id: Int)
    fun messagesUnpinMessage(peer: TelegramPeer, id: Int)
    fun messagesUnpinAllMessages(peer: TelegramPeer)
    fun messagesSendReaction(peer: TelegramPeer, id: Int, emoji: String, big: Boolean = false)
    fun messagesRemoveReaction(peer: TelegramPeer, id: Int)
    fun messagesGetDialogs(limit: Int = 50): List<Dialog>
    fun messagesReadHistory(peer: TelegramPeer)
    fun channelsJoinChannel(peer: TelegramPeer): TelegramPeer?
    fun channelsLeaveChannel(peer: TelegramPeer)
    fun channelsGetParticipants(peer: TelegramPeer, limit: Int = 100): List<Participant>
    fun channelsKickParticipant(peer: TelegramPeer, user: TelegramPeer)
}

data class SentCode(val phoneNumber: String, val phoneCodeHash: String)

data class Authorization(val user: User)

data class User(
    val id: Long,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
)

@ConsistentCopyVisibility
data class TelegramPeer internal constructor(
    val id: Long,
    val kind: String,
    val username: String?,
    val name: String?,
    internal val native: GrammersClient.Peer,
)

data class Message(
    val id: Int,
    val text: String,
    val outgoing: Boolean,
    val replyToMessageId: Int?,
)

data class OutgoingMedia(
    val path: Path,
    val caption: String = "",
    val asPhoto: Boolean = false,
)

data class Dialog(val peer: TelegramPeer, val lastMessage: Message?)

data class Participant(val user: User, val role: String)

class PasswordRequiredException(val hint: String?) : TelegramException("Two-factor authentication password required")

internal class DefaultTelegramClient(
    private val client: GrammersClient,
    private val application: TelegramApp,
    @Suppress("unused") private val updateCallback: UpdateCallback?,
) : TelegramClient {
    override fun isAuthorized(): Boolean = client.isAuthorized()

    override fun isClosed(): Boolean = closed

    override fun authSendCode(
        allowFlashcall: Boolean,
        phoneNumber: String,
        currentNumber: Boolean,
    ): SentCode {
        client.requestLoginCode(phoneNumber)
        // The native bridge retains the actual Telegram token, which is deliberately not exposed.
        return SentCode(phoneNumber, "managed-by-kotlogramme")
    }

    override fun authSignIn(phoneNumber: String, phoneCodeHash: String, phoneCode: String): Authorization =
        when (val result = client.signIn(phoneCode)) {
            is GrammersClient.SignInResult.Authorized -> Authorization(result.user.toCompatibility())
            is GrammersClient.SignInResult.PasswordRequired -> throw PasswordRequiredException(result.hint)
        }

    override fun authCheckPassword(password: String): Authorization = Authorization(client.checkPassword(password).toCompatibility())

    override fun authImportBotAuthorization(botAuthToken: String): Authorization =
        Authorization(client.signInBot(botAuthToken, application.apiHash).toCompatibility())

    override fun getMe(): User = client.getMe().toCompatibility()

    override fun contactsResolveUsername(username: String): TelegramPeer = client.resolveUsername(username).toCompatibility()

    override fun messagesSendMessage(
        peer: TelegramPeer,
        message: String,
        randomId: Long,
        replyToMsgId: Int?,
        silent: Boolean,
        noWebpage: Boolean,
    ): Message = client.sendMessage(
        peer.native,
        message,
        replyToMessageId = replyToMsgId,
        silent = silent,
        linkPreview = !noWebpage,
    ).toCompatibility()

    override fun messagesSendFile(
        peer: TelegramPeer,
        path: Path,
        caption: String,
        asPhoto: Boolean,
        replyToMsgId: Int?,
        silent: Boolean,
    ): Message = client.sendFile(peer.native, path, caption, asPhoto, replyToMsgId, silent).toCompatibility()

    override fun messagesSendAlbum(peer: TelegramPeer, items: List<OutgoingMedia>): List<Message?> =
        client.sendAlbum(peer.native, items.map { GrammersClient.OutgoingMedia(it.path, it.caption, it.asPhoto) })
            .map { it?.toCompatibility() }

    override fun messagesEditMessage(peer: TelegramPeer, id: Int, message: String, noWebpage: Boolean) {
        client.editMessage(peer.native, id, message, linkPreview = !noWebpage)
    }

    override fun messagesDeleteMessages(peer: TelegramPeer, ids: Collection<Int>): Int =
        client.deleteMessages(peer.native, ids)

    override fun messagesGetHistory(peer: TelegramPeer, limit: Int): List<Message> =
        client.getHistory(peer.native, limit).map { it.toCompatibility() }

    override fun messagesGetMessages(peer: TelegramPeer, ids: Collection<Int>): List<Message?> =
        client.getMessages(peer.native, ids).map { it?.toCompatibility() }

    override fun messagesSearch(peer: TelegramPeer, query: String, limit: Int): List<Message> =
        client.searchMessages(peer.native, query, limit).map { it.toCompatibility() }

    override fun messagesForwardMessages(
        toPeer: TelegramPeer,
        ids: Collection<Int>,
        fromPeer: TelegramPeer,
    ): List<Message?> = client.forwardMessages(toPeer.native, ids, fromPeer.native).map { it?.toCompatibility() }

    override fun messagesGetPinnedMessage(peer: TelegramPeer): Message? =
        client.getPinnedMessage(peer.native)?.toCompatibility()

    override fun messagesPinMessage(peer: TelegramPeer, id: Int) {
        client.pinMessage(peer.native, id)
    }

    override fun messagesUnpinMessage(peer: TelegramPeer, id: Int) {
        client.unpinMessage(peer.native, id)
    }

    override fun messagesUnpinAllMessages(peer: TelegramPeer) {
        client.unpinAllMessages(peer.native)
    }

    override fun messagesSendReaction(peer: TelegramPeer, id: Int, emoji: String, big: Boolean) {
        client.sendReaction(peer.native, id, emoji, big)
    }

    override fun messagesRemoveReaction(peer: TelegramPeer, id: Int) {
        client.removeReaction(peer.native, id)
    }

    override fun messagesGetDialogs(limit: Int): List<Dialog> = client.getDialogs(limit).map {
        Dialog(it.peer.toCompatibility(), it.lastMessage?.toCompatibility())
    }

    override fun messagesReadHistory(peer: TelegramPeer) {
        client.markAsRead(peer.native)
    }

    override fun channelsJoinChannel(peer: TelegramPeer): TelegramPeer? = client.joinChat(peer.native)?.toCompatibility()

    override fun channelsLeaveChannel(peer: TelegramPeer) {
        client.leaveChat(peer.native)
    }

    override fun channelsGetParticipants(peer: TelegramPeer, limit: Int): List<Participant> =
        client.getParticipants(peer.native, limit).map { Participant(it.user.toCompatibility(), it.role) }

    override fun channelsKickParticipant(peer: TelegramPeer, user: TelegramPeer) {
        client.kickParticipant(peer.native, user.native)
    }

    override fun close() {
        if (!closed) {
            closed = true
            client.close()
        }
    }

    private var closed = false
}

private fun GrammersClient.User.toCompatibility() = User(id, username, firstName, lastName)

private fun GrammersClient.Peer.toCompatibility() = TelegramPeer(id, kind, username, name, this)

private fun GrammersClient.Message.toCompatibility() = Message(id, text, outgoing, replyToMessageId)
