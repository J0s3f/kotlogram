package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.ForceReplyPayload
import org.kotlogramme.protocol.HideKeyboardPayload
import org.kotlogramme.protocol.InlineButtonSpec
import org.kotlogramme.protocol.InlineMarkupPayload
import org.kotlogramme.protocol.KeyboardButtonSpec
import org.kotlogramme.protocol.KeyboardMarkupPayload
import org.kotlogramme.protocol.MessageMarkupPayload
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.ReplyMarkup as BridgeReplyMarkup

/**
 * Reply markups: reading the one a message carries and building the four grammers can send.
 *
 * grammers attaches a markup to an outgoing message through `InputMessage::reply_markup`, which the
 * message operations own, so the builders here hand the markup back rather than send it.
 */
internal interface MarkupBridge {
    val transport: Transport

    /**
     * Reads the markup a message carries, which is grammers' `Message::reply_markup`.
     *
     * Only a message a bot sent carries one, so this answers null for anything else: the native
     * side reports "no markup" as a bare `null`, which [Transport.decode] decodes when asked for a
     * nullable result.
     */
    @Operation("getReplyMarkup")
    fun getReplyMarkup(peer: Peer, messageId: Int): BridgeReplyMarkup? =
        transport.request<MessageMarkupPayload, BridgeReplyMarkup?>(
            "getReplyMarkup",
            MessageMarkupPayload(PeerTarget(peer.nativeHandle), messageId),
        )

    /** Builds the markup grammers' `reply_markup::inline` builds: buttons attached to the message. */
    @Operation("buildInlineMarkup")
    fun buildInlineMarkup(rows: List<List<InlineButtonSpec>>): BridgeReplyMarkup = transport.request(
        "buildInlineMarkup",
        InlineMarkupPayload(rows),
    )

    /**
     * Builds the markup grammers' `reply_markup::keyboard` builds, with the options its inherent
     * methods offer.
     *
     * The layer's `persistent` flag and input-field placeholder have no grammers builder method, so
     * a built keyboard always reports them unset.
     */
    @Operation("buildReplyKeyboard")
    fun buildReplyKeyboard(
        rows: List<List<KeyboardButtonSpec>>,
        fitSize: Boolean = false,
        singleUse: Boolean = false,
        selective: Boolean = false,
    ): BridgeReplyMarkup = transport.request(
        "buildReplyKeyboard",
        KeyboardMarkupPayload(rows, fitSize, singleUse, selective),
    )

    /** Builds the markup grammers' `reply_markup::force_reply` builds. */
    @Operation("buildForceReply")
    fun buildForceReply(singleUse: Boolean = false, selective: Boolean = false): BridgeReplyMarkup =
        transport.request("buildForceReply", ForceReplyPayload(singleUse, selective))

    /**
     * Builds the markup grammers' `reply_markup::hide` builds, which removes a keyboard this bot sent
     * earlier.
     */
    @Operation("buildHideKeyboard")
    fun buildHideKeyboard(selective: Boolean = false): BridgeReplyMarkup =
        transport.request("buildHideKeyboard", HideKeyboardPayload(selective))
}

/** The [MarkupBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class MarkupOperations(override val transport: Transport) : MarkupBridge
