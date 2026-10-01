package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.PeerTarget as BridgePeerTarget

/**
 * The typed dialog-filter (folder) family, mapped onto grammers' TL layer.
 *
 * These operations have no high-level grammers equivalent, so the bridge builds the raw
 * `messages.*` requests; the facade keeps the same Kotlogram-shaped surface as the other domains.
 * The sharing half builds the `chatlists.*` requests instead.
 */
interface FoldersApi : BridgeApi {
    /** Lists the account's dialog filters. */
    fun messagesGetDialogFilters(): DialogFolders =
        bridge.messagesGetDialogFilters().toCompatibility()

    /**
     * Creates or replaces the filter named by [id].
     *
     * A null [filter] removes it, which is the layer's own delete form.
     */
    fun messagesUpdateDialogFilter(id: Int, filter: DialogFolderSpec? = null) {
        bridge.messagesUpdateDialogFilter(id, filter?.toBridge())
    }

    /** Reorders the account's filters to match [order]. */
    fun messagesUpdateDialogFiltersOrder(order: List<Int>) {
        bridge.messagesUpdateDialogFiltersOrder(order)
    }

    /**
     * Exports the folder [filterId] as an invite labelled [title] covering [peers].
     *
     * The answer is the invite's label, its URL and the peers it covers. The invite is addressed
     * everywhere else by the [FolderInvite.slug] read out of that URL.
     */
    fun chatlistsExportChatlistInvite(
        filterId: Int,
        title: String,
        peers: List<TelegramPeer> = emptyList(),
    ): FolderInvite =
        bridge.chatlistsExportChatlistInvite(filterId, title, peers.bridgeTargets()).toCompatibility()

    /** Drops the invite [slug] of the folder [filterId]. */
    fun chatlistsDeleteExportedInvite(filterId: Int, slug: String) {
        bridge.chatlistsDeleteExportedInvite(filterId, slug)
    }

    /**
     * Changes the invite [slug] of the folder [filterId].
     *
     * A null [title] or [peers] leaves that part of the invite as it was; a non-null empty [peers]
     * clears the invite's peer list.
     */
    fun chatlistsEditExportedInvite(
        filterId: Int,
        slug: String,
        title: String? = null,
        peers: List<TelegramPeer>? = null,
    ): FolderInvite =
        bridge.chatlistsEditExportedInvite(filterId, slug, title, peers?.bridgeTargets())
            .toCompatibility()

    /** Lists the exported invites of the folder [filterId]. */
    fun chatlistsGetExportedInvites(filterId: Int): FolderInvites =
        bridge.chatlistsGetExportedInvites(filterId).toCompatibility()

    /**
     * Looks up the invite [slug].
     *
     * The answer says which case it is: a [FolderInviteCheck.New] is still unjoined and carries the
     * title and roster, a [FolderInviteCheck.Already] names the folder the slug already points at.
     */
    fun chatlistsCheckChatlistInvite(slug: String): FolderInviteCheck =
        bridge.chatlistsCheckChatlistInvite(slug).toCompatibility()

    /**
     * Joins the folder the invite [slug] names.
     *
     * For an unjoined slug [peers] is the invite's roster; for one already joined it is the
     * [FolderInviteCheck.Already.missingPeers] of a prior check. The answer acknowledges the call
     * and reports the chats it touched.
     */
    fun chatlistsJoinChatlistInvite(
        slug: String,
        peers: List<TelegramPeer> = emptyList(),
    ): FolderUpdatesAck =
        bridge.chatlistsJoinChatlistInvite(slug, peers.bridgeTargets()).toCompatibility()

    /** Lists the peers the joined folder [filterId] holds that this account is not in yet. */
    fun chatlistsGetChatlistUpdates(filterId: Int): FolderUpdates =
        bridge.chatlistsGetChatlistUpdates(filterId).toCompatibility()

    /** Joins [peers], the missing peers of the joined folder [filterId]. */
    fun chatlistsJoinChatlistUpdates(
        filterId: Int,
        peers: List<TelegramPeer>,
    ): FolderUpdatesAck =
        bridge.chatlistsJoinChatlistUpdates(filterId, peers.bridgeTargets()).toCompatibility()

    /** Hides the pending updates of the joined folder [filterId]. */
    fun chatlistsHideChatlistUpdates(filterId: Int) {
        bridge.chatlistsHideChatlistUpdates(filterId)
    }

    /** Reports the peers a caller could leave from the joined folder [filterId]. */
    fun chatlistsGetLeaveChatlistSuggestions(filterId: Int): FolderLeaveSuggestions =
        bridge.chatlistsGetLeaveChatlistSuggestions(filterId).toCompatibility()

    /** Leaves [peers] of the joined folder [filterId]. */
    fun chatlistsLeaveChatlist(
        filterId: Int,
        peers: List<TelegramPeer>,
    ): FolderUpdatesAck =
        bridge.chatlistsLeaveChatlist(filterId, peers.bridgeTargets()).toCompatibility()
}

/** Resolves each registered peer to the bridge's peer target, which the native side re-resolves. */
private fun List<TelegramPeer>.bridgeTargets() =
    map { BridgePeerTarget(it.native.nativeHandle) }
