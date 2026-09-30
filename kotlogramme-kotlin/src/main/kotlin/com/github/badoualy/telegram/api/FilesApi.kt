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

    /** Uploads a local file; the result is the metadata a later send can reuse. */
    fun uploadFile(path: Path): UploadedFile = bridge.uploadFile(path).toCompatibility()

    /**
     * Uploads [data] under [name] in one call, without touching the filesystem.
     *
     * The bytes cross the bridge base64-encoded, so this suits small files; [uploadStream] is the
     * chunked form for anything sizeable. The result carries the handle a later send references.
     */
    fun uploadBytes(data: ByteArray, name: String): UploadedFile =
        bridge.uploadBytes(name, Base64.getEncoder().encodeToString(data)).toCompatibility()

    /**
     * Uploads [input] in [chunkSize]-byte chunks, without touching the filesystem.
     *
     * The stream is read to its end, each chunk base64-encoded and appended to a native stream
     * whose accumulated bytes the finish call uploads. The result carries the handle a later send
     * references. [input] is closed by this method.
     */
    fun uploadStream(input: InputStream, name: String, chunkSize: Int = DEFAULT_CHUNK_SIZE): UploadedFile {
        require(chunkSize > 0) { "chunkSize must be positive" }
        val uploadId = bridge.uploadStreamBegin(name)
        val buffer = ByteArray(chunkSize)
        input.use {
            while (true) {
                val read = it.read(buffer)
                if (read < 0) break
                if (read > 0) {
                    bridge.uploadStreamChunk(uploadId, Base64.getEncoder().encodeToString(buffer.copyOf(read)))
                }
            }
        }
        return bridge.uploadStreamFinish(uploadId).toCompatibility()
    }

    /** Lists up to [limit] profile photos of a peer, most recent first. */
    fun getProfilePhotos(peer: TelegramPeer, limit: Int = 50): List<ProfilePhoto> =
        bridge.iterProfilePhotos(peer.native, limit).map { it.toCompatibility() }
}
