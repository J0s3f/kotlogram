package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.BotWidgetQueryAnswer
import org.kotlogramme.protocol.EditInlineMessageWidgetDataPayload
import org.kotlogramme.protocol.EditMessageWidgetDataPayload
import org.kotlogramme.protocol.EditWidgetDataPayload
import org.kotlogramme.protocol.EditedInlineMessage
import org.kotlogramme.protocol.GetBotWidgetQueryAnswerPayload
import org.kotlogramme.protocol.GetWidgetPublicDataPayload
import org.kotlogramme.protocol.InlineMessageIdPayload
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.SendWidgetPayload
import org.kotlogramme.protocol.SentWidget
import org.kotlogramme.protocol.SetBotWidgetQueryAnswerPayload
import org.kotlogramme.protocol.WidgetPublicData
import org.kotlogramme.protocol.WidgetSpec

/**
 * Widgets: HTML payloads a bot attaches to a message (layer 233).
 *
 * grammers has no helper for any of this, so the native side builds the layer's requests itself.
 * Wallet-carrying query types stay behind `invokeRaw`. See `docs/layer-233-facade.md`.
 */
internal interface WidgetsBridge {
    val transport: Transport

    /**
     * Sends a widget to [peer]: a new one from [widget] or an existing one by [widgetId] (with its
     * per-chat [chatData]), never both. [caption] is the message text it travels with.
     *
     * The layer answers with an update bundle, so [SentWidget.message] is only set when the bundle
     * named the message; a scheduled send is acknowledged alone.
     */
    @Operation("sendWidget")
    fun sendWidget(
        peer: Peer,
        widget: WidgetSpec? = null,
        widgetId: Long? = null,
        chatData: String? = null,
        caption: String = "",
        replyToMessageId: Int? = null,
        silent: Boolean = false,
        scheduleDate: Long? = null,
    ): SentWidget = transport.request(
        "sendWidget",
        SendWidgetPayload(
            PeerTarget(peer.nativeHandle),
            widget,
            widgetId,
            chatData,
            caption,
            replyToMessageId,
            silent,
            scheduleDate,
        ),
    )

    /**
     * Reads the public data of the widget on message [messageId]. Pass the [hash] of the copy you hold
     * and the answer is `notModified` while it is current.
     */
    @Operation("getWidgetPublicData")
    fun getWidgetPublicData(peer: Peer, messageId: Int, hash: Long = 0): WidgetPublicData =
        transport.request(
            "getWidgetPublicData",
            GetWidgetPublicDataPayload(PeerTarget(peer.nativeHandle), messageId, hash),
        )

    /** Replaces the public and/or per-chat data of the widget on a message; a null field is kept. */
    @Operation("editMessageWidgetData")
    fun editMessageWidgetData(
        peer: Peer,
        messageId: Int,
        publicData: String? = null,
        chatData: String? = null,
    ) {
        transport.request<EditMessageWidgetDataPayload, OperationResult>(
            "editMessageWidgetData",
            EditMessageWidgetDataPayload(PeerTarget(peer.nativeHandle), messageId, publicData, chatData),
        )
    }

    /** Like [editMessageWidgetData], for the inline message an inline update identifies. */
    @Operation("editInlineMessageWidgetData")
    fun editInlineMessageWidgetData(
        dcId: Int,
        accessHash: Long,
        id: Long,
        publicData: String? = null,
        chatData: String? = null,
    ): Boolean = transport.request<EditInlineMessageWidgetDataPayload, EditedInlineMessage>(
        "editInlineMessageWidgetData",
        EditInlineMessageWidgetDataPayload(InlineMessageIdPayload(dcId, accessHash, id), publicData, chatData),
    ).edited

    /** Replaces the public data of every message that shows the shared widget [widgetId]. */
    @Operation("editWidgetData")
    fun editWidgetData(widgetId: Long, publicData: String, cacheTimeSeconds: Int? = null) {
        transport.request<EditWidgetDataPayload, OperationResult>(
            "editWidgetData",
            EditWidgetDataPayload(widgetId, publicData, cacheTimeSeconds),
        )
    }

    /**
     * Reads the bot's answer to the widget query [dataBase64] asked on a message; null while the bot
     * has not answered. Only the plain `data` query type is built here.
     */
    @Operation("getBotWidgetQueryAnswer")
    fun getBotWidgetQueryAnswer(
        peer: Peer,
        messageId: Int,
        dataBase64: String,
        withWallet: Boolean = false,
    ): String? = transport.request<GetBotWidgetQueryAnswerPayload, BotWidgetQueryAnswer>(
        "getBotWidgetQueryAnswer",
        GetBotWidgetQueryAnswerPayload(PeerTarget(peer.nativeHandle), messageId, dataBase64, withWallet),
    ).result

    /** Answers the widget query [queryId], which a bot reads off a `botWidgetQuery` update. */
    @Operation("setBotWidgetQueryAnswer")
    fun setBotWidgetQueryAnswer(queryId: Long, result: String? = null) {
        transport.request<SetBotWidgetQueryAnswerPayload, OperationResult>(
            "setBotWidgetQueryAnswer",
            SetBotWidgetQueryAnswerPayload(queryId, result),
        )
    }
}

/** The [WidgetsBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class WidgetsOperations(override val transport: Transport) : WidgetsBridge
