package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.GetMessageActionPayload
import org.kotlogramme.protocol.GetMessageActionResult
import org.kotlogramme.protocol.MessageAction
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.SendChatActionPayload

/** Reporting the chat action, and reading the service action off a message. */
internal interface ActionsBridge {
    val transport: Transport

    /**
     * Sends the one-shot chat action [action], the status Telegram shows next to the chat.
     *
     * [action] is one of the wire names `com.github.badoualy.telegram.api.ChatAction` lists, and
     * [progress] is the percentage the uploading statuses report.
     */
    @Operation("sendChatAction")
    fun sendChatAction(peer: Peer, action: String, progress: Int? = null, topicId: Int? = null) {
        transport.request<SendChatActionPayload, OperationResult>(
            "sendChatAction",
            SendChatActionPayload(PeerTarget(peer.nativeHandle), action, progress, topicId),
        )
    }

    /** The service action of a message, or null when the message is an ordinary one. */
    @Operation("getMessageAction")
    fun getMessageAction(peer: Peer, messageId: Int): MessageAction? =
        transport.request<GetMessageActionPayload, GetMessageActionResult>(
            "getMessageAction",
            GetMessageActionPayload(PeerTarget(peer.nativeHandle), messageId),
        ).action
}

/** The [ActionsBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class ActionsOperations(override val transport: Transport) : ActionsBridge
