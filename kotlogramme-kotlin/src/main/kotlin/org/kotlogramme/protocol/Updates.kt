package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the update-stream operations. */

/** Payload of `nextUpdate` and `nextRawUpdate`, which wait for the same time. */
@Serializable
internal data class NextUpdatePayload(val timeoutMillis: Long)

/** Result of `nextUpdate`; a timed-out stream returns a null update rather than an error. */
@Serializable
internal data class NextUpdateResult(val update: Update? = null)

/** Result of `nextRawUpdate`, which reports the same null on a timeout. */
@Serializable
internal data class NextRawUpdateResult(val update: RawUpdateEntry? = null)

/**
 * One update exactly as Telegram sent it, with the state it advanced to and the peers it named.
 *
 * The peer map is a hash map on the native side, so [peers] is sorted by id to keep the document
 * stable. This is what a caller needs when the typed [Update] does not describe the event.
 */
@Serializable
internal data class RawUpdateEntry(
    val update: RawUpdate,
    val state: UpdateState,
    val peers: List<Peer>,
)
