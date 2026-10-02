package com.github.badoualy.telegram.api

/** Listing dialogs, their metadata and totals, and clearing their unread state. */
interface DialogsApi : BridgeApi {
    /**
     * Lists a page of the dialogs, newest first, or the whole list when [all] is set.
     *
     * [offsetPeer], [offsetId] and [offsetDate] are the paging cursor: they resume the listing
     * after the dialog they name (the peer, the id of its last message, and that message's date in
     * epoch milliseconds), and must all be present to have any effect. Absent, the listing starts
     * from the top. [all] wins over [limit] and the cursor.
     */
    fun messagesGetDialogs(
        limit: Int = 50,
        offsetPeer: Long? = null,
        offsetId: Int? = null,
        offsetDate: Long? = null,
        all: Boolean = false,
    ): List<Dialog> =
        bridge.getDialogs(limit, offsetPeer, offsetId, offsetDate, all).map { it.toCompatibility() }

    /**
     * The same dialogs as [messagesGetDialogs], each carrying the metadata that listing projection
     * leaves out — the read markers, the unread reaction count, the notification settings and, for
     * a folder row, the folder's own counters. The paging cursor is the same as [messagesGetDialogs]
     * takes, and [all] walks the whole list in one call.
     *
     * The metadata lands on the `Dialog` model rather than a second type, so a caller keeps using
     * the same shape; the fields [messagesGetDialogs] cannot fill stay at their defaults here.
     */
    fun messagesGetDialogsMeta(
        limit: Int = 50,
        offsetPeer: Long? = null,
        offsetId: Int? = null,
        offsetDate: Long? = null,
        all: Boolean = false,
    ): List<Dialog> =
        bridge.getDialogsMeta(limit, offsetPeer, offsetId, offsetDate, all).map { it.toCompatibility() }

    /** The number of dialogs in the account, without fetching them. */
    fun messagesGetDialogsTotal(): Long = bridge.getDialogsTotal()

    fun messagesReadHistory(peer: TelegramPeer) {
        bridge.markAsRead(peer.native)
    }

    /**
     * Clears the account's pending mentions in [peer], marking them as read.
     *
     * [messagesReadHistory] does not clear mentions on its own, so a caller that wants a chat fully
     * cleared calls both.
     */
    fun messagesClearMentions(peer: TelegramPeer) {
        bridge.clearMentions(peer.native)
    }
}
