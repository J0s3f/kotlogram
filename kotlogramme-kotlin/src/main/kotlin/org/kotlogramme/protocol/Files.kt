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

/** Payload of `uploadFile`. */
@Serializable
internal data class UploadFilePayload(val path: String)

/** Payload of `iterProfilePhotos`. */
@Serializable
internal data class ProfilePhotosPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val limit: Int = 50,
) {
    constructor(peer: PeerTarget, limit: Int) : this(peer.peerHandle, peer.username, limit)
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
 * the small-file one carries an [md5Checksum].
 */
@Serializable
internal data class UploadedFile(
    val id: Long,
    val name: String,
    val size: Long,
    val parts: Int,
    val md5Checksum: String? = null,
    val isBig: Boolean = false,
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
