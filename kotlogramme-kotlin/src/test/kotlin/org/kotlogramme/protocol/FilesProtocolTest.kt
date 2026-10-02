package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the file-transfer models and payloads.
 *
 * Each document below is exactly what the Rust projections in `native/src/dto/files.rs` emit, so a
 * field renamed on one side, a size turned from bytes into kilobytes, or a payload key spelled
 * differently breaks a test here rather than a live session. The Rust side pins the same documents
 * in its own `dto::files` and `ops::files` test modules.
 */
class FilesProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** The transport's own codec, which leaves a field at its default out of a request payload. */
    private val requests = Json { ignoreUnknownKeys = true }

    @Test
    fun `a download result decodes and re-encodes unchanged`() {
        val result = roundTrips<DownloadResult>(DOWNLOAD_RESULT)

        assertEquals("/tmp/holidays.jpg", result.path)
        assertEquals(5_150, result.size)
    }

    @Test
    fun `a chunk carries base64 bytes, an offset and a decoded size`() {
        val chunk = roundTrips<MediaChunk>(CHUNK)

        assertEquals(524_288, chunk.offset)
        assertEquals(6, chunk.size)
        assertEquals("Zm9vYmFy", chunk.data)
        // The bytes travel base64-encoded; six of them are exactly the decoded length.
        assertContentEquals("foobar".toByteArray(), Base64.getDecoder().decode(chunk.data))
    }

    @Test
    fun `an exhausted download answers a bare null, which decodes to no chunk`() {
        // The native side answers `null` rather than a document once the iterator is done, so the
        // Kotlin call site asks for a nullable result type.
        assertNull(json.decodeFromString<MediaChunk?>("null"))
    }

    @Test
    fun `an uploaded small file carries its checksum and registry handle`() {
        val file = roundTrips<UploadedFile>(UPLOADED_SMALL)

        assertEquals(7, file.id)
        assertEquals("holidays.jpg", file.name)
        assertEquals(1_048_576, file.size)
        assertEquals(2, file.parts)
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", file.md5Checksum)
        assertEquals(false, file.isBig)
        assertEquals(7, file.handle)
    }

    @Test
    fun `an uploaded big file has no checksum`() {
        val file = roundTrips<UploadedFile>(UPLOADED_BIG)

        assertEquals(8, file.id)
        assertEquals(20, file.parts)
        assertNull(file.md5Checksum)
        assertEquals(true, file.isBig)
        assertNull(file.handle)
    }

    @Test
    fun `a profile photo decodes its location and dimensions`() {
        val photo = roundTrips<ProfilePhoto>(PROFILE_PHOTO)

        assertEquals(4_242, photo.id)
        assertEquals(2, photo.dcId)
        assertEquals(1_024, photo.size)
        assertEquals(640, photo.width)
        assertEquals(480, photo.height)
        assertEquals(false, photo.spoiler)
        assertNull(photo.ttlSeconds)
    }

    @Test
    fun `a profile photo without a location or a resolution keeps both null`() {
        val photo = roundTrips<ProfilePhoto>(PROFILE_PHOTO_EMPTY)

        assertNull(photo.dcId)
        assertNull(photo.width)
        assertNull(photo.height)
        assertEquals(0, photo.size)
    }

    @Test
    fun `the download payload names the peer, the message and the path`() {
        assertEquals(
            """{"peerHandle":12,"messageId":31,"path":"/tmp/a.jpg"}""",
            requests.encodeToString(
                DownloadMediaPayload(PeerTarget(peerHandle = 12L), messageId = 31, path = "/tmp/a.jpg"),
            ),
        )
        assertEquals(
            """{"username":"channel","messageId":31,"path":"/tmp/a.jpg"}""",
            requests.encodeToString(
                DownloadMediaPayload(PeerTarget(username = "channel"), messageId = 31, path = "/tmp/a.jpg"),
            ),
        )
    }

    @Test
    fun `the chunk payload carries the range it reads`() {
        assertEquals(
            """{"peerHandle":12,"messageId":31,"chunkSize":524288,"skipChunks":2}""",
            requests.encodeToString(
                DownloadMediaChunkPayload(
                    PeerTarget(peerHandle = 12L),
                    messageId = 31,
                    chunkSize = 524_288,
                    skipChunks = 2,
                ),
            ),
        )
    }

    @Test
    fun `the upload payload is just the path and an optional progress slot`() {
        assertEquals("""{"path":"/tmp/a.jpg"}""", requests.encodeToString(UploadFilePayload("/tmp/a.jpg")))
        assertEquals(
            """{"path":"/tmp/a.jpg","progressHandle":12}""",
            requests.encodeToString(UploadFilePayload("/tmp/a.jpg", progressHandle = 12)),
        )
    }

    @Test
    fun `the in-memory upload payload carries the name and the base64 data`() {
        assertEquals(
            """{"name":"holidays.jpg","dataBase64":"Zm9vYmFy"}""",
            requests.encodeToString(UploadBytesPayload("holidays.jpg", "Zm9vYmFy")),
        )
    }

    @Test
    fun `the streamed upload payloads carry the id, the size and each chunk`() {
        assertEquals(
            """{"name":"movie.mp4","size":1048576}""",
            requests.encodeToString(UploadStreamBeginPayload("movie.mp4", 1_048_576)),
        )
        assertEquals(
            """{"uploadId":12,"dataBase64":"Zm9v"}""",
            requests.encodeToString(UploadStreamChunkPayload(12, "Zm9v")),
        )
        assertEquals(
            """{"uploadId":12}""",
            requests.encodeToString(UploadStreamFinishPayload(12)),
        )
        assertEquals(12, json.decodeFromString<UploadStreamBeginResult>("""{"uploadId":12}""").uploadId)
    }

    @Test
    fun `the progress payloads carry the slot and the total`() {
        assertEquals(
            """{"total":2048}""",
            requests.encodeToString(UploadProgressBeginPayload(2_048)),
        )
        assertEquals(
            """{"uploadId":12}""",
            requests.encodeToString(UploadProgressPayload(12)),
        )
    }

    @Test
    fun `an upload progress report decodes and re-encodes unchanged`() {
        val progress = roundTrips<UploadProgress>(PROGRESS)

        assertEquals(512, progress.sent)
        assertEquals(1_024, progress.total)
        assertEquals(1_500, progress.elapsedMillis)
        assertEquals(341.33, progress.bytesPerSecond, 0.001)
    }

    @Test
    fun `the profile-photos payload carries the peer and the limit`() {
        assertEquals(
            """{"peerHandle":12,"limit":10}""",
            requests.encodeToString(ProfilePhotosPayload(PeerTarget(peerHandle = 12L), limit = 10)),
        )
        // A limit at its default is left out, and the native side fills in the same 50.
        assertEquals(
            """{"username":"channel"}""",
            requests.encodeToString(ProfilePhotosPayload(PeerTarget(username = "channel"), limit = 50)),
        )
    }

    @Test
    fun `the profile-photos payload carries the index cursor`() {
        // Without a cursor the payload leaves it out, exactly as before.
        assertEquals(
            """{"peerHandle":12,"limit":10}""",
            requests.encodeToString(ProfilePhotosPayload(PeerTarget(peerHandle = 12L), limit = 10)),
        )
        // The cursor is an index into the photo history.
        assertEquals(
            """{"peerHandle":12,"limit":10,"offset":100}""",
            requests.encodeToString(ProfilePhotosPayload(PeerTarget(peerHandle = 12L), limit = 10, offset = 100)),
        )
        val payload = requests.decodeFromString<ProfilePhotosPayload>(
            """{"peerHandle":12,"limit":10,"offset":100}""",
        )
        assertEquals(100, payload.offset)
    }

    @Test
    fun `the profile-photos payload carries the all flag`() {
        // Without `all` the payload is exactly what it always was.
        assertEquals(
            """{"peerHandle":12,"limit":10}""",
            requests.encodeToString(ProfilePhotosPayload(PeerTarget(peerHandle = 12L), limit = 10)),
        )
        // `all` travels as the wire's `all` and wins over the limit and cursor.
        assertEquals(
            """{"peerHandle":12,"limit":10,"all":true}""",
            requests.encodeToString(ProfilePhotosPayload(PeerTarget(peerHandle = 12L), limit = 10, all = true)),
        )
        val payload = requests.decodeFromString<ProfilePhotosPayload>(
            """{"peerHandle":12,"limit":10,"all":true}""",
        )
        assertTrue(payload.all)
    }

    /** Decodes a native document and asserts that re-encoding it reproduces the same document. */
    private inline fun <reified T> roundTrips(document: String): T {
        val decoded = json.decodeFromString<T>(document)
        assertEquals(
            json.parseToJsonElement(document),
            json.parseToJsonElement(json.encodeToString(decoded)),
            "the projection changed the document on the way back",
        )
        return decoded
    }

    private companion object {
        val DOWNLOAD_RESULT = """{"path": "/tmp/holidays.jpg", "size": 5150}"""

        val CHUNK = """{"data": "Zm9vYmFy", "offset": 524288, "size": 6}"""

        val PROGRESS = """
            {"sent": 512, "total": 1024, "elapsedMillis": 1500, "bytesPerSecond": 341.33}
        """.trimIndent()

        val UPLOADED_SMALL = """
            {"id": 7, "name": "holidays.jpg", "size": 1048576, "parts": 2,
             "md5Checksum": "d41d8cd98f00b204e9800998ecf8427e", "isBig": false, "handle": 7}
        """.trimIndent()

        val UPLOADED_BIG = """
            {"id": 8, "name": "movie.mp4", "size": 10485760, "parts": 20,
             "md5Checksum": null, "isBig": true, "handle": null}
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
