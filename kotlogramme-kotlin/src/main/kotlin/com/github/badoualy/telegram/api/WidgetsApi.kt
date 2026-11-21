package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.WidgetPublicData
import org.kotlogramme.protocol.WidgetSpec

/**
 * Widgets: HTML payloads a bot attaches to a message (layer 233).
 *
 * Bots answer widget queries by reading `botWidgetQuery` / `inlineBotWidgetQuery` updates
 * ([TypedUpdate.widgetQuery]) and calling [messagesSetBotWidgetQueryAnswer]. Wallet-carrying query
 * types are not built here and stay behind `invokeRaw`. A widget can also be an inline answer, see
 * [InlineWidgetResult].
 */
interface WidgetsApi : BridgeApi {
    /**
     * Sends a widget to [peer]: a new one from [widget] or an existing one by [widgetId] (with its
     * per-chat [chatData]), never both. [caption] is the text the message carries.
     *
     * The layer answers with an update bundle, so the result is the sent message only when the bundle
     * named it; a scheduled send answers null and the message arrives on the update stream.
     */
    fun messagesSendWidget(
        peer: TelegramPeer,
        widget: WidgetSpec? = null,
        widgetId: Long? = null,
        chatData: String? = null,
        caption: String = "",
        replyToMessageId: Int? = null,
        silent: Boolean = false,
        scheduleDate: Long? = null,
    ): Message? = bridge.sendWidget(
        peer.native,
        widget,
        widgetId,
        chatData,
        caption,
        replyToMessageId,
        silent,
        scheduleDate,
    ).message?.toCompatibility()

    /**
     * Reads the public data of the widget on message [messageId]. Pass the [hash] of the copy you
     * hold and the answer is [WidgetPublicData.notModified] while it is current.
     */
    fun messagesGetWidgetPublicData(peer: TelegramPeer, messageId: Int, hash: Long = 0): WidgetPublicData =
        bridge.getWidgetPublicData(peer.native, messageId, hash)

    /** Replaces the public and/or per-chat data of the widget on a message; a null field is kept. */
    fun messagesEditMessageWidgetData(
        peer: TelegramPeer,
        messageId: Int,
        publicData: String? = null,
        chatData: String? = null,
    ) = bridge.editMessageWidgetData(peer.native, messageId, publicData, chatData)

    /** Like [messagesEditMessageWidgetData], for the inline message [messageId] identifies. */
    fun messagesEditInlineMessageWidgetData(
        messageId: InlineMessageId,
        publicData: String? = null,
        chatData: String? = null,
    ): Boolean = bridge.editInlineMessageWidgetData(
        messageId.dcId,
        messageId.accessHash,
        messageId.id,
        publicData,
        chatData,
    )

    /** Replaces the public data of every message that shows the shared widget [widgetId]. */
    fun messagesEditWidgetData(widgetId: Long, publicData: String, cacheTimeSeconds: Int? = null) =
        bridge.editWidgetData(widgetId, publicData, cacheTimeSeconds)

    /**
     * Reads the bot's answer to the widget query [dataBase64] asked on a message; null while the bot
     * has not answered.
     */
    fun messagesGetBotWidgetQueryAnswer(
        peer: TelegramPeer,
        messageId: Int,
        dataBase64: String,
        withWallet: Boolean = false,
    ): String? = bridge.getBotWidgetQueryAnswer(peer.native, messageId, dataBase64, withWallet)

    /** Answers the widget query [queryId], which a bot reads off a `botWidgetQuery` update. */
    fun messagesSetBotWidgetQueryAnswer(queryId: Long, result: String? = null) =
        bridge.setBotWidgetQueryAnswer(queryId, result)
}
