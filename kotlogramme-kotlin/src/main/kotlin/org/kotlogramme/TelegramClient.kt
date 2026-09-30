package org.kotlogramme

import org.kotlogramme.bridge.AccountBridge
import org.kotlogramme.bridge.AccountOperations
import org.kotlogramme.bridge.ActionsBridge
import org.kotlogramme.bridge.ActionsOperations
import org.kotlogramme.bridge.AuthBridge
import org.kotlogramme.bridge.AuthOperations
import org.kotlogramme.bridge.ChatsBridge
import org.kotlogramme.bridge.ChatsOperations
import org.kotlogramme.bridge.ContactsBridge
import org.kotlogramme.bridge.ContactsOperations
import org.kotlogramme.bridge.DialogsBridge
import org.kotlogramme.bridge.DialogsOperations
import org.kotlogramme.bridge.FilesBridge
import org.kotlogramme.bridge.FilesOperations
import org.kotlogramme.bridge.FoldersBridge
import org.kotlogramme.bridge.FoldersOperations
import org.kotlogramme.bridge.InlineBridge
import org.kotlogramme.bridge.InlineOperations
import org.kotlogramme.bridge.MarkupBridge
import org.kotlogramme.bridge.MarkupOperations
import org.kotlogramme.bridge.MediaBridge
import org.kotlogramme.bridge.MediaOperations
import org.kotlogramme.bridge.MessagesBridge
import org.kotlogramme.bridge.MessagesOperations
import org.kotlogramme.bridge.RawBridge
import org.kotlogramme.bridge.RawOperations
import org.kotlogramme.bridge.StickersBridge
import org.kotlogramme.bridge.StickersOperations
import org.kotlogramme.bridge.UpdatesBridge
import org.kotlogramme.bridge.UpdatesOperations
import org.kotlogramme.protocol.Message
import org.kotlogramme.protocol.User
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Synchronous Kotlin/JVM facade over the grammers Telegram client.
 *
 * The native side owns the Tokio runtime and the grammers client. Calls are safe to make from
 * different JVM threads; a single client should still be closed exactly once.
 *
 * This class is the transport layer. Each domain of operations lives in its own file under
 * `org.kotlogramme.bridge` and is delegated to here, so a feature area is added as one bridge
 * interface plus its line in the supertype list below. Wire models live in
 * `org.kotlogramme.protocol`, request payloads are grouped by domain, and the Kotlogram-shaped
 * facade in `com.github.badoualy.telegram.api` is built on top of it.
 */
class TelegramClient private constructor(
    override val transport: Transport,
) : AutoCloseable,
    AccountBridge by AccountOperations(transport),
    AuthBridge by AuthOperations(transport),
    MessagesBridge by MessagesOperations(transport),
    ChatsBridge by ChatsOperations(transport),
    ContactsBridge by ContactsOperations(transport),
    DialogsBridge by DialogsOperations(transport),
    UpdatesBridge by UpdatesOperations(transport),
    MediaBridge by MediaOperations(transport),
    FilesBridge by FilesOperations(transport),
    FoldersBridge by FoldersOperations(transport),
    InlineBridge by InlineOperations(transport),
    ActionsBridge by ActionsOperations(transport),
    MarkupBridge by MarkupOperations(transport),
    StickersBridge by StickersOperations(transport),
    RawBridge by RawOperations(transport) {

    /** Returns whether this session is already authorized with Telegram. */
    @Operation("isAuthorized")
    fun isAuthorized(): Boolean = transport.call {
        val result = Native.isAuthorized(transport.handle)
        result.toBooleanStrictOrNull() ?: throw TelegramException(result)
    }

    /** Signs in a bot using a token created by BotFather. */
    @Operation("signInBot")
    fun signInBot(botToken: String, apiHash: String): User = transport.call {
        transport.decode(Native.signInBot(transport.handle, botToken, apiHash))
    }

    /** Resolves a public username and sends a plain text message to it. */
    @Operation("sendMessage")
    fun sendMessage(username: String, text: String): Message = transport.call {
        transport.decode(Native.sendMessage(transport.handle, username.removePrefix("@"), text))
    }

    /**
     * Sends one Layer-216 TL-encoded request body and returns the raw TL response body.
     *
     * This is experimental. Callers are responsible for generating a request compatible with
     * [com.github.badoualy.telegram.api.Kotlogram.API_LAYER] and decoding the corresponding
     * response type. Pass null to use the session's home data center.
     */
    @Operation("invokeRaw")
    fun invokeRaw(body: ByteArray, dataCenterId: Int? = null): ByteArray = transport.call {
        require(body.isNotEmpty()) { "A raw TL request body must not be empty" }
        Native.invokeRaw(transport.handle, body, dataCenterId ?: 0)
    }

    override fun close() {
        if (transport.close()) {
            Native.close(transport.handle)
        }
    }

    companion object {
        /** Creates a client backed by a persistent grammers SQLite session. */
        @JvmStatic
        fun create(apiId: Int, apiHash: String, sessionPath: Path): TelegramClient {
            require(apiId > 0) { "apiId must be positive" }
            require(apiHash.isNotBlank()) { "apiHash must not be blank" }
            NativeLibraryLoader.ensureLoaded()
            val result = Native.create(apiId, apiHash, sessionPath.toAbsolutePath().toString())
            val handle = result.toLongOrNull() ?: throw TelegramException(result)
            return TelegramClient(Transport(handle))
        }

        @JvmStatic
        fun create(apiId: Int, apiHash: String, sessionPath: String): TelegramClient =
            create(apiId, apiHash, Paths.get(sessionPath))
    }

    /**
     * The seven JNI exports the Rust cdylib provides.
     *
     * The names are part of the native ABI: the Rust side exports
     * `Java_org_kotlogramme_TelegramClient_00024Native_<name>` for each of them, so neither this
     * object nor the enclosing class may be renamed or moved out of it.
     *
     * It is published to the module because [Transport]'s inline request and decode reach it.
     */
    @PublishedApi
    internal object Native {
        external fun create(apiId: Int, apiHash: String, sessionPath: String): String
        external fun close(handle: Long): String
        external fun isAuthorized(handle: Long): String
        external fun signInBot(handle: Long, token: String, apiHash: String): String
        external fun sendMessage(handle: Long, username: String, text: String): String
        external fun request(handle: Long, operation: String, payload: String): String
        external fun invokeRaw(handle: Long, body: ByteArray, dataCenterId: Int): ByteArray
    }
}
