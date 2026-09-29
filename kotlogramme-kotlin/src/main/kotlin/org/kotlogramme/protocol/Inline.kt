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
 * [gallery] asks the client to show the results as a grid rather than a list.
 */
@Serializable
data class InlineQueryResults(
    val queryId: Long,
    val nextOffset: String? = null,
    val gallery: Boolean = false,
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
 * [type] is the layer's result type, for example `article`.
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
internal data class InlineSwitchPmSpec(val text: String, val startParam: String)

/** Payload of `answerInlineQuery`. */
@Serializable
internal data class AnswerInlineQueryPayload(
    val queryId: Long,
    val results: List<InlineArticleSpec>,
    val cacheTimeSeconds: Int = 0,
    val gallery: Boolean = false,
    /** grammers' `Answer::private`, which caches the results on the user's client. */
    @SerialName("private") val isPrivate: Boolean = false,
    /** The offset the client sends back to ask for the next page. */
    val nextOffset: String? = null,
    val switchPm: InlineSwitchPmSpec? = null,
)

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
    val text: String,
    val linkPreview: Boolean = true,
    val invertMedia: Boolean = false,
)

/** Result of `editInlineMessage`; the layer reports whether the edit was accepted. */
@Serializable
internal data class EditedInlineMessage(val edited: Boolean)
