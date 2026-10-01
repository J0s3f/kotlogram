package com.github.badoualy.telegram.api

import java.nio.file.Path

/**
 * How a local file is attached to an outgoing message, which is the ways grammers sends one.
 *
 * [wireName] is what the native bridge sends.
 */
enum class MediaKind(val wireName: String) {
    /** Telegram compresses the image and may convert it; grammers' `photo`. */
    PHOTO("photo"),

    /** Telegram inspects the file and may treat it as video or audio; grammers' `document`. */
    DOCUMENT("document"),

    /** The file is sent verbatim; grammers' `file`, which is `force_file`. */
    FILE("file"),

    /** Telegram streams the video in place, playing it without a download. */
    VIDEO("video"),
}

/**
 * How a caption is parsed into formatting entities.
 *
 * [wireName] is what the native bridge sends; [NONE] leaves the caption as plain text.
 */
enum class CaptionParseMode(val wireName: String?) {
    NONE(null),
    HTML("html"),
    MARKDOWN("markdown"),
}

/**
 * Typed media: sending a photo, document or file with captions, parse modes, spoiler, MIME type,
 * TTL and scheduling, and copying the media of an existing message.
 *
 * The options are the `InputMessage` builders grammers exposes and the [MessagesApi] file senders
 * predate: this domain is where a caller reaches `html`/`markdown` captions, a custom MIME type, a
 * self-destructing media and `copy_media`.
 */
interface MediaApi : BridgeApi {
    /**
     * Uploads and sends a local file as a typed attachment.
     *
     * [ttlSeconds] makes the media self-destruct after that many seconds; [invertMedia] moves the
     * media below the caption; [scheduleDate] is epoch milliseconds and [scheduleOnceOnline] takes
     * precedence over it. [durationSeconds], [width] and [height] describe [MediaKind.VIDEO] and
     * are ignored for the other kinds. [progressHandle] is an optional slot from
     * [FilesApi.uploadProgressBegin] that the upload reports into; when given, poll it from another
     * thread because this call blocks until the send completes.
     */
    fun mediaSend(
        peer: TelegramPeer,
        path: Path,
        kind: MediaKind = MediaKind.DOCUMENT,
        caption: String = "",
        parseMode: CaptionParseMode = CaptionParseMode.NONE,
        spoiler: Boolean = false,
        mimeType: String? = null,
        durationSeconds: Double? = null,
        width: Int? = null,
        height: Int? = null,
        ttlSeconds: Int? = null,
        invertMedia: Boolean = false,
        silent: Boolean = false,
        replyToMsgId: Int? = null,
        scheduleDate: Long? = null,
        scheduleOnceOnline: Boolean = false,
        replyMarkup: ReplyMarkup? = null,
        progressHandle: Long? = null,
    ): Message = bridge.sendMedia(
        peer = peer.native,
        path = path,
        kind = kind.wireName,
        caption = caption,
        parseMode = parseMode.wireName,
        spoiler = spoiler,
        mimeType = mimeType,
        durationSeconds = durationSeconds,
        width = width,
        height = height,
        ttlSeconds = ttlSeconds,
        invertMedia = invertMedia,
        silent = silent,
        replyToMessageId = replyToMsgId,
        scheduleDate = scheduleDate,
        scheduleOnceOnline = scheduleOnceOnline,
        replyMarkup = replyMarkup?.asSpec(),
        progressHandle = progressHandle,
    ).toCompatibility()

    /**
     * Sends a file already uploaded by this client as a typed attachment, referencing it by
     * [UploadedFile.handle] so it is not uploaded a second time. The options are those [mediaSend]
     * takes.
     */
    fun mediaSend(
        peer: TelegramPeer,
        file: UploadedFile,
        kind: MediaKind = MediaKind.DOCUMENT,
        caption: String = "",
        parseMode: CaptionParseMode = CaptionParseMode.NONE,
        spoiler: Boolean = false,
        mimeType: String? = null,
        durationSeconds: Double? = null,
        width: Int? = null,
        height: Int? = null,
        ttlSeconds: Int? = null,
        invertMedia: Boolean = false,
        silent: Boolean = false,
        replyToMsgId: Int? = null,
        scheduleDate: Long? = null,
        scheduleOnceOnline: Boolean = false,
        replyMarkup: ReplyMarkup? = null,
    ): Message = bridge.sendMedia(
        peer = peer.native,
        path = null,
        kind = kind.wireName,
        caption = caption,
        parseMode = parseMode.wireName,
        spoiler = spoiler,
        mimeType = mimeType,
        durationSeconds = durationSeconds,
        width = width,
        height = height,
        ttlSeconds = ttlSeconds,
        invertMedia = invertMedia,
        silent = silent,
        replyToMessageId = replyToMsgId,
        scheduleDate = scheduleDate,
        scheduleOnceOnline = scheduleOnceOnline,
        replyMarkup = replyMarkup?.asSpec(),
        fileHandle = requireNotNull(file.handle) { "the uploaded file carries no handle" },
    ).toCompatibility()

    /**
     * Sends media Telegram downloads from [url], with the same options as [mediaSend].
     *
     * A URL attachment is either a photo or a document; there is no verbatim form of it.
     */
    fun mediaSendUrl(
        peer: TelegramPeer,
        url: String,
        asPhoto: Boolean = false,
        caption: String = "",
        parseMode: CaptionParseMode = CaptionParseMode.NONE,
        spoiler: Boolean = false,
        mimeType: String? = null,
        ttlSeconds: Int? = null,
        invertMedia: Boolean = false,
        silent: Boolean = false,
        replyToMsgId: Int? = null,
        scheduleDate: Long? = null,
        scheduleOnceOnline: Boolean = false,
        replyMarkup: ReplyMarkup? = null,
    ): Message = bridge.sendMediaUrl(
        peer = peer.native,
        url = url,
        kind = if (asPhoto) MediaKind.PHOTO.wireName else MediaKind.DOCUMENT.wireName,
        caption = caption,
        parseMode = parseMode.wireName,
        spoiler = spoiler,
        mimeType = mimeType,
        ttlSeconds = ttlSeconds,
        invertMedia = invertMedia,
        silent = silent,
        replyToMessageId = replyToMsgId,
        scheduleDate = scheduleDate,
        scheduleOnceOnline = scheduleOnceOnline,
        replyMarkup = replyMarkup?.asSpec(),
    ).toCompatibility()

    /**
     * Sends the media of the message [messageId] in [source] to [destination] without re-uploading
     * it, with a new caption.
     */
    fun mediaCopy(
        destination: TelegramPeer,
        messageId: Int,
        source: TelegramPeer,
        caption: String = "",
        parseMode: CaptionParseMode = CaptionParseMode.NONE,
        silent: Boolean = false,
        replyToMsgId: Int? = null,
        replyMarkup: ReplyMarkup? = null,
    ): Message = bridge.copyMedia(
        destination = destination.native,
        source = source.native,
        messageId = messageId,
        caption = caption,
        parseMode = parseMode.wireName,
        silent = silent,
        replyToMessageId = replyToMsgId,
        replyMarkup = replyMarkup?.asSpec(),
    ).toCompatibility()
}
