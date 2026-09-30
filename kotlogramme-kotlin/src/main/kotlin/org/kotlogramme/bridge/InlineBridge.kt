package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.AnswerCallbackQueryPayload
import org.kotlogramme.protocol.AnswerInlineQueryPayload
import org.kotlogramme.protocol.EditInlineMessagePayload
import org.kotlogramme.protocol.EditedInlineMessage
import org.kotlogramme.protocol.EntitySpec
import org.kotlogramme.protocol.InlineEditMediaSpec
import org.kotlogramme.protocol.InlineMessageIdPayload
import org.kotlogramme.protocol.InlineQueryPayload
import org.kotlogramme.protocol.InlineQueryResults
import org.kotlogramme.protocol.InlineResultSpec
import org.kotlogramme.protocol.InlineSwitchPm
import org.kotlogramme.protocol.InlineSwitchWebview
import org.kotlogramme.protocol.MarkupSpec
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.SendInlineBotResultPayload
import org.kotlogramme.protocol.SentInlineResult

/**
 * Running an inline bot and answering the updates it produces.
 *
 * grammers models the answering side on the update values themselves, which borrow the session and
 * the peers they were delivered with; a request/response bridge cannot hold one, so the caller
 * passes the identifiers it read off the projected update and the native handler invokes the same
 * request the builder would have.
 */
internal interface InlineBridge {
    val transport: Transport

    /**
     * Asks the inline bot [bot] for a page of results, as if [query] had been typed after its
     * username.
     *
     * [peer] is the chat the query is typed in, which some bots use to shape their results; it is
     * left empty when not given. [offset] is the previous answer's `nextOffset`, which asks for the
     * page after it.
     */
    @Operation("inlineQuery")
    fun inlineQuery(
        bot: Peer,
        query: String,
        peer: Peer? = null,
        offset: String? = null,
    ): InlineQueryResults = transport.request(
        "inlineQuery",
        InlineQueryPayload(
            PeerTarget(bot.nativeHandle),
            query,
            peer?.let { PeerTarget(it.nativeHandle) },
            offset,
        ),
    )

    /**
     * Answers the callback query [queryId], which a caller reads off a projected
     * `CallbackQueryUpdate`.
     *
     * [text] is shown to the user; [alert] shows it as a modal window instead of a fading toast.
     * Every callback query should be answered even with no text, or the user's client shows the
     * button as unresponsive.
     */
    @Operation("answerCallbackQuery")
    fun answerCallbackQuery(
        queryId: Long,
        text: String? = null,
        alert: Boolean = false,
        cacheTimeSeconds: Int = 0,
    ) {
        transport.request<AnswerCallbackQueryPayload, OperationResult>(
            "answerCallbackQuery",
            AnswerCallbackQueryPayload(queryId, text, alert, cacheTimeSeconds),
        )
    }

    /**
     * Answers the inline query [queryId] with [results], which a caller reads off a projected
     * `InlineQueryUpdate`.
     *
     * [nextOffset] is the offset the client sends back to ask for the next page; leave it out on
     * the last page. [switchPm] offers to move the query to the bot's private chat and
     * [switchWebview] to open it in a webview.
     */
    @Operation("answerInlineQuery")
    fun answerInlineQuery(
        queryId: Long,
        results: List<InlineResultSpec>,
        cacheTimeSeconds: Int = 0,
        gallery: Boolean = false,
        isPrivate: Boolean = false,
        nextOffset: String? = null,
        switchPm: InlineSwitchPm? = null,
        switchWebview: InlineSwitchWebview? = null,
    ) {
        transport.request<AnswerInlineQueryPayload, OperationResult>(
            "answerInlineQuery",
            AnswerInlineQueryPayload(
                queryId,
                results,
                cacheTimeSeconds,
                gallery,
                isPrivate,
                nextOffset,
                switchPm,
                switchWebview,
            ),
        )
    }

    /**
     * Sends the inline result a caller chose, which it reads off a projected `InlineQueryUpdate`
     * together with its query id.
     *
     * The layer answers with the sent message when it could be identified; when it could not, the
     * send still succeeded and the update stream delivers the message asynchronously, so only the
     * acknowledgement is answered.
     */
    @Operation("sendInlineBotResult")
    fun sendInlineBotResult(
        peer: Peer,
        queryId: Long,
        resultId: String,
        silent: Boolean = false,
        background: Boolean = false,
        clearDraft: Boolean = false,
        hideVia: Boolean = false,
        replyToMessageId: Int? = null,
        scheduleDate: Long? = null,
    ): SentInlineResult = transport.request(
        "sendInlineBotResult",
        SendInlineBotResultPayload(
            PeerTarget(peer.nativeHandle),
            queryId,
            resultId,
            silent,
            background,
            clearDraft,
            hideVia,
            replyToMessageId,
            scheduleDate,
        ),
    )

    /**
     * Edits the inline message a chosen result produced, addressed by the identifier a projected
     * `InlineSendUpdate` or inline `CallbackQueryUpdate` carries.
     *
     * The layer reports whether it accepted the edit, which is what this answers.
     */
    @Operation("editInlineMessage")
    fun editInlineMessage(
        dcId: Int,
        accessHash: Long,
        id: Long,
        text: String = "",
        linkPreview: Boolean = true,
        invertMedia: Boolean = false,
        parseMode: String? = null,
        entities: List<EntitySpec>? = null,
        replyMarkup: MarkupSpec? = null,
        media: InlineEditMediaSpec? = null,
    ): Boolean = transport.request<EditInlineMessagePayload, EditedInlineMessage>(
        "editInlineMessage",
        EditInlineMessagePayload(
            InlineMessageIdPayload(dcId, accessHash, id),
            text,
            linkPreview,
            invertMedia,
            parseMode,
            entities,
            replyMarkup,
            media,
        ),
    ).edited
}

/** The [InlineBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class InlineOperations(override val transport: Transport) : InlineBridge
