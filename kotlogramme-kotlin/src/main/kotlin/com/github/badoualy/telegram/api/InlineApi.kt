package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.InlineArticleSpec
import org.kotlogramme.protocol.InlineEditMediaSpec
import org.kotlogramme.protocol.InlineResultSpec
import org.kotlogramme.protocol.InlineSwitchPm as BridgeInlineSwitchPm
import org.kotlogramme.protocol.InlineSwitchWebview as BridgeInlineSwitchWebview

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
     * [nextOffset] is the offset the client sends back to ask for the next page, [switchPm] offers
     * to move the query to the bot's private chat and [switchWebview] to open it in a webview.
     * [results] is a mix of [InlineArticle] and [InlineMediaResult] results.
     */
    fun answerInlineQuery(
        queryId: Long,
        results: List<InlineAnswerResult>,
        cacheTimeSeconds: Int = 0,
        gallery: Boolean = false,
        isPrivate: Boolean = false,
        nextOffset: String? = null,
        switchPm: InlineSwitchPm? = null,
        switchWebview: InlineSwitchWebview? = null,
    ) = bridge.answerInlineQuery(
        queryId,
        results.map { it.asSpec() },
        cacheTimeSeconds,
        gallery,
        isPrivate,
        nextOffset,
        switchPm?.asSpec(),
        switchWebview?.asSpec(),
    )

    /**
     * Sends the inline result a caller chose, which a caller reads off an [InlineQueryUpdate]
     * together with its [InlineQueryResults.queryId].
     *
     * The layer answers with the sent message when it could be identified; when it could not (a
     * scheduled send, for instance), the send still succeeded and the update stream delivers the
     * message asynchronously, so this returns null and the caller waits for the update.
     *
     * [scheduleDate] is epoch milliseconds at which to schedule the send. [hideVia] hides the "via
     * @bot" caption.
     */
    fun sendInlineBotResult(
        peer: TelegramPeer,
        queryId: Long,
        resultId: String,
        silent: Boolean = false,
        background: Boolean = false,
        clearDraft: Boolean = false,
        hideVia: Boolean = false,
        replyToMessageId: Int? = null,
        scheduleDate: Long? = null,
    ): Message? = bridge.sendInlineBotResult(
        peer.native,
        queryId,
        resultId,
        silent,
        background,
        clearDraft,
        hideVia,
        replyToMessageId,
        scheduleDate,
    ).message?.toCompatibility()

    /**
     * Edits the inline message a chosen result produced, addressed by [messageId].
     *
     * The layer reports whether it accepted the edit. [text] is null on a media-only edit; an
     * empty text with [media] set is omitted, while an empty text without it clears the message.
     * [media] is URL-only, because an inline message has no local file to upload.
     */
    fun editInlineMessage(
        messageId: InlineMessageId,
        text: String = "",
        linkPreview: Boolean = true,
        invertMedia: Boolean = false,
        parseMode: CaptionParseMode = CaptionParseMode.NONE,
        entities: List<MessageEntity>? = null,
        replyMarkup: ReplyMarkup? = null,
        media: InlineEditMedia? = null,
    ): Boolean = bridge.editInlineMessage(
        messageId.dcId,
        messageId.accessHash,
        messageId.id,
        text,
        linkPreview,
        invertMedia,
        parseMode.wireName,
        entities?.map { it.asSpec() },
        replyMarkup?.asSpec(),
        media?.asSpec(),
    )
}

/** The result the bridge is built from: an article or a media result. */
internal fun InlineAnswerResult.asSpec(): InlineResultSpec = when (this) {
    is InlineArticle -> InlineResultSpec(
        kind = "article",
        title = title,
        messageText = messageText,
        linkPreview = linkPreview,
        invertMedia = invertMedia,
        id = id,
        description = description,
        url = url,
        thumbUrl = thumbUrl,
    )
    is InlineMediaResult -> InlineResultSpec(
        kind = kind,
        id = id,
        title = title,
        description = description,
        url = url,
        thumbUrl = thumbUrl,
        contentUrl = contentUrl,
        caption = caption,
    )
}

/** The article the guest-chat answer is built from, which is the one grammers' `Article` builds. */
internal fun InlineArticle.asArticleSpec(): InlineArticleSpec =
    InlineArticleSpec(title, messageText, id, description, url, thumbUrl, linkPreview, invertMedia)

/** The switch-to-private-message prompt the bridge is built from. */
internal fun InlineSwitchPm.asSpec(): BridgeInlineSwitchPm = BridgeInlineSwitchPm(text, startParam)

/** The switch-to-webview prompt the bridge is built from. */
internal fun InlineSwitchWebview.asSpec(): BridgeInlineSwitchWebview =
    BridgeInlineSwitchWebview(text, url)

/** The URL media the bridge is built from. */
internal fun InlineEditMedia.asSpec(): InlineEditMediaSpec = InlineEditMediaSpec(url, kind.wireName)
