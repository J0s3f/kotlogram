package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.DownloadMediaChunkPayload
import org.kotlogramme.protocol.DownloadMediaPayload
import org.kotlogramme.protocol.DownloadResult
import org.kotlogramme.protocol.MediaChunk
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.ProfilePhoto
import org.kotlogramme.protocol.ProfilePhotosPayload
import org.kotlogramme.protocol.UploadedFile
import org.kotlogramme.protocol.UploadFilePayload
import java.nio.file.Path

/**
 * File transfer: downloading media, chunked downloads and uploads with declared MIME types, plus
 * profile photos.
 */
internal interface FilesBridge {
    val transport: Transport

    /** Downloads the media of [messageId] to [path]; the file is overwritten if it already exists. */
    @Operation("downloadMedia")
    fun downloadMedia(peer: Peer, messageId: Int, path: Path): DownloadResult = transport.request(
        "downloadMedia",
        DownloadMediaPayload(PeerTarget(peer.nativeHandle), messageId, path.toAbsolutePath().toString()),
    )

    /**
     * Fetches one chunk of the media of [messageId], or `null` once the file is exhausted.
     *
     * [chunkSize] must be a multiple of 4096 between 4096 and 524288, and [skipChunks] how many
     * chunks to skip before reading, which is how a caller seeks within the file.
     */
    @Operation("downloadMediaChunk")
    fun downloadMediaChunk(peer: Peer, messageId: Int, chunkSize: Int, skipChunks: Int): MediaChunk? =
        transport.request<DownloadMediaChunkPayload, MediaChunk?>(
            "downloadMediaChunk",
            DownloadMediaChunkPayload(PeerTarget(peer.nativeHandle), messageId, chunkSize, skipChunks),
        )

    /** Uploads a local file; the result is the metadata a later send can reuse. */
    @Operation("uploadFile")
    fun uploadFile(path: Path): UploadedFile = transport.request(
        "uploadFile",
        UploadFilePayload(path.toAbsolutePath().toString()),
    )

    /** Lists up to [limit] profile photos of a peer, most recent first. */
    @Operation("iterProfilePhotos")
    fun iterProfilePhotos(peer: Peer, limit: Int): List<ProfilePhoto> = transport.request(
        "iterProfilePhotos",
        ProfilePhotosPayload(PeerTarget(peer.nativeHandle), limit),
    )
}

/** The [FilesBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class FilesOperations(override val transport: Transport) : FilesBridge
