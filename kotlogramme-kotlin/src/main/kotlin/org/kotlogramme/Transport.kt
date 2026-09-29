package org.kotlogramme

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The transport every domain bridge shares: the native handle, the JSON codec and the closed flag.
 *
 * A domain bridge holds this and nothing else, which is what lets its interface carry default
 * implementations: [TelegramClient] builds one transport and delegates every domain to it.
 *
 * This is an implementation detail of the bridge rather than part of the Kotlogram compatibility
 * surface, which lives in `com.github.badoualy.telegram.api`; callers should not depend on it.
 */
class Transport(val handle: Long) {
    /** Shared with the inline [request] and [decode] below. */
    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true }

    private val closed = AtomicBoolean(false)

    /** Whether [close] has already run. */
    val isClosed: Boolean get() = closed.get()

    /** Runs [block] unless the client has already been closed. */
    fun <T> call(block: () -> T): T {
        check(!closed.get()) { "TelegramClient is already closed" }
        return block()
    }

    /** Sends one operation to the native dispatcher and decodes its JSON result. */
    inline fun <reified Payload : Any, reified Result> request(operation: String, payload: Payload): Result =
        call { decode(TelegramClient.Native.request(handle, operation, json.encodeToString(payload))) }

    /**
     * Decodes a JSON result, or raises the native error message as a [TelegramException].
     *
     * The native side answers an error as a plain `kotlogramme error: ...` string instead of a
     * document, so anything that is not a JSON object, array or the literal `null` is reported as
     * one. `null` is a document: an operation that answers "nothing" decodes to `null` when the
     * caller asks for a nullable type, which is why `getPinnedMessage` and `getReplyMarkup` do.
     */
    inline fun <reified T> decode(payload: String): T {
        val text = payload.trim()
        if (text != "null" && !text.startsWith('{') && !text.startsWith('[')) {
            throw TelegramException(payload)
        }
        return json.decodeFromString(payload)
    }

    /**
     * Marks the client closed and reports whether this call was the one that did it.
     *
     * The caller closes the native handle only on the first call, so a double close is a no-op.
     */
    fun close(): Boolean = closed.compareAndSet(false, true)
}
