package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.DownloadMediaChunkPayload
import org.kotlogramme.protocol.DownloadMediaPayload
import org.kotlogramme.protocol.DownloadResult
import org.kotlogramme.protocol.MediaChunk
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.ProfilePhoto
import org.kotlogramme.protocol.ProfilePhotosPayload
import org.kotlogramme.protocol.UploadBytesPayload
import org.kotlogramme.protocol.UploadedFile
import org.kotlogramme.protocol.UploadFilePayload
import org.kotlogramme.protocol.UploadProgress
import org.kotlogramme.protocol.UploadProgressBeginPayload
import org.kotlogramme.protocol.UploadProgressPayload
import org.kotlogramme.protocol.UploadStreamBeginPayload
import org.kotlogramme.protocol.UploadStreamBeginResult
import org.kotlogramme.protocol.UploadStreamChunkPayload
import org.kotlogramme.protocol.UploadStreamFinishPayload
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

    /**
     * Uploads a local file; the result is the metadata a later send can reuse.
     *
     * When [progressHandle] names a slot from [uploadProgressBegin], the file is uploaded through a
     * counting reader so the slot can be polled while this call runs.
     */
    @Operation("uploadFile")
    fun uploadFile(path: Path, progressHandle: Long? = null): UploadedFile = transport.request(
        "uploadFile",
        UploadFilePayload(path.toAbsolutePath().toString(), progressHandle),
    )

    /**
     * Uploads [dataBase64] under [name] in one call; the result is the metadata a later send
     * reuses. The bytes travel base64-encoded, so this is for small files.
     */
    @Operation("uploadBytes")
    fun uploadBytes(name: String, dataBase64: String): UploadedFile = transport.request(
        "uploadBytes",
        UploadBytesPayload(name, dataBase64),
    )

    /**
     * Opens a chunked upload of [size] bytes under [name] and returns the id its chunks, its finish
     * and its progress poll name.
     */
    @Operation("uploadStreamBegin")
    fun uploadStreamBegin(name: String, size: Long): Long =
        transport.request<UploadStreamBeginPayload, UploadStreamBeginResult>(
            "uploadStreamBegin",
            UploadStreamBeginPayload(name, size),
        ).uploadId

    /** Appends one base64 chunk to the stream [uploadId] names. */
    @Operation("uploadStreamChunk")
    fun uploadStreamChunk(uploadId: Long, dataBase64: String) {
        transport.request<UploadStreamChunkPayload, OperationResult>(
            "uploadStreamChunk",
            UploadStreamChunkPayload(uploadId, dataBase64),
        )
    }

    /** Uploads everything accumulated for [uploadId]; the result is the reusable metadata. */
    @Operation("uploadStreamFinish")
    fun uploadStreamFinish(uploadId: Long): UploadedFile = transport.request(
        "uploadStreamFinish",
        UploadStreamFinishPayload(uploadId),
    )

    /** Allocates a progress slot for an upload whose bytes do not travel through a stream. */
    @Operation("uploadProgressBegin")
    fun uploadProgressBegin(total: Long): Long =
        transport.request<UploadProgressBeginPayload, UploadStreamBeginResult>(
            "uploadProgressBegin",
            UploadProgressBeginPayload(total),
        ).uploadId

    /** Reads the live progress of the upload [uploadId] names. */
    @Operation("uploadProgress")
    fun uploadProgress(uploadId: Long): UploadProgress =
        transport.request<UploadProgressPayload, UploadProgress>(
            "uploadProgress",
            UploadProgressPayload(uploadId),
        )

    /**
     * Lists up to [limit] profile photos of a peer, most recent first, or all of them when [all] is
     * set.
     *
     * [offset] is the paging cursor: the index of the first photo to return, absent is the first
     * page. [all] wins over [limit] and [offset].
     */
    @Operation("iterProfilePhotos")
    fun iterProfilePhotos(peer: Peer, limit: Int, offset: Int? = null, all: Boolean = false): List<ProfilePhoto> =
        transport.request(
            "iterProfilePhotos",
            ProfilePhotosPayload(PeerTarget(peer.nativeHandle), limit, offset, all),
        )
}

/** The [FilesBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class FilesOperations(override val transport: Transport) : FilesBridge
