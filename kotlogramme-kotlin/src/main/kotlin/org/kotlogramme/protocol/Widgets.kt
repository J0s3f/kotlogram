package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/**
 * Payloads, results and update projections of the layer-233 additions: the forum sidebar,
 * audio-file transcription and widgets. See `docs/layer-233-facade.md`.
 */

// ---- forum sidebar --------------------------------------------------------------------------

/** Payload of `toggleForum`. [sidebar] is new in layer 233. */
@Serializable
internal data class ToggleForumPayload(
    val channel: PeerTarget,
    val enabled: Boolean,
    /** Show the topics as tabs at the top of the chat. */
    val tabs: Boolean = false,
    /** Show the topics in a sidebar. */
    val sidebar: Boolean = false,
)

// ---- transcription --------------------------------------------------------------------------

/** Payload of `transcribeAudioFile`: the handle of a finished upload. */
@Serializable
internal data class TranscribeAudioFilePayload(val uploadHandle: Long)

/** Payload of `getTranscriptionLangs`: the peer to ask for, or none for the account. */
@Serializable
internal data class GetTranscriptionLangsPayload(val peer: PeerTarget? = null)

/**
 * A transcription of an audio file.
 *
 * [pending] means [text] is partial; the final text arrives as an `audioTranscribed` update with
 * the same [transcriptionId]. The trial allowance is only present for accounts without Premium, and
 * [trialRemainingUntilDate] is epoch milliseconds.
 */
@Serializable
data class AudioTranscription(
    val transcriptionId: Long,
    val text: String,
    val pending: Boolean,
    val trialRemaining: Int? = null,
    val trialRemainingUntilDate: Long? = null,
)

// ---- widgets --------------------------------------------------------------------------------

/**
 * The HTML of a new widget and how it is shown.
 *
 * [publicData] is readable by everyone who sees the message, [chatData] is kept per chat.
 * [allowQueries] lets the widget ask its bot questions and [sharedWidget] lets other messages reuse
 * it by id. [width] and [height] must be positive and [html] must not be empty.
 */
@Serializable
data class WidgetSpec(
    val html: String,
    val width: Int,
    val height: Int,
    val allowQueries: Boolean = false,
    val sharedWidget: Boolean = false,
    val publicData: String? = null,
    val chatData: String? = null,
)

/** Payload of `sendWidget`: a new [widget] or an existing one by [widgetId], never both. */
@Serializable
internal data class SendWidgetPayload(
    val peer: PeerTarget,
    val widget: WidgetSpec? = null,
    val widgetId: Long? = null,
    val chatData: String? = null,
    val caption: String = "",
    val replyToMessageId: Int? = null,
    val silent: Boolean = false,
    /** Epoch milliseconds at which to schedule the send. */
    val scheduleDate: Long? = null,
)

/**
 * Result of `sendWidget`.
 *
 * [message] is the sent message when the layer's answer named it; otherwise only [ok] is set and the
 * update stream delivers the message asynchronously.
 */
@Serializable
internal data class SentWidget(val ok: Boolean, val message: Message? = null)

/** Payload of `getWidgetPublicData`; [hash] is the cached copy's hash, zero for none. */
@Serializable
internal data class GetWidgetPublicDataPayload(
    val peer: PeerTarget,
    val messageId: Int,
    val hash: Long = 0,
)

/**
 * A widget's public data. When [notModified] the caller's copy of hash [hash] is current and [data]
 * is null; [periodSeconds] is how long the answer may be reused.
 */
@Serializable
data class WidgetPublicData(
    val notModified: Boolean,
    val hash: Long? = null,
    val data: String? = null,
    val periodSeconds: Int,
)

/** Payload of `editMessageWidgetData`; a field left null is not touched. */
@Serializable
internal data class EditMessageWidgetDataPayload(
    val peer: PeerTarget,
    val messageId: Int,
    val publicData: String? = null,
    val chatData: String? = null,
)

/** Payload of `editInlineMessageWidgetData`; a field left null is not touched. */
@Serializable
internal data class EditInlineMessageWidgetDataPayload(
    val messageId: InlineMessageIdPayload,
    val publicData: String? = null,
    val chatData: String? = null,
)

/** Payload of `editWidgetData`: replaces the public data of every message that shows the widget. */
@Serializable
internal data class EditWidgetDataPayload(
    val widgetId: Long,
    val publicData: String,
    val cacheTimeSeconds: Int? = null,
)

/** Payload of `getBotWidgetQueryAnswer`; [dataBase64] is the query data. */
@Serializable
internal data class GetBotWidgetQueryAnswerPayload(
    val peer: PeerTarget,
    val messageId: Int,
    val dataBase64: String,
    val withWallet: Boolean = false,
)

/** The bot's answer to a widget query, null while it has not answered. */
@Serializable
internal data class BotWidgetQueryAnswer(val result: String? = null)

/** Payload of `setBotWidgetQueryAnswer`. */
@Serializable
internal data class SetBotWidgetQueryAnswerPayload(val queryId: Long, val result: String? = null)

// ---- updates --------------------------------------------------------------------------------

/**
 * `forumTopicEdited`: a topic changed. Only the fields that changed are set. [peerKind] is `user`,
 * `chat` or `channel`.
 */
@Serializable
data class ForumTopicEditUpdate(
    val peerId: Long,
    val peerKind: String,
    val topicId: Int,
    val title: String? = null,
    val iconEmojiId: Long? = null,
    val closed: Boolean? = null,
    val hidden: Boolean? = null,
)

/**
 * `botWidgetQuery` or `inlineBotWidgetQuery`: a widget asked its bot a question. A message query
 * names [peerId], [peerKind] and [messageId]; an inline one names [inlineMessageId].
 *
 * [queryType] is `data`, `signData` or `sentTransaction`; the wallet payload of the last two is not
 * projected and stays in the update's raw bytes.
 */
@Serializable
data class WidgetQueryUpdate(
    val queryId: Long,
    val userId: Long,
    val queryType: String,
    val peerId: Long? = null,
    val peerKind: String? = null,
    val messageId: Int? = null,
    val inlineMessageId: InlineMessageId? = null,
    val chatInstance: Long,
    val chatType: String,
    val dataBase64: String,
    val widgetId: Long? = null,
)

/** `messageWidgetData`: the data of a widget message changed. */
@Serializable
data class WidgetDataUpdate(
    val peerId: Long,
    val peerKind: String,
    val messageId: Int,
    val publicData: String? = null,
    val publicDataHash: Long? = null,
    val chatData: String? = null,
)

/** `audioTranscribed`: a transcription progressed or finished. */
@Serializable
data class TranscriptionUpdate(
    val transcriptionId: Long,
    val text: String,
    val pending: Boolean,
)
