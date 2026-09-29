package com.github.badoualy.telegram.api

/** Joining, leaving, inspecting and moderating chats. */
interface ChatsApi : BridgeApi {
    fun channelsJoinChannel(peer: TelegramPeer): TelegramPeer? = bridge.joinChat(peer.native)?.toCompatibility()

    fun channelsLeaveChannel(peer: TelegramPeer) {
        bridge.leaveChat(peer.native)
    }

    fun channelsGetParticipants(peer: TelegramPeer, limit: Int = 100): List<Participant> =
        bridge.getParticipants(peer.native, limit).toCompatibility().participants

    /**
     * Lists a filtered page of a chat's participants together with the chat's total member count.
     *
     * The filter is one of grammers' `ChannelParticipantsFilter` shapes; [query] is read by the
     * `Search`, `Banned`, `Kicked` and `Contacts` filters. grammers only applies a filter to a
     * channel or megagroup, so a small group always reports its whole membership.
     */
    fun channelsGetParticipantPage(
        peer: TelegramPeer,
        filter: ParticipantFilter = ParticipantFilter.Recent,
        query: String? = null,
        limit: Int = 100,
    ): ParticipantPage =
        bridge.getParticipants(peer.native, limit, filter.wire, query).toCompatibility()

    fun channelsKickParticipant(peer: TelegramPeer, user: TelegramPeer) {
        bridge.kickParticipant(peer.native, user.native)
    }

    /** Reports one member's role in a chat, which is grammers' `get_permissions`. */
    fun channelsGetParticipantPermissions(
        peer: TelegramPeer,
        user: TelegramPeer,
    ): ParticipantPermissions =
        bridge.getPermissions(peer.native, user.native).toCompatibility()

    /**
     * Bans or restricts a member, which is Kotlogram's `channelsEditBanned`.
     *
     * [restrictions] keeps the layer's "banned" polarity: a `true` flag denies the right. With
     * [loadCurrent] the rights already in place are loaded first and the requested flags applied
     * on top, so a caller can take away one right without listing the others.
     */
    fun channelsEditBanned(
        peer: TelegramPeer,
        user: TelegramPeer,
        restrictions: ChatRestrictions,
        loadCurrent: Boolean = false,
    ) {
        bridge.setBannedRights(peer.native, user.native, restrictions.toBridge(), loadCurrent)
    }

    /**
     * Grants or revokes admin rights, which is Kotlogram's `channelsEditAdmin`.
     *
     * [permissions] is the "can do" direction, matching grammers' `set_admin_rights`. [rank] is
     * the custom admin badge, and [loadCurrent] loads the rights already in place first.
     */
    fun channelsEditAdmin(
        peer: TelegramPeer,
        user: TelegramPeer,
        permissions: ChatPermissions,
        rank: String? = null,
        loadCurrent: Boolean = false,
    ) {
        bridge.setAdminRights(peer.native, user.native, permissions.toBridge(), rank, loadCurrent)
    }

    /**
     * Joins a private chat from its invite link, which is grammers' `accept_invite_link`.
     *
     * Answers null for a chat the account is already in, exactly as [channelsJoinChannel] does.
     */
    fun messagesImportChatInvite(inviteLink: String): TelegramPeer? =
        bridge.acceptInviteLink(inviteLink)?.toCompatibility()

    /**
     * Extracts the hash of a private invite link, which is grammers' `parse_invite_link`.
     *
     * Answers null for a public `t.me/username` link or anything that is not a Telegram invite
     * URL, so a caller can check a link before importing it.
     */
    fun messagesParseInviteLink(inviteLink: String): String? = bridge.parseInviteLink(inviteLink)

    /**
     * Resolves a Bot API dialog id back into a peer, which is grammers' `resolve_peer`.
     *
     * The id is the one [TelegramPeer.id] reports: positive for a user, negative for a small
     * group, `-100…` for a channel. Without an [accessHash] Telegram only answers for a peer the
     * session already knows.
     */
    fun channelsResolvePeer(id: Long, accessHash: Long? = null): TelegramPeer? =
        bridge.resolvePeer(id, accessHash)?.toCompatibility()
}
