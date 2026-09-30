package org.kotlogramme.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Payloads, results and wire models of the inline-bot operations. */

/**
 * Payload of `inlineQuery`: the bot to ask, the text to ask it, the peer the query is typed in and
 * the offset that asks for the next page of an earlier answer.
 */
@Serializable
internal data class InlineQueryPayload(
    val bot: PeerTarget,
    val query: String,
    val peer: PeerTarget? = null,
    val offset: String? = null,
)

/**
 * One page of results an inline bot answered a query with.
 *
 * [queryId] identifies the query, which an inline answer or a chosen result is sent with, and
 * [nextOffset] is the offset that asks for the page after this one; it is null on the last page.
 * [gallery] asks the client to show the results as a grid rather than a list. [switchPm] and
 * [switchWebview] are the prompts the bot answered with, when it sent any.
 */
@Serializable
data class InlineQueryResults(
    val queryId: Long,
    val nextOffset: String? = null,
    val gallery: Boolean = false,
    val switchPm: InlineSwitchPm? = null,
    val switchWebview: InlineSwitchWebview? = null,
    val results: List<InlineResult> = emptyList(),
)

/**
 * One result of an inline query.
 *
 * The layer's two result constructors share an id, a type name and an optional title and
 * description, and differ in how they point at their content: [kind] names which one this is,
 * `result` carries [url], [thumb] and [content] and `mediaResult` carries [photoId] or
 * [documentId]. A field the other variant cannot answer is null.
 *
 * [type] is the layer's result type, for example `article`. [sendMessageText] is the text the
 * result's message would post when it is a text message, and null for a media-carrying one.
 */
@Serializable
data class InlineResult(
    val kind: String,
    val id: String,
    @SerialName("type") val type: String,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val thumb: InlineWebDocument? = null,
    val content: InlineWebDocument? = null,
    val photoId: Long? = null,
    val documentId: Long? = null,
    val sendMessageText: String? = null,
)

/**
 * A web document a result points at.
 *
 * grammers exposes only the URL, the size and the MIME type of the layer's two web-document
 * constructors, so the access hash and the attributes are absent.
 */
@Serializable
data class InlineWebDocument(
    val url: String,
    val size: Int,
    val mimeType: String,
)

/** Payload of `answerCallbackQuery`. */
@Serializable
internal data class AnswerCallbackQueryPayload(
    val queryId: Long,
    val text: String? = null,
    val alert: Boolean = false,
    val cacheTimeSeconds: Int = 0,
)

/**
 * One article result an inline answer carries, mirroring grammers' `Article` builder.
 *
 * `Article` is the only result grammers 0.8.1 has a builder for; it wraps an `InputMessage`, so
 * these are the text message it sends plus the metadata Telegram renders it with. The reply markup
 * and the format entities an `InputMessage` can also carry are absent, because the markup and
 * message domains own those payload shapes.
 */
@Serializable
internal data class InlineArticleSpec(
    val title: String,
    val messageText: String,
    val id: String? = null,
    val description: String? = null,
    val url: String? = null,
    val thumbUrl: String? = null,
    val linkPreview: Boolean = true,
    val invertMedia: Boolean = false,
)

/** The prompt that offers to switch an inline query to the bot's private chat. */
@Serializable
data class InlineSwitchPm(val text: String, val startParam: String)

/** The prompt that offers to open an inline query's result in a webview. */
@Serializable
data class InlineSwitchWebview(val text: String, val url: String)

/**
 * One result an inline answer carries, tagged by [kind].
 *
 * [kind] is `article` when absent (the default, mirroring grammers' `Article` builder) or one of
 * the media kinds `photo`, `gif`, `video`, `voice` and `document`, each described by a
 * [contentUrl]. grammers' builder only covers articles, so the media kinds are built on the native
 * side from the raw layer constructors. This is the same flat shape the reply markup uses: a field
 * the named variant cannot answer is left out, and every field is optional but the variant's own.
 */
@Serializable
data class InlineResultSpec(
    /** `article` when absent, or `photo`, `gif`, `video`, `voice`, `document`. */
    val kind: String? = null,
    // Article fields.
    val title: String? = null,
    /** The text the article's message posts. */
    val messageText: String? = null,
    val linkPreview: Boolean? = null,
    val invertMedia: Boolean = false,
    // Shared fields.
    val id: String? = null,
    val description: String? = null,
    val url: String? = null,
    val thumbUrl: String? = null,
    // Media fields.
    /** The URL a media kind's content is fetched from. */
    val contentUrl: String? = null,
    /** The caption a media kind's message carries. */
    val caption: String? = null,
)

/** Payload of `answerInlineQuery`. */
@Serializable
internal data class AnswerInlineQueryPayload(
    val queryId: Long,
    val results: List<InlineResultSpec>,
    val cacheTimeSeconds: Int = 0,
    val gallery: Boolean = false,
    /** grammers' `Answer::private`, which caches the results on the user's client. */
    @SerialName("private") val isPrivate: Boolean = false,
    /** The offset the client sends back to ask for the next page. */
    val nextOffset: String? = null,
    val switchPm: InlineSwitchPm? = null,
    val switchWebview: InlineSwitchWebview? = null,
)

/**
 * Payload of `sendInlineBotResult`: the peer the chosen result is sent to, the query it was chosen
 * from, the result id and the send options the layer's `messages.SendInlineBotResult` carries.
 */
@Serializable
internal data class SendInlineBotResultPayload(
    val peer: PeerTarget,
    val queryId: Long,
    val resultId: String,
    val silent: Boolean = false,
    val background: Boolean = false,
    val clearDraft: Boolean = false,
    val hideVia: Boolean = false,
    val replyToMessageId: Int? = null,
    /** Epoch milliseconds at which to schedule the send. */
    val scheduleDate: Long? = null,
)

/**
 * Result of `sendInlineBotResult`.
 *
 * The layer answers with an `Updates` bundle rather than the message itself. [message] is the sent
 * message when the bundle named it; otherwise only [ok] is set, and the update stream delivers the
 * message asynchronously.
 */
@Serializable
internal data class SentInlineResult(val ok: Boolean, val message: Message? = null)

/** The identifier of an inline message, as the operations address it. */
@Serializable
internal data class InlineMessageIdPayload(
    val dcId: Int,
    val accessHash: Long,
    val id: Long,
)

/** Payload of `editInlineMessage`. */
@Serializable
internal data class EditInlineMessagePayload(
    val messageId: InlineMessageIdPayload,
    val text: String = "",
    val linkPreview: Boolean = true,
    val invertMedia: Boolean = false,
    val parseMode: String? = null,
    val entities: List<EntitySpec>? = null,
    val markup: MarkupSpec? = null,
    val media: InlineEditMediaSpec? = null,
)

/**
 * The media an inline edit attaches, which is always a URL: an inline message has no local file to
 * upload. [kind] is `photo` or `document` (the default).
 */
@Serializable
internal data class InlineEditMediaSpec(val url: String, val kind: String? = null)

/** Result of `editInlineMessage`; the layer reports whether the edit was accepted. */
@Serializable
internal data class EditedInlineMessage(val edited: Boolean)
