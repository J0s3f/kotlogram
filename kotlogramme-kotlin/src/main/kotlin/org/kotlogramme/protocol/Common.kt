package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Request and result payloads shared by more than one operation. */

/** Peer selector: either a handle from a previous result, or a public username. */
@Serializable
internal data class PeerTarget(
    val peerHandle: Long? = null,
    val username: String? = null,
)

/** Result of an operation that only reports success. */
@Serializable
internal data class OperationResult(val ok: Boolean)

/**
 * Result of `joinChat`.
 *
 * A successful join returns a peer; an already-joined chat returns `{"joined": false}` instead.
 */
@Serializable
internal data class JoinResult(
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
