package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.DialogFilterSpec
import org.kotlogramme.protocol.DialogFoldersResult
import org.kotlogramme.protocol.EmptyPayload
import org.kotlogramme.protocol.OperationResult
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
}

/** The [FoldersBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class FoldersOperations(override val transport: Transport) : FoldersBridge
