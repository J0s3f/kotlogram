package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.Message as BridgeMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade's media vocabulary matches what the native bridge accepts,
 * and that the media options this domain sends survive the projection.
 *
 * The wire names are the contract: `native/src/ops/media.rs` maps each of them onto a grammers
 * builder or a raw `InputMedia`, so a name renamed or invented here would only fail against a live
 * session.
 */
class MediaCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `the media kinds name the wire values the native bridge maps`() {
        assertEquals(
            mapOf(
                "photo" to MediaKind.PHOTO,
                "document" to MediaKind.DOCUMENT,
                "file" to MediaKind.FILE,
            ),
            MediaKind.entries.associateBy { it.wireName },
        )
    }

    @Test
    fun `the caption parse modes name the wire values the native bridge parses`() {
        assertEquals(
            listOf(null, "html", "markdown"),
            CaptionParseMode.entries.map { it.wireName },
        )
    }

    @Test
    fun `a spoiler document keeps its mime and ttl in the compatibility projection`() {
        val message = json.decodeFromString<BridgeMessage>(MESSAGE).toCompatibility()
        val media = message.media

        assertEquals("document", media?.kind)
        assertEquals("report.pdf", media?.name)
        assertEquals("application/pdf", media?.mimeType)
        assertTrue(media?.spoiler == true)
        assertEquals(60, media?.ttlSeconds)
    }

    private companion object {
        val MESSAGE = """
            {"id": 31, "text": "hello", "outgoing": true, "replyToMessageId": null,
             "media": {"kind": "document", "id": 1, "size": 2, "name": "report.pdf",
                       "mimeType": "application/pdf", "spoiler": true, "ttlSeconds": 60}}
        """.trimIndent()
    }
}
