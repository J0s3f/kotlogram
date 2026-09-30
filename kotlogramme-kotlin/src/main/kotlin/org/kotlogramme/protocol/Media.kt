package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/**
 * The media attached to a message.
 *
 * grammers models an attachment as an enum with one variant per attachment type and no shared
 * accessor surface, so the bridge sends one flat object whose [kind] says which of the other
 * fields are populated. Every field is always present in the JSON; the ones that do not apply to
 * a [kind] are `null`.
 */
@Serializable
data class Media(
    /** The grammers media variant in lowerCamelCase, or `unknown` for one this build cannot name. */
    val kind: String,
    val id: Long? = null,
    val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val spoiler: Boolean? = null,
    val ttlSeconds: Int? = null,
    val name: String? = null,
    val mimeType: String? = null,
    val creationDate: Long? = null,
    val duration: Double? = null,
    val resolutionWidth: Int? = null,
    val resolutionHeight: Int? = null,
    val audioTitle: String? = null,
    val performer: String? = null,
    /** The sticker emoji, or the dice emoticon. */
    val emoji: String? = null,
    val isAnimated: Boolean? = null,
    val phoneNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val vcard: String? = null,
    val question: String? = null,
    val isQuiz: Boolean? = null,
    val closed: Boolean? = null,
    val totalVoters: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyRadius: Int? = null,
    val title: String? = null,
    val address: String? = null,
    val provider: String? = null,
    val venueId: String? = null,
    val venueType: String? = null,
    val heading: Int? = null,
    val period: Int? = null,
    val proximityNotificationRadius: Int? = null,
    val value: Int? = null,
    val url: String? = null,
    val displayUrl: String? = null,
    val siteName: String? = null,
    val description: String? = null,
    /** The web page's `type` flag, for example `video` or `article`. */
    val pageType: String? = null,
    val author: String? = null,
)

/**
 * Payload of `sendMedia`.
 *
 * The peer fields are flattened into the payload rather than nested, matching the native
 * [PeerTarget] shape. A field left at its default is omitted, so the native side fills in
 * grammers' own default: `document`, a plain caption, no spoiler, no TTL and no schedule.
 * [kind] is `photo`, `document` or `file`; [parseMode] is `html`, `markdown` or absent.
 *
 * Exactly one of [path] and [fileHandle] is set: a path uploads the local file now, a handle
 * reuses an upload a `uploadBytes` or `uploadStreamFinish` already produced.
 */
@Serializable
internal data class SendMediaPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    /** The local file to upload and attach, when the send names a path rather than a handle. */
    val path: String? = null,
    val kind: String = "document",
    val caption: String = "",
    val parseMode: String? = null,
    val spoiler: Boolean = false,
    val mimeType: String? = null,
    val ttlSeconds: Int? = null,
    val invertMedia: Boolean = false,
    val silent: Boolean = false,
    val replyToMessageId: Int? = null,
    /** Epoch milliseconds at which to schedule the message. */
    val scheduleDate: Long? = null,
    val scheduleOnceOnline: Boolean = false,
    val markup: MarkupSpec? = null,
    /** The handle of an upload that already ran, as an alternative to [path]. */
    val fileHandle: Long? = null,
)

/**
 * Payload of `sendMediaUrl`.
 *
 * As in [SendMediaPayload], but the media is a URL Telegram downloads instead of a local file.
 * [kind] is `photo` or `document`.
 */
@Serializable
internal data class SendMediaUrlPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val url: String,
    val kind: String = "document",
    val caption: String = "",
    val parseMode: String? = null,
    val spoiler: Boolean = false,
    val mimeType: String? = null,
    val ttlSeconds: Int? = null,
    val invertMedia: Boolean = false,
    val silent: Boolean = false,
    val replyToMessageId: Int? = null,
    val scheduleDate: Long? = null,
    val scheduleOnceOnline: Boolean = false,
    val markup: MarkupSpec? = null,
)

/**
 * Payload of `copyMedia`.
 *
 * The media is reused from [source] without a re-upload and sent to [destination]. The source is
 * spelled the way the other two-peer operation, `forwardMessages`, spells its two ends.
 */
@Serializable
internal data class CopyMediaPayload(
    val destination: PeerTarget,
    val source: PeerTarget,
    val messageId: Int,
    val caption: String = "",
    val parseMode: String? = null,
    val silent: Boolean = false,
    val replyToMessageId: Int? = null,
    val markup: MarkupSpec? = null,
)
