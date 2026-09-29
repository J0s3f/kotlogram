package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.EmptyPayload
import org.kotlogramme.protocol.NextRawUpdateResult
import org.kotlogramme.protocol.NextUpdatePayload
import org.kotlogramme.protocol.NextUpdateResult
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.RawUpdateEntry
import org.kotlogramme.protocol.Update

/** Consuming grammers' ordered update stream. */
internal interface UpdatesBridge {
    val transport: Transport

    /** Waits for the next ordered grammers update, or returns null after [timeoutMillis]. */
    @Operation("nextUpdate")
    fun nextUpdate(timeoutMillis: Long = 30_000): Update? {
        val timeout = requireTimeout(timeoutMillis)
        return transport.request<NextUpdatePayload, NextUpdateResult>(
            "nextUpdate", NextUpdatePayload(timeout),
        ).update
    }

    /**
     * Waits for the next update exactly as Telegram sent it, or returns null after [timeoutMillis].
     *
     * This is the same stream [nextUpdate] reads: the typed update is a projection of the raw one,
     * so consuming both in turn drains the stream rather than racing each other.
     */
    @Operation("nextRawUpdate")
    fun nextRawUpdate(timeoutMillis: Long = 30_000): RawUpdateEntry? {
        val timeout = requireTimeout(timeoutMillis)
        return transport.request<NextUpdatePayload, NextRawUpdateResult>(
            "nextRawUpdate", NextUpdatePayload(timeout),
        ).update
    }

    /**
     * Asks grammers to write the update state again.
     *
     * grammers writes it whenever the stream is dropped, but it only notices at that point, so this
     * lets a client persist the state before it closes — or at any other point.
     */
    @Operation("syncUpdateState")
    fun syncUpdateState() {
        transport.request<EmptyPayload, OperationResult>("syncUpdateState", EmptyPayload)
    }
}

/** The wait both streaming operations apply; grammers polls the stream in a loop until it expires. */
private fun requireTimeout(timeoutMillis: Long): Long {
    require(timeoutMillis in 1..60_000) { "timeoutMillis must be between 1 and 60000" }
    return timeoutMillis
}

/** The [UpdatesBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class UpdatesOperations(override val transport: Transport) : UpdatesBridge
