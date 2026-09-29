package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.CopyMediaPayload
import org.kotlogramme.protocol.Message
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.SendMediaPayload
import org.kotlogramme.protocol.SendMediaUrlPayload
import java.nio.file.Path

/**
 * Typed media: sending a photo, document or file with a caption, a parse mode, a MIME type, a
 * time-to-live and scheduling, and copying the media of an existing message.
 *
 * These wrap the grammers `InputMessage` builders the message operations do not expose: `photo`,
 * `document`, `file`, `photo_url`, `document_url`, `copy_media` and the `html`/`markdown` caption
 * parsers. Sending a file is a low-level upload, which is why this domain owns the options rather
 * than the `files` domain, whose subject is transfer.
 */
internal interface MediaBridge {
    val transport: Transport

    /**
     * Uploads [path] and sends it as a typed attachment.
     *
     * [kind] is one of `document` (the default), `photo` or `file`; [parseMode] is `html`,
     * `markdown` or absent for a plain caption; [ttlSeconds] makes the media self-destruct;
     * [mimeType] overrides the type inferred from the extension. [scheduleOnceOnline] takes
     * precedence over [scheduleDate].
     */
    @Operation("sendMedia")
    fun sendMedia(
        peer: Peer,
        path: Path,
        kind: String = "document",
        caption: String = "",
        parseMode: String? = null,
        spoiler: Boolean = false,
        mimeType: String? = null,
        ttlSeconds: Int? = null,
        invertMedia: Boolean = false,
        silent: Boolean = false,
        replyToMessageId: Int? = null,
        scheduleDate: Long? = null,
        scheduleOnceOnline: Boolean = false,
    ): Message = transport.request(
        "sendMedia",
        SendMediaPayload(
            peerHandle = peer.nativeHandle,
            path = path.toAbsolutePath().toString(),
            kind = kind,
            caption = caption,
            parseMode = parseMode,
            spoiler = spoiler,
            mimeType = mimeType,
            ttlSeconds = ttlSeconds,
            invertMedia = invertMedia,
            silent = silent,
            replyToMessageId = replyToMessageId,
            scheduleDate = scheduleDate,
            scheduleOnceOnline = scheduleOnceOnline,
        ),
    )

    /** Sends media Telegram downloads from [url] with the same options as [sendMedia]. */
    @Operation("sendMediaUrl")
    fun sendMediaUrl(
        peer: Peer,
        url: String,
        kind: String = "document",
        caption: String = "",
        parseMode: String? = null,
        spoiler: Boolean = false,
        mimeType: String? = null,
        ttlSeconds: Int? = null,
        invertMedia: Boolean = false,
        silent: Boolean = false,
        replyToMessageId: Int? = null,
        scheduleDate: Long? = null,
        scheduleOnceOnline: Boolean = false,
    ): Message = transport.request(
        "sendMediaUrl",
        SendMediaUrlPayload(
            peerHandle = peer.nativeHandle,
            url = url,
            kind = kind,
            caption = caption,
            parseMode = parseMode,
            spoiler = spoiler,
            mimeType = mimeType,
            ttlSeconds = ttlSeconds,
            invertMedia = invertMedia,
            silent = silent,
            replyToMessageId = replyToMessageId,
            scheduleDate = scheduleDate,
            scheduleOnceOnline = scheduleOnceOnline,
        ),
    )

    /**
     * Sends the media of the message [messageId] in [source] to [destination] without re-uploading
     * it, with a new caption.
     */
    @Operation("copyMedia")
    fun copyMedia(
        destination: Peer,
        source: Peer,
        messageId: Int,
        caption: String = "",
        parseMode: String? = null,
        silent: Boolean = false,
        replyToMessageId: Int? = null,
    ): Message = transport.request(
        "copyMedia",
        CopyMediaPayload(
            PeerTarget(destination.nativeHandle),
            PeerTarget(source.nativeHandle),
            messageId,
            caption,
            parseMode,
            silent,
            replyToMessageId,
        ),
    )
}

/** The [MediaBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class MediaOperations(override val transport: Transport) : MediaBridge
