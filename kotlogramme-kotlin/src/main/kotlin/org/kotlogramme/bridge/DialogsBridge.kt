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

    @Operation("getDialogs")
    fun getDialogs(limit: Int = 50): List<Dialog> = transport.request("getDialogs", LimitPayload(limit))

    /**
     * Lists the same dialogs as [getDialogs], each carrying the metadata that projection leaves
     * out under `Dialog.meta`.
     */
    @Operation("getDialogsMeta")
    fun getDialogsMeta(limit: Int = 50): List<Dialog> =
        transport.request("getDialogsMeta", LimitPayload(limit))

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
