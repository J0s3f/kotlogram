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

/** Payload of `answerGuestChatQuery`: the query to answer and the article result to send. */
@Serializable
internal data class AnswerGuestChatQueryPayload(
    val queryId: Long,
    val result: InlineArticleSpec,
)

/**
 * Result of `answerGuestChatQuery`: the identifier of the inline message the answer produced.
 *
 * The layer has two constructors: a 32-bit one and a 64-bit one that also carries the owner.
 * [ownerId] is only set when the 64-bit constructor is the one returned.
 */
@Serializable
internal data class GuestChatAnswerResult(
    val dcId: Int,
    val id: Long,
    val accessHash: Long,
    val ownerId: Long? = null,
)

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
