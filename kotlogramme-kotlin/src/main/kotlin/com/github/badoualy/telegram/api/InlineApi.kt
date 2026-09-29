package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.InlineArticleSpec
import org.kotlogramme.protocol.InlineSwitchPmSpec

/**
 * Inline bots: asking one for results, answering the bot updates that carry a query, and editing
 * the inline message a chosen result produced.
 *
 * The answering operations are driven by the projected updates: a caller reads `queryId` off a
 * [CallbackQueryUpdate] or [InlineQueryUpdate], or the message id off an [InlineSendUpdate], and
 * passes it back here together with the answer. The update value itself does not survive the
 * bridge, so the identifiers are what carries the exchange.
 */
interface InlineApi : BridgeApi {
    /**
     * Asks the inline bot [bot] for a page of results, as if [query] had been typed after its
     * username.
     *
     * [peer] is the chat the query is typed in, which some bots use to shape their results.
     * [offset] is a previous answer's [InlineQueryResults.nextOffset], which asks for the page
     * after it.
     */
    fun inlineQuery(
        bot: TelegramPeer,
        query: String,
        peer: TelegramPeer? = null,
        offset: String? = null,
    ): InlineQueryResults = bridge.inlineQuery(bot.native, query, peer?.native, offset).toCompatibility()

    /**
     * Answers the callback query [queryId], which a caller reads off a [CallbackQueryUpdate].
     *
     * [text] is shown to the user and [alert] shows it as a modal window rather than a toast. A
     * query should be answered even with no text, or the user's client shows the button as
     * unresponsive.
     */
    fun answerCallbackQuery(
        queryId: Long,
        text: String? = null,
        alert: Boolean = false,
        cacheTimeSeconds: Int = 0,
    ) = bridge.answerCallbackQuery(queryId, text, alert, cacheTimeSeconds)

    /**
     * Answers the inline query [queryId] with [results], which a caller reads off an
     * [InlineQueryUpdate].
     *
     * [nextOffset] is the offset the client sends back to ask for the next page, and [switchPm]
     * offers to move the query to the bot's private chat.
     */
    fun answerInlineQuery(
        queryId: Long,
        results: List<InlineArticle>,
        cacheTimeSeconds: Int = 0,
        gallery: Boolean = false,
        isPrivate: Boolean = false,
        nextOffset: String? = null,
        switchPm: InlineSwitchPm? = null,
    ) = bridge.answerInlineQuery(
        queryId,
        results.map { it.asSpec() },
        cacheTimeSeconds,
        gallery,
        isPrivate,
        nextOffset,
        switchPm?.asSpec(),
    )

    /**
     * Edits the inline message a chosen result produced, addressed by [messageId].
     *
     * The layer reports whether it accepted the edit.
     */
    fun editInlineMessage(
        messageId: InlineMessageId,
        text: String,
        linkPreview: Boolean = true,
        invertMedia: Boolean = false,
    ): Boolean = bridge.editInlineMessage(
        messageId.dcId,
        messageId.accessHash,
        messageId.id,
        text,
        linkPreview,
        invertMedia,
    )
}

/** The article result the bridge is built from, which is the one grammers' `Article` builds. */
internal fun InlineArticle.asSpec(): InlineArticleSpec =
    InlineArticleSpec(title, messageText, id, description, url, thumbUrl, linkPreview, invertMedia)

/** The switch-to-private-message prompt the bridge is built from. */
internal fun InlineSwitchPm.asSpec(): InlineSwitchPmSpec = InlineSwitchPmSpec(text, startParam)
