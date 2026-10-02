package com.github.badoualy.telegram.api

import java.io.InputStream
import java.nio.file.Path
import java.util.Base64

/** Downloading media, uploading files and listing profile photos. */
interface FilesApi : BridgeApi {
    companion object {
        /** grammers' maximum chunk size in bytes, the default [downloadMediaChunk] reads with. */
        const val DEFAULT_CHUNK_SIZE = 512 * 1024
    }

    /**
     * Downloads the media attached to [messageId] into [path], overwriting it if it exists.
     *
     * The file is the largest thumbnail of a photo, or the document itself. Returns where it landed
     * and how many bytes it holds.
     */
    fun downloadMedia(peer: TelegramPeer, messageId: Int, path: Path): DownloadedMedia =
        bridge.downloadMedia(peer.native, messageId, path).toCompatibility()

    /**
     * Fetches one chunk of the media attached to [messageId], or `null` once the file is exhausted.
     *
     * [chunkSize] is a multiple of 4096 between 4096 and 524288, and [skipChunks] how many chunks to
     * skip before reading, so a caller can seek within a file by asking for a chunk at an offset.
     */
    fun downloadMediaChunk(
        peer: TelegramPeer,
        messageId: Int,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        skipChunks: Int = 0,
    ): MediaChunk? = bridge.downloadMediaChunk(peer.native, messageId, chunkSize, skipChunks)?.toCompatibility()

    /**
     * Uploads a local file; the result is the metadata a later send can reuse.
     *
     * [progressHandle] is an optional slot created by [uploadProgressBegin]. When one is given, the
     * file is uploaded while [uploadProgress] reports its bytes; because this call blocks until the
     * upload finishes, the polling must come from another thread. Without a slot it is grammers'
     * own path upload, unchanged.
     */
    fun uploadFile(path: Path, progressHandle: Long? = null): UploadedFile =
        bridge.uploadFile(path, progressHandle).toCompatibility()

    /**
     * Reserves a progress slot for a path upload, which is how [uploadFile] and a path [MediaApi.mediaSend]
     * can be observed although they run as one blocking call.
     *
     * Pass the returned handle to the upload and poll it with [uploadProgress] from a render thread.
     * [total] is the expected byte count, or zero when it is not known yet; a path upload fills in
     * the file's own length when it opens it.
     */
    fun uploadProgressBegin(total: Long = 0L): Long = bridge.uploadProgressBegin(total)

    /**
     * Reads the live progress of the upload [handle] names, created by [uploadProgressBegin] or by
     * [uploadStream].
     *
     * This is a plain counter read, so it can be called from a render loop while the upload's own
     * call is blocked on another thread.
     */
    fun uploadProgress(handle: Long): UploadProgress = bridge.uploadProgress(handle).toCompatibility()

    /**
     * Uploads [data] under [name] in one call, without touching the filesystem.
     *
     * The bytes cross the bridge base64-encoded, so this suits small files; [uploadStream] is the
     * chunked form for anything sizeable. The result carries the handle a later send references.
     */
    fun uploadBytes(data: ByteArray, name: String): UploadedFile =
        bridge.uploadBytes(name, Base64.getEncoder().encodeToString(data)).toCompatibility()

    /**
     * Uploads [input], which is exactly [size] bytes long, in [chunkSize]-byte chunks without
     * touching the filesystem.
     *
     * The stream is read to its end and each chunk is handed to a native upload that is already
     * running, so the file is never held in memory and only a bounded number of chunks wait for the
     * network; [size] is required because grammers must know the total before it sends the first
     * part. [onProgress], when given, is invoked with a fresh [UploadProgress] after each chunk, so
     * the caller should run this on a background thread and post from the callback to its UI. The
     * result carries the handle a later send references. [input] is closed by this method.
     */
    fun uploadStream(
        input: InputStream,
        name: String,
        size: Long,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        onProgress: ((UploadProgress) -> Unit)? = null,
    ): UploadedFile {
        require(chunkSize > 0) { "chunkSize must be positive" }
        require(size >= 0) { "size must not be negative" }
        val uploadId = bridge.uploadStreamBegin(name, size)
        val buffer = ByteArray(chunkSize)
        var sent = 0L
        input.use {
            while (true) {
                val read = it.read(buffer)
                if (read < 0) break
                if (read > 0) {
                    bridge.uploadStreamChunk(uploadId, Base64.getEncoder().encodeToString(buffer.copyOf(read)))
                    sent += read
                    if (onProgress != null) {
                        onProgress(bridge.uploadProgress(uploadId).toCompatibility())
                    }
                }
            }
        }
        if (sent != size) {
            // Finish the native stream anyway, which aborts the upload task, then report the
            // mismatch the way the native side would have.
            runCatching { bridge.uploadStreamFinish(uploadId) }
            throw IllegalArgumentException("the stream held $sent bytes but $size were declared")
        }
        return bridge.uploadStreamFinish(uploadId).toCompatibility()
    }

    /**
     * Lists up to [limit] profile photos of a peer, most recent first, or all of them when [all] is
     * set.
     *
     * [offset] is the paging cursor: the index of the first photo to return, absent is the first
     * page. [all] wins over [limit] and [offset].
     */
    fun getProfilePhotos(
        peer: TelegramPeer,
        limit: Int = 50,
        offset: Int? = null,
        all: Boolean = false,
    ): List<ProfilePhoto> =
        bridge.iterProfilePhotos(peer.native, limit, offset, all).map { it.toCompatibility() }
}
