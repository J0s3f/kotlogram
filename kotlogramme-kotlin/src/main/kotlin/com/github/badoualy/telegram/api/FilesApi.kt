package com.github.badoualy.telegram.api

import java.nio.file.Path

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

    /** Lists up to [limit] profile photos of a peer, most recent first. */
    fun getProfilePhotos(peer: TelegramPeer, limit: Int = 50): List<ProfilePhoto> =
        bridge.iterProfilePhotos(peer.native, limit).map { it.toCompatibility() }
}
