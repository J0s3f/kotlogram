package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the inline models.
 *
 * The result documents below are exactly what the Rust projection in `native/src/dto/inline.rs`
 * emits, so a field renamed on one side, a result variant dropped, or a web document left out
 * breaks a test here rather than a live session. The Rust side pins the same documents in the
 * `dto::inline` test module.
 */
class InlineProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** The default codec, which is what [Transport] sends payloads with. */
    private val wire = Json { ignoreUnknownKeys = true }

    @Test
    fun `a page decodes and re-encodes unchanged`() {
        val page = roundTrips<InlineQueryResults>(PAGE)
        assertEquals(901, page.queryId)
        assertEquals("10", page.nextOffset)
        assertTrue(page.gallery)
        assertEquals(2, page.results.size)
    }

    @Test
    fun `a page declares exactly the fields the native projection emits`() {
        val names = (json.parseToJsonElement(PAGE) as JsonObject).keys.sorted()
        assertEquals(
            listOf("gallery", "nextOffset", "queryId", "results", "switchPm", "switchWebview"),
            names,
        )
    }

    @Test
    fun `a plain result carries its metadata and its web documents`() {
        val result = roundTrips<InlineResult>(PLAIN)
        assertEquals("result", result.kind)
        assertEquals("result-1", result.id)
        assertEquals("article", result.type)
        assertEquals("An article", result.title)
        assertEquals("It describes things", result.description)
        assertEquals("https://example.org/article", result.url)
        assertEquals(InlineWebDocument("https://example.org/thumb.jpg", 1024, "image/jpeg"), result.thumb)
        assertNull(result.content)
        assertNull(result.photoId)
        assertNull(result.documentId)
        assertEquals("hello", result.sendMessageText)
    }

    @Test
    fun `a media result points at a photo or a document instead`() {
        val photo = roundTrips<InlineResult>(MEDIA_PHOTO)
        assertEquals("mediaResult", photo.kind)
        assertEquals(4_242, photo.photoId)
        assertNull(photo.documentId)
        assertNull(photo.url)
        assertNull(photo.thumb)
        // A media-carrying message posts no text.
        assertNull(photo.sendMessageText)

        val document = roundTrips<InlineResult>(MEDIA_DOCUMENT)
        assertEquals(9_001, document.documentId)
        assertNull(document.photoId)
    }

    @Test
    fun `a page carries the prompts the bot answered with`() {
        val page = roundTrips<InlineQueryResults>(PAGE)
        assertEquals(InlineSwitchPm("Open the bot", "start"), page.switchPm)
        assertEquals(InlineSwitchWebview("Open the page", "https://example.org/page"), page.switchWebview)
    }

    @Test
    fun `the last page of a query carries no offset`() {
        val page = roundTrips<InlineQueryResults>(LAST_PAGE)
        assertNull(page.nextOffset)
        assertTrue(page.results.isEmpty())
    }

    @Test
    fun `an inline query payload nests its two peer selectors`() {
        val payload = InlineQueryPayload(
            PeerTarget(peerHandle = 5),
            "cats",
            PeerTarget(username = "peer"),
            "10",
        )
        assertEquals(
            """{"bot":{"peerHandle":5},"query":"cats","peer":{"username":"peer"},"offset":"10"}""",
            wire.encodeToString(payload),
        )
    }

    @Test
    fun `an inline answer payload spells the private flag as the layer does`() {
        val payload = AnswerInlineQueryPayload(
            queryId = 7,
            results = listOf(InlineResultSpec(kind = "article", title = "Title", messageText = "hello")),
            isPrivate = true,
            nextOffset = "20",
        )
        val encoded = wire.encodeToString(payload)
        assertTrue(encoded.contains("\"private\":true"), encoded)
        assertTrue(encoded.contains("\"nextOffset\":\"20\""), encoded)
        // The article result the answer carries keeps the text message it sends.
        assertTrue(encoded.contains("\"messageText\":\"hello\""), encoded)
    }

    @Test
    fun `a media answer payload carries the kind content url and caption`() {
        val payload = AnswerInlineQueryPayload(
            queryId = 7,
            results = listOf(
                InlineResultSpec(
                    kind = "video",
                    contentUrl = "https://example.org/movie.mp4",
                    thumbUrl = "https://example.org/thumb.jpg",
                    caption = "a movie",
                ),
            ),
            switchWebview = InlineSwitchWebview("Open the page", "https://example.org/page"),
        )
        val encoded = wire.encodeToString(payload)
        assertTrue(encoded.contains("\"kind\":\"video\""), encoded)
        assertTrue(encoded.contains("\"contentUrl\":\"https://example.org/movie.mp4\""), encoded)
        assertTrue(encoded.contains("\"caption\":\"a movie\""), encoded)
        assertTrue(encoded.contains("\"switchWebview\":{\"text\":\"Open the page\""), encoded)
    }

    @Test
    fun `a send inline result payload nests its peer and options`() {
        val payload = SendInlineBotResultPayload(
            peer = PeerTarget(peerHandle = 5),
            queryId = 900,
            resultId = "result-1",
            silent = true,
            hideVia = true,
            replyToMessageId = 42,
            scheduleDate = 1_700_000_000_000,
        )
        assertEquals(
            """{"peer":{"peerHandle":5},"queryId":900,"resultId":"result-1","silent":true,""" +
                """"hideVia":true,"replyToMessageId":42,"scheduleDate":1700000000000}""",
            wire.encodeToString(payload),
        )
    }

    @Test
    fun `a sent inline result carries the message only when the native side identified it`() {
        val identified = json.decodeFromString<SentInlineResult>(
            """{"ok": true, "message": null}""",
        )
        assertTrue(identified.ok)
        assertNull(identified.message)
    }

    @Test
    fun `an edit payload nests the inline message id`() {
        val payload = EditInlineMessagePayload(
            InlineMessageIdPayload(dcId = 2, accessHash = 77, id = 88),
            "new text",
        )
        assertEquals(
            """{"messageId":{"dcId":2,"accessHash":77,"id":88},"text":"new text"}""",
            wire.encodeToString(payload),
        )
    }

    @Test
    fun `an edited inline message reports whether the layer accepted it`() {
        assertTrue(json.decodeFromString<EditedInlineMessage>("""{"edited": true}""").edited)
        assertTrue(!json.decodeFromString<EditedInlineMessage>("""{"edited": false}""").edited)
    }

    @Test
    fun `a rich inline edit payload nests its media, entities and markup`() {
        val payload = EditInlineMessagePayload(
            messageId = InlineMessageIdPayload(dcId = 2, accessHash = 77, id = 88),
            text = "<b>hi</b>",
            linkPreview = false,
            invertMedia = true,
            parseMode = "html",
            entities = listOf(EntitySpec(offset = 0, length = 2, type = "bold")),
            markup = MarkupSpec.Hide(selective = true),
            media = InlineEditMediaSpec(url = "https://example.org/movie.mp4", kind = "document"),
        )

        assertEquals(
            """{"messageId":{"dcId":2,"accessHash":77,"id":88},"text":"<b>hi</b>",""" +
                """"linkPreview":false,"invertMedia":true,"parseMode":"html",""" +
                """"entities":[{"offset":0,"length":2,"type":"bold"}],""" +
                """"markup":{"kind":"hide","selective":true},""" +
                """"media":{"url":"https://example.org/movie.mp4","kind":"document"}}""",
            wire.encodeToString(payload),
        )
    }

    @Test
    fun `a bare inline edit leaves the rich options out`() {
        val payload = EditInlineMessagePayload(
            InlineMessageIdPayload(dcId = 2, accessHash = 77, id = 88),
            "new text",
        )

        assertEquals(
            """{"messageId":{"dcId":2,"accessHash":77,"id":88},"text":"new text"}""",
            wire.encodeToString(payload),
        )
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
        val PLAIN = """
            {"kind": "result", "id": "result-1", "type": "article", "title": "An article",
             "description": "It describes things", "url": "https://example.org/article",
             "thumb": {"url": "https://example.org/thumb.jpg", "size": 1024, "mimeType": "image/jpeg"},
             "content": null, "photoId": null, "documentId": null, "sendMessageText": "hello"}
        """.trimIndent()

        val MEDIA_PHOTO = """
            {"kind": "mediaResult", "id": "result-3", "type": "photo", "title": "A photo",
             "description": null, "url": null, "thumb": null, "content": null, "photoId": 4242,
             "documentId": null, "sendMessageText": null}
        """.trimIndent()

        val MEDIA_DOCUMENT = """
            {"kind": "mediaResult", "id": "result-2", "type": "document", "title": null,
             "description": null, "url": null, "thumb": null, "content": null, "photoId": null,
             "documentId": 9001, "sendMessageText": "hello"}
        """.trimIndent()

        val LAST_PAGE =
            """{"queryId": 2, "nextOffset": null, "gallery": false, "switchPm": null, """ +
                """"switchWebview": null, "results": []}"""

        /** The page the Rust `a_page_carries_its_query_id_next_offset_and_results` test pins. */
        val PAGE = """
            {"queryId": 901, "nextOffset": "10", "gallery": true,
             "switchPm": {"text": "Open the bot", "startParam": "start"},
             "switchWebview": {"text": "Open the page", "url": "https://example.org/page"},
             "results": [${PLAIN.trimIndent()}, ${MEDIA_DOCUMENT.trimIndent()}]}
        """.trimIndent()
    }
}
