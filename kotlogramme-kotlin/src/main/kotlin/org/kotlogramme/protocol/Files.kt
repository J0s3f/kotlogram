package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the file-transfer operations. */

/**
 * Payload of `downloadMedia`.
 *
 * The peer fields are flattened into the payload rather than nested, matching the native
 * [PeerTarget] shape.
 */
@Serializable
internal data class DownloadMediaPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val messageId: Int,
    val path: String,
) {
    constructor(peer: PeerTarget, messageId: Int, path: String) :
        this(peer.peerHandle, peer.username, messageId, path)
}

/** Payload of `downloadMediaChunk`; [chunkSize] and [skipChunks] select the range of the file. */
@Serializable
internal data class DownloadMediaChunkPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val messageId: Int,
    val chunkSize: Int,
    val skipChunks: Int,
) {
    constructor(peer: PeerTarget, messageId: Int, chunkSize: Int, skipChunks: Int) :
        this(peer.peerHandle, peer.username, messageId, chunkSize, skipChunks)
}

/** Payload of `uploadFile`. `progressHandle` is an optional `uploadProgressBegin` slot. */
@Serializable
internal data class UploadFilePayload(
    val path: String,
    val progressHandle: Long? = null,
)

/**
 * Payload of `uploadBytes`: the declared name and the whole file base64-encoded.
 *
 * Base64 inflates the bytes by a third and JSON carries them as text, so this is for small files;
 * `FilesApi.uploadStream` is the operation for anything sizeable.
 */
@Serializable
internal data class UploadBytesPayload(
    val name: String,
    val dataBase64: String,
)

/**
 * Payload of `uploadStreamBegin`: the name the finished upload carries and its total size.
 *
 * grammers must be told the size before it reads the first part, so a stream declares it up front;
 * the begin call starts the upload task that reads the chunks as they arrive.
 */
@Serializable
internal data class UploadStreamBeginPayload(
    val name: String,
    val size: Long,
)

/** Result of `uploadStreamBegin`: the id every later chunk and the finish call name. */
@Serializable
internal data class UploadStreamBeginResult(val uploadId: Long)

/**
 * Payload of `uploadProgressBegin`: the total an upload will have. A path upload corrects the total
 * to the file's length when it opens it.
 */
@Serializable
internal data class UploadProgressBeginPayload(val total: Long)

/** Payload of `uploadProgress`: the progress slot to read. */
@Serializable
internal data class UploadProgressPayload(val uploadId: Long)

/**
 * The live progress of an upload.
 *
 * [sent] is how many bytes have been handed to grammers so far and [total] the declared size.
 * [bytesPerSecond] is the average rate since the upload started and [elapsedMillis] how long that
 * is, so a render loop can show a bar and a transfer rate without keeping its own clock.
 */
@Serializable
internal data class UploadProgress(
    val sent: Long,
    val total: Long,
    val elapsedMillis: Long,
    val bytesPerSecond: Double,
)

/** Payload of `uploadStreamChunk`: the stream to append to and one base64 chunk. */
@Serializable
internal data class UploadStreamChunkPayload(
    val uploadId: Long,
    val dataBase64: String,
)

/** Payload of `uploadStreamFinish`: the stream whose accumulated bytes are uploaded. */
@Serializable
internal data class UploadStreamFinishPayload(val uploadId: Long)

/**
 * Payload of `iterProfilePhotos`.
 *
 * [offset] is the paging cursor: the index of the first photo to return. An index, not an id, so
 * a caller can page without any new field on the photo projection. Absent is the first page.
 */
@Serializable
internal data class ProfilePhotosPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val limit: Int = 50,
    val offset: Int? = null,
) {
    constructor(peer: PeerTarget, limit: Int, offset: Int? = null) :
        this(peer.peerHandle, peer.username, limit, offset)
}

/** Result of `downloadMedia`: where the file landed and how many bytes it holds. */
@Serializable
internal data class DownloadResult(
    val path: String,
    val size: Long,
)

/**
 * One chunk of `downloadMediaChunk`.
 *
 * [data] is the chunk bytes base64-encoded, the established convention for binary data on this
 * bridge; [offset] is where in the file the chunk starts and [size] is its decoded length.
 */
@Serializable
internal data class MediaChunk(
    val data: String,
    val offset: Long,
    val size: Long,
)

/**
 * Result of `uploadFile`: the metadata of an upload, which a later send can reuse.
 *
 * [isBig] distinguishes the two TL constructors grammers picks between at ten megabytes, and only
 * the small-file one carries an [md5Checksum]. [handle] is the per-client registry handle a later
 * send references this upload by; it is distinct from [id], the TL file id Telegram assigned, and
 * lives only as long as the client that produced it.
 */
@Serializable
internal data class UploadedFile(
    val id: Long,
    val name: String,
    val size: Long,
    val parts: Int,
    val md5Checksum: String? = null,
    val isBig: Boolean = false,
    val handle: Long? = null,
)

/** One profile photo, the item `iterProfilePhotos` yields. */
@Serializable
internal data class ProfilePhoto(
    val id: Long,
    val dcId: Int? = null,
    val size: Long,
    val width: Int? = null,
    val height: Int? = null,
    val spoiler: Boolean = false,
    val ttlSeconds: Int? = null,
)
