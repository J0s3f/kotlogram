package com.github.badoualy.telegram.api

import org.kotlogramme.TelegramClient as GrammersClient
import org.kotlogramme.TelegramException

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

    fun messagesEditMessage(peer: TelegramPeer, id: Int, message: String, noWebpage: Boolean = false)
    fun messagesDeleteMessages(peer: TelegramPeer, ids: Collection<Int>): Int
    fun messagesGetHistory(peer: TelegramPeer, limit: Int = 50): List<Message>
    fun messagesGetDialogs(limit: Int = 50): List<Dialog>
    fun messagesReadHistory(peer: TelegramPeer)
    fun channelsJoinChannel(peer: TelegramPeer): TelegramPeer?
    fun channelsLeaveChannel(peer: TelegramPeer)
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

data class Dialog(val peer: TelegramPeer, val lastMessage: Message?)

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

    override fun messagesEditMessage(peer: TelegramPeer, id: Int, message: String, noWebpage: Boolean) {
        client.editMessage(peer.native, id, message, linkPreview = !noWebpage)
    }

    override fun messagesDeleteMessages(peer: TelegramPeer, ids: Collection<Int>): Int =
        client.deleteMessages(peer.native, ids)

    override fun messagesGetHistory(peer: TelegramPeer, limit: Int): List<Message> =
        client.getHistory(peer.native, limit).map { it.toCompatibility() }

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
