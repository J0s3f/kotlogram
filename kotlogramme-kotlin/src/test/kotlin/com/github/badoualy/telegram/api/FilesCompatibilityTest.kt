package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.DownloadResult as BridgeDownloadResult
import org.kotlogramme.protocol.MediaChunk as BridgeMediaChunk
import org.kotlogramme.protocol.ProfilePhoto as BridgeProfilePhoto
import org.kotlogramme.protocol.UploadedFile as BridgeUploadedFile
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests that the compatibility facade carries the file-transfer projection, not just the wire
 * shape.
 *
 * The documents are the same ones `FilesProtocolTest` pins on the wire side, so a field added to
 * `native/src/dto/files.rs` without reaching this layer fails here.
 */
class FilesCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a download result projects the path and size`() {
        val result = json.decodeFromString<BridgeDownloadResult>(DOWNLOAD_RESULT).toCompatibility()

        assertEquals("/tmp/holidays.jpg", result.path)
        assertEquals(5_150, result.size)
    }

    @Test
    fun `a chunk decodes its base64 payload into bytes`() {
        val chunk = json.decodeFromString<BridgeMediaChunk>(CHUNK).toCompatibility()

        assertEquals(524_288, chunk.offset)
        assertEquals(6, chunk.size)
        assertContentEquals("foobar".toByteArray(), chunk.data)
        // The decoded bytes are the ones the base64 string stands for.
        assertContentEquals(Base64.getDecoder().decode("Zm9vYmFy"), chunk.data)
    }

    @Test
    fun `an uploaded file keeps its metadata`() {
        val small = json.decodeFromString<BridgeUploadedFile>(UPLOADED_SMALL).toCompatibility()

        assertEquals(7, small.id)
        assertEquals("holidays.jpg", small.name)
        assertEquals(1_048_576, small.size)
        assertEquals(2, small.parts)
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", small.md5Checksum)
        assertEquals(false, small.isBig)

        val big = json.decodeFromString<BridgeUploadedFile>(UPLOADED_BIG).toCompatibility()

        assertEquals(8, big.id)
        assertEquals(20, big.parts)
        assertNull(big.md5Checksum)
        assertEquals(true, big.isBig)
    }

    @Test
    fun `a profile photo keeps its location and dimensions`() {
        val photo = json.decodeFromString<BridgeProfilePhoto>(PROFILE_PHOTO).toCompatibility()

        assertEquals(4_242, photo.id)
        assertEquals(2, photo.dcId)
        assertEquals(1_024, photo.size)
        assertEquals(640, photo.width)
        assertEquals(480, photo.height)
        assertEquals(false, photo.spoiler)
        assertNull(photo.ttlSeconds)

        val empty = json.decodeFromString<BridgeProfilePhoto>(PROFILE_PHOTO_EMPTY).toCompatibility()

        assertNull(empty.dcId)
        assertNull(empty.width)
        assertNull(empty.height)
    }

    private companion object {
        val DOWNLOAD_RESULT = """{"path": "/tmp/holidays.jpg", "size": 5150}"""

        val CHUNK = """{"data": "Zm9vYmFy", "offset": 524288, "size": 6}"""

        val UPLOADED_SMALL = """
            {"id": 7, "name": "holidays.jpg", "size": 1048576, "parts": 2,
             "md5Checksum": "d41d8cd98f00b204e9800998ecf8427e", "isBig": false}
        """.trimIndent()

        val UPLOADED_BIG = """
            {"id": 8, "name": "movie.mp4", "size": 10485760, "parts": 20,
             "md5Checksum": null, "isBig": true}
        """.trimIndent()

        val PROFILE_PHOTO = """
            {"id": 4242, "dcId": 2, "size": 1024, "width": 640, "height": 480, "spoiler": false,
             "ttlSeconds": null}
        """.trimIndent()

        val PROFILE_PHOTO_EMPTY = """
            {"id": 4243, "dcId": null, "size": 0, "width": null, "height": null, "spoiler": false,
             "ttlSeconds": null}
        """.trimIndent()
    }
}
