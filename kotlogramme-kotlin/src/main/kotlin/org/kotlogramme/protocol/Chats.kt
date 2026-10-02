package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the chat membership and moderation operations. */

/**
 * Payload of `getParticipants`.
 *
 * [offset] is the paging cursor: the index of the first member to return. An index, not an id, so
 * a caller can page without any new field on the participant projection. Absent is the first page.
 */
@Serializable
internal data class ParticipantPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val limit: Int = 100,
    /** One of the `ParticipantFilter` names; absent is the layer's `recent`. */
    val filter: String? = null,
    /** The query the `search`, `banned`, `kicked` and `contacts` filters take. */
    val query: String? = null,
    val offset: Int? = null,
) {
    constructor(
        peer: PeerTarget,
        limit: Int = 100,
        filter: String? = null,
        query: String? = null,
        offset: Int? = null,
    ) : this(peer.peerHandle, peer.username, limit, filter, query, offset)
}

/** Result of `getParticipants`: the listing plus the chat's total member count. */
@Serializable
internal data class ParticipantsResult(
    val participants: List<Participant>,
    val total: Int,
)

/** Payload of `getPermissions`, `setBannedRights` and `setAdminRights`. */
@Serializable
internal data class ParticipantPairPayload(
    val chat: PeerTarget,
    val user: PeerTarget,
)

/**
 * Payload of `setBannedRights`.
 *
 * [`rights`] keeps the layer's "banned" polarity, so `true` denies the right.
 */
@Serializable
internal data class SetBannedRightsPayload(
    val chat: PeerTarget,
    val user: PeerTarget,
    val rights: ChatRestrictions,
    /** Load the rights already in place before applying the requested flags. */
    val loadCurrent: Boolean = false,
)

/** Payload of `setAdminRights`; `rank` is the custom admin badge. */
@Serializable
internal data class SetAdminRightsPayload(
    val chat: PeerTarget,
    val user: PeerTarget,
    val rights: ChatPermissions,
    val rank: String? = null,
    val loadCurrent: Boolean = false,
)

/** Payload of `kickParticipant`. */
@Serializable
internal data class KickParticipantPayload(
    val chat: PeerTarget,
    val user: PeerTarget,
)

/** Payload of `acceptInviteLink` and `parseInviteLink`. */
@Serializable
internal data class InviteLinkPayload(val inviteLink: String)

/** Result of `parseInviteLink`; a null [hash] means the link was not a private invite link. */
@Serializable
internal data class InviteLinkResult(val hash: String? = null)

/** Payload of `resolvePeer`. */
@Serializable
internal data class ResolvePeerPayload(
    /** A Bot API dialog id: positive for a user, negative for a group, `-100…` for a channel. */
    val id: Long,
    /** The peer's access hash, when the caller has one. */
    val accessHash: Long? = null,
)
