package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/**
 * A resolved chat, user or channel.
 *
 * [nativeHandle] is the opaque token the native registry uses to resolve this peer again later, so
 * it can only be read inside this module.
 *
 * [kind] is `user`, `group` or `channel`, which is how grammers splits a peer. [isMegagroup] is
 * `null` for anything that is not a group chat, and [permissions] is only reported for a
 * broadcast channel, because that is the only peer grammers exposes admin rights for.
 */
@ConsistentCopyVisibility
@Serializable
data class Peer internal constructor(
    internal val nativeHandle: Long,
    val id: Long,
    val kind: String,
    val username: String? = null,
    val name: String? = null,
    /** The collectible usernames, which grammers reports separately from [username]. */
    val usernames: List<String> = emptyList(),
    val isMegagroup: Boolean? = null,
    val hasPhoto: Boolean = false,
    val permissions: ChatPermissions? = null,
)
