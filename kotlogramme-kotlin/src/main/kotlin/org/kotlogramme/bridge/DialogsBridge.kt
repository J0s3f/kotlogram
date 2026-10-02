package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.Dialog
import org.kotlogramme.protocol.DialogsTotal
import org.kotlogramme.protocol.EmptyPayload
import org.kotlogramme.protocol.LimitPayload
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget

/** Listing the dialog list and clearing its unread state. */
internal interface DialogsBridge {
    val transport: Transport

    /**
     * Lists a page of the dialogs, newest first.
     *
     * [offsetPeer], [offsetId] and [offsetDate] are the paging cursor: they resume the listing
     * after the dialog they name (the peer, the id of its last message, and that message's date in
     * epoch milliseconds), and must all be present to have any effect. Absent, the listing starts
     * from the top.
     */
    @Operation("getDialogs")
    fun getDialogs(
        limit: Int = 50,
        offsetPeer: Long? = null,
        offsetId: Int? = null,
        offsetDate: Long? = null,
    ): List<Dialog> = transport.request(
        "getDialogs",
        LimitPayload(limit, offsetPeer, offsetId, offsetDate),
    )

    /**
     * Lists the same dialogs as [getDialogs], each carrying the metadata that projection leaves
     * out under `Dialog.meta`. The paging cursor is the same as [getDialogs] takes.
     */
    @Operation("getDialogsMeta")
    fun getDialogsMeta(
        limit: Int = 50,
        offsetPeer: Long? = null,
        offsetId: Int? = null,
        offsetDate: Long? = null,
    ): List<Dialog> = transport.request(
        "getDialogsMeta",
        LimitPayload(limit, offsetPeer, offsetId, offsetDate),
    )

    /** Counts the dialogs the account has, without fetching them; grammers' `DialogIter::total`. */
    @Operation("getDialogsTotal")
    fun getDialogsTotal(): Long =
        transport.request<EmptyPayload, DialogsTotal>("getDialogsTotal", EmptyPayload).total

    @Operation("markAsRead")
    fun markAsRead(peer: Peer) {
        transport.request<PeerTarget, OperationResult>("markAsRead", PeerTarget(peer.nativeHandle))
    }

    /** Clears the account's pending mentions in [peer]; grammers' `Client::clear_mentions`. */
    @Operation("clearMentions")
    fun clearMentions(peer: Peer) {
        transport.request<PeerTarget, OperationResult>("clearMentions", PeerTarget(peer.nativeHandle))
    }
}

/** The [DialogsBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class DialogsOperations(override val transport: Transport) : DialogsBridge
