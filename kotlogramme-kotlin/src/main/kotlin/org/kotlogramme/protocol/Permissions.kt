package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Chat rights, as reported by grammers. */

/**
 * The admin rights of a chat member, mirroring grammers' `Permissions` (TL `chatAdminRights`).
 *
 * Only the ten rights grammers exposes as accessors are carried. The layer's `other`,
 * `manage_topics` and story rights have no accessor, so they are absent rather than guessed.
 */
@Serializable
data class ChatPermissions(
    val changeInfo: Boolean = false,
    val postMessages: Boolean = false,
    val editMessages: Boolean = false,
    val deleteMessages: Boolean = false,
    val banUsers: Boolean = false,
    val inviteUsers: Boolean = false,
    val pinMessages: Boolean = false,
    val addAdmins: Boolean = false,
    val anonymous: Boolean = false,
    val manageCall: Boolean = false,
)

/**
 * The restrictions applied to a banned or restricted member, mirroring grammers' `Restrictions`
 * (TL `chatBannedRights`).
 *
 * A right is `true` when it is *allowed*; the layer spells the same flags the other way round.
 * [untilDate] is epoch milliseconds, and the epoch itself means the ban never expires.
 */
@Serializable
data class ChatRestrictions(
    val viewMessages: Boolean = false,
    val sendMessages: Boolean = false,
    val sendMedia: Boolean = false,
    val sendStickers: Boolean = false,
    val sendGifs: Boolean = false,
    val sendGames: Boolean = false,
    val sendInline: Boolean = false,
    val embedLinks: Boolean = false,
    val sendPolls: Boolean = false,
    val changeInfo: Boolean = false,
    val inviteUsers: Boolean = false,
    val pinMessages: Boolean = false,
    val untilDate: Long = 0,
)
