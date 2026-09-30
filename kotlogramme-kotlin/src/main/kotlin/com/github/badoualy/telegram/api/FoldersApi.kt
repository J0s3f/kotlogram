package com.github.badoualy.telegram.api

/**
 * The typed dialog-filter (folder) family, mapped onto grammers' TL layer.
 *
 * These operations have no high-level grammers equivalent, so the bridge builds the raw
 * `messages.*` requests; the facade keeps the same Kotlogram-shaped surface as the other domains.
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
}
