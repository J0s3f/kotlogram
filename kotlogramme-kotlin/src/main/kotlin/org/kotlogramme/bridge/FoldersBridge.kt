package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.ChatlistInviteResult
import org.kotlogramme.protocol.ChatlistPayload
import org.kotlogramme.protocol.ChatlistPeersPayload
import org.kotlogramme.protocol.ChatlistUpdatesAck
import org.kotlogramme.protocol.ChatlistUpdatesResult
import org.kotlogramme.protocol.CheckChatlistInvitePayload
import org.kotlogramme.protocol.DeleteExportedInvitePayload
import org.kotlogramme.protocol.DialogFilterSpec
import org.kotlogramme.protocol.DialogFoldersResult
import org.kotlogramme.protocol.EditExportedInvitePayload
import org.kotlogramme.protocol.EmptyPayload
import org.kotlogramme.protocol.ExportChatlistInvitePayload
import org.kotlogramme.protocol.ExportedInvite
import org.kotlogramme.protocol.ExportedInvitesResult
import org.kotlogramme.protocol.JoinChatlistInvitePayload
import org.kotlogramme.protocol.LeaveChatlistSuggestionsResult
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.UpdateDialogFilterPayload
import org.kotlogramme.protocol.UpdateDialogFiltersOrderPayload

/**
 * The typed dialog-filter (folder) family, built on grammers' TL layer because it exposes no
 * high-level folders API.
 */
internal interface FoldersBridge {
    val transport: Transport

    /** Lists the account's dialog filters. */
    @Operation("messagesGetDialogFilters")
    fun messagesGetDialogFilters(): DialogFoldersResult =
        transport.request("messagesGetDialogFilters", EmptyPayload)

    /**
     * Creates or replaces the filter named by [id].
     *
     * A null [filter] removes it: the layer has no separate delete constructor, so the update call
     * is sent with the filter flag cleared.
     */
    @Operation("messagesUpdateDialogFilter")
    fun messagesUpdateDialogFilter(id: Int, filter: DialogFilterSpec? = null) {
        transport.request<UpdateDialogFilterPayload, OperationResult>(
            "messagesUpdateDialogFilter",
            UpdateDialogFilterPayload(id, filter),
        )
    }

    /** Reorders the account's filters to match [order]. */
    @Operation("messagesUpdateDialogFiltersOrder")
    fun messagesUpdateDialogFiltersOrder(order: List<Int>) {
        transport.request<UpdateDialogFiltersOrderPayload, OperationResult>(
            "messagesUpdateDialogFiltersOrder",
            UpdateDialogFiltersOrderPayload(order),
        )
    }

    /** Exports the folder named by [filterId] as an invite labelled [title], answering the invite. */
    @Operation("chatlistsExportChatlistInvite")
    fun chatlistsExportChatlistInvite(
        filterId: Int,
        title: String,
        peers: List<PeerTarget> = emptyList(),
    ): ExportedInvite =
        transport.request(
            "chatlistsExportChatlistInvite",
            ExportChatlistInvitePayload(filterId, title, peers),
        )

    /** Drops the invite [slug] of the folder named by [filterId]. */
    @Operation("chatlistsDeleteExportedInvite")
    fun chatlistsDeleteExportedInvite(filterId: Int, slug: String) {
        transport.request<DeleteExportedInvitePayload, OperationResult>(
            "chatlistsDeleteExportedInvite",
            DeleteExportedInvitePayload(filterId, slug),
        )
    }

    /**
     * Changes the invite [slug] of the folder named by [filterId].
     *
     * A null [title] or [peers] leaves that part of the invite alone; a present empty [peers] clears
     * the invite's peer list.
     */
    @Operation("chatlistsEditExportedInvite")
    fun chatlistsEditExportedInvite(
        filterId: Int,
        slug: String,
        title: String? = null,
        peers: List<PeerTarget>? = null,
    ): ExportedInvite =
        transport.request(
            "chatlistsEditExportedInvite",
            EditExportedInvitePayload(filterId, slug, title, peers),
        )

    /** Lists the folder [filterId]'s exported invites. */
    @Operation("chatlistsGetExportedInvites")
    fun chatlistsGetExportedInvites(filterId: Int): ExportedInvitesResult =
        transport.request("chatlistsGetExportedInvites", ChatlistPayload(filterId))

    /** Looks up the invite [slug], keeping the joined and unjoined cases apart. */
    @Operation("chatlistsCheckChatlistInvite")
    fun chatlistsCheckChatlistInvite(slug: String): ChatlistInviteResult =
        transport.request("chatlistsCheckChatlistInvite", CheckChatlistInvitePayload(slug))

    /** Joins the folder the invite [slug] names. */
    @Operation("chatlistsJoinChatlistInvite")
    fun chatlistsJoinChatlistInvite(
        slug: String,
        peers: List<PeerTarget> = emptyList(),
    ): ChatlistUpdatesAck =
        transport.request(
            "chatlistsJoinChatlistInvite",
            JoinChatlistInvitePayload(slug, peers),
        )

    /** Lists the peers the joined folder [filterId] holds that this account is not in yet. */
    @Operation("chatlistsGetChatlistUpdates")
    fun chatlistsGetChatlistUpdates(filterId: Int): ChatlistUpdatesResult =
        transport.request("chatlistsGetChatlistUpdates", ChatlistPayload(filterId))

    /** Joins the peers of the joined folder [filterId]. */
    @Operation("chatlistsJoinChatlistUpdates")
    fun chatlistsJoinChatlistUpdates(
        filterId: Int,
        peers: List<PeerTarget>,
    ): ChatlistUpdatesAck =
        transport.request(
            "chatlistsJoinChatlistUpdates",
            ChatlistPeersPayload(filterId, peers),
        )

    /** Hides the pending updates of the joined folder [filterId]. */
    @Operation("chatlistsHideChatlistUpdates")
    fun chatlistsHideChatlistUpdates(filterId: Int) {
        transport.request<ChatlistPayload, OperationResult>(
            "chatlistsHideChatlistUpdates",
            ChatlistPayload(filterId),
        )
    }

    /** Reports the peers a caller could leave from the joined folder [filterId]. */
    @Operation("chatlistsGetLeaveChatlistSuggestions")
    fun chatlistsGetLeaveChatlistSuggestions(filterId: Int): LeaveChatlistSuggestionsResult =
        transport.request("chatlistsGetLeaveChatlistSuggestions", ChatlistPayload(filterId))

    /** Leaves the peers of the joined folder [filterId]. */
    @Operation("chatlistsLeaveChatlist")
    fun chatlistsLeaveChatlist(filterId: Int, peers: List<PeerTarget>): ChatlistUpdatesAck =
        transport.request("chatlistsLeaveChatlist", ChatlistPeersPayload(filterId, peers))
}

/** The [FoldersBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class FoldersOperations(override val transport: Transport) : FoldersBridge
