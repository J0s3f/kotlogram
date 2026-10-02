package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.ChatPermissions
import org.kotlogramme.protocol.ChatRestrictions
import org.kotlogramme.protocol.InviteLinkPayload
import org.kotlogramme.protocol.InviteLinkResult
import org.kotlogramme.protocol.JoinResult
import org.kotlogramme.protocol.KickParticipantPayload
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.ParticipantPairPayload
import org.kotlogramme.protocol.ParticipantPayload
import org.kotlogramme.protocol.ParticipantPermissions
import org.kotlogramme.protocol.ParticipantsResult
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.ResolvePeerPayload
import org.kotlogramme.protocol.SetAdminRightsPayload
import org.kotlogramme.protocol.SetBannedRightsPayload

/** Joining, leaving, inspecting and moderating the membership of a chat. */
internal interface ChatsBridge {
    val transport: Transport

    /**
     * Lists a chat's participants together with the chat's total member count.
     *
     * [filter] is one of the names the native side knows (`recent`, `admins`, `bots`, `banned`,
     * `contacts`, `search`, `kicked`, `mentions`); the query-taking filters read [query]. [offset]
     * is the paging cursor: the index of the first member to return, absent is the first page.
     */
    @Operation("getParticipants")
    fun getParticipants(
        peer: Peer,
        limit: Int = 100,
        filter: String? = null,
        query: String? = null,
        offset: Int? = null,
    ): ParticipantsResult = transport.request(
        "getParticipants",
        ParticipantPayload(PeerTarget(peer.nativeHandle), limit, filter, query, offset),
    )

    @Operation("kickParticipant")
    fun kickParticipant(chat: Peer, user: Peer) {
        transport.request<KickParticipantPayload, OperationResult>(
            "kickParticipant",
            KickParticipantPayload(PeerTarget(chat.nativeHandle), PeerTarget(user.nativeHandle)),
        )
    }

    @Operation("inviteToChannel")
    fun inviteToChannel(chat: Peer, user: Peer) {
        transport.request<KickParticipantPayload, OperationResult>(
            "inviteToChannel",
            KickParticipantPayload(PeerTarget(chat.nativeHandle), PeerTarget(user.nativeHandle)),
        )
    }

    @Operation("joinChat")
    fun joinChat(peer: Peer): Peer? = transport.request<PeerTarget, JoinResult>(
        "joinChat",
        PeerTarget(peer.nativeHandle),
    ).peer

    @Operation("leaveChat")
    fun leaveChat(peer: Peer) {
        transport.request<PeerTarget, OperationResult>("leaveChat", PeerTarget(peer.nativeHandle))
    }

    /** Reports one member's role in a chat, which is grammers' `get_permissions`. */
    @Operation("getPermissions")
    fun getPermissions(chat: Peer, user: Peer): ParticipantPermissions = transport.request(
        "getPermissions",
        ParticipantPairPayload(PeerTarget(chat.nativeHandle), PeerTarget(user.nativeHandle)),
    )

    /**
     * Bans or restricts a member, which is grammers' `set_banned_rights`.
     *
     * [rights] keeps the layer's "banned" polarity, so a `true` flag denies the right.
     */
    @Operation("setBannedRights")
    fun setBannedRights(
        chat: Peer,
        user: Peer,
        rights: ChatRestrictions,
        loadCurrent: Boolean = false,
    ) {
        transport.request<SetBannedRightsPayload, OperationResult>(
            "setBannedRights",
            SetBannedRightsPayload(
                PeerTarget(chat.nativeHandle),
                PeerTarget(user.nativeHandle),
                rights,
                loadCurrent,
            ),
        )
    }

    /** Grants or revokes admin rights, which is grammers' `set_admin_rights`. */
    @Operation("setAdminRights")
    fun setAdminRights(
        chat: Peer,
        user: Peer,
        rights: ChatPermissions,
        rank: String? = null,
        loadCurrent: Boolean = false,
    ) {
        transport.request<SetAdminRightsPayload, OperationResult>(
            "setAdminRights",
            SetAdminRightsPayload(
                PeerTarget(chat.nativeHandle),
                PeerTarget(user.nativeHandle),
                rights,
                rank,
                loadCurrent,
            ),
        )
    }

    /** Joins a private chat from its invite link. */
    @Operation("acceptInviteLink")
    fun acceptInviteLink(inviteLink: String): Peer? =
        transport.request<InviteLinkPayload, JoinResult>(
            "acceptInviteLink",
            InviteLinkPayload(inviteLink),
        ).peer

    /** Extracts the hash of a private invite link, or null when the link carries none. */
    @Operation("parseInviteLink")
    fun parseInviteLink(inviteLink: String): String? =
        transport.request<InviteLinkPayload, InviteLinkResult>(
            "parseInviteLink",
            InviteLinkPayload(inviteLink),
        ).hash

    /** Resolves a Bot API dialog id back into a peer. */
    @Operation("resolvePeer")
    fun resolvePeer(id: Long, accessHash: Long? = null): Peer? =
        transport.request<ResolvePeerPayload, JoinResult>(
            "resolvePeer",
            ResolvePeerPayload(id, accessHash),
        ).peer
}

/** The [ChatsBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class ChatsOperations(override val transport: Transport) : ChatsBridge
