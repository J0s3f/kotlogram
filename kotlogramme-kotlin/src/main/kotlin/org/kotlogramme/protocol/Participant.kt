package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/**
 * A chat member and the role grammers reports for it.
 *
 * A role is one of five variants and they share no accessors, so every role-specific field is
 * `null` for the roles that cannot answer it. A creator has no date, only a creator and an admin
 * carry rights, and only a banned member carries restrictions. [role] is `member`, `creator`,
 * `admin`, `banned`, `left`, or `unknown` for a variant this build does not model yet.
 */
@Serializable
data class Participant(
    val user: User,
    val role: String,
    /** The custom admin title Telegram shows instead of the role name. */
    val rank: String? = null,
    /** Epoch milliseconds, from the role's join, promotion or ban date. */
    val date: Long? = null,
    /** The identifier of the user who added this member, which is all grammers reports. */
    val invitedBy: Long? = null,
    val promotedBy: Long? = null,
    val kickedBy: Long? = null,
    val canEdit: Boolean? = null,
    /** True when a banned participant has already left the chat on their own. */
    val left: Boolean? = null,
    val permissions: ChatPermissions? = null,
    val restrictions: ChatRestrictions? = null,
)

/**
 * The role membership of one participant, as grammers' `ParticipantPermissions` answers it.
 *
 * This is a different question from the rights a role carries: it says what the member *is* in the
 * chat, not what they are allowed to do. Every flag is a plain boolean because the layer answers
 * all of them for every participant variant.
 */
@Serializable
data class ParticipantPermissions(
    /** True for the chat or channel creator. */
    val isCreator: Boolean = false,
    /** True for a creator too, because a creator has every right an admin has. */
    val isAdmin: Boolean = false,
    /** True for a banned channel participant. */
    val isBanned: Boolean = false,
    /** True for a member that left the channel on their own. */
    val hasLeft: Boolean = false,
    /** True for a normal member with no restrictions and no admin rights. */
    val hasDefaultPermissions: Boolean = false,
    /** True for a creator and for an admin whose rights let them add admins. */
    val canAddAdmins: Boolean = false,
)
