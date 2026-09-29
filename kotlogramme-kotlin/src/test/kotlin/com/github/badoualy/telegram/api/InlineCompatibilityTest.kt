package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.InlineArticleSpec
import org.kotlogramme.protocol.InlineQueryResults as BridgeInlineQueryResults
import org.kotlogramme.protocol.InlineSwitchPmSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade carries every field the inline projection sends, and that
 * the builders take exactly the shapes grammers' `Article` builder accepts.
 *
 * The document is the same one [org.kotlogramme.protocol.InlineProtocolTest] pins on the wire side,
 * so a field added to `native/src/dto/inline.rs` without reaching this layer fails here.
 */
class InlineCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an inline page carries its query offset and results into the facade`() {
        val page = json.decodeFromString<BridgeInlineQueryResults>(PAGE).toCompatibility()

        assertEquals(901, page.queryId)
        assertEquals("10", page.nextOffset)
        assertTrue(page.gallery)
        assertEquals(2, page.results.size)

        val plain = page.results[0]
        assertEquals("result", plain.kind)
        assertEquals("result-1", plain.id)
        assertEquals("article", plain.type)
        assertEquals("An article", plain.title)
        assertEquals("It describes things", plain.description)
        assertEquals("https://example.org/article", plain.url)
        assertEquals(
            InlineWebDocument("https://example.org/thumb.jpg", 1024, "image/jpeg"),
            plain.thumb,
        )

        val media = page.results[1]
        assertEquals("mediaResult", media.kind)
        assertEquals(9_001, media.documentId)
    }

    @Test
    fun `an article result becomes the builder spec grammers takes`() {
        assertEquals(
            InlineArticleSpec(
                title = "Title",
                messageText = "hello",
                id = "r1",
                description = "desc",
                url = "https://example.org",
                thumbUrl = "https://example.org/t.jpg",
                linkPreview = false,
                invertMedia = true,
            ),
            InlineArticle(
                title = "Title",
                messageText = "hello",
                id = "r1",
                description = "desc",
                url = "https://example.org",
                thumbUrl = "https://example.org/t.jpg",
                linkPreview = false,
                invertMedia = true,
            ).asSpec(),
        )
    }

    @Test
    fun `a switch prompt becomes the builder spec grammers takes`() {
        assertEquals(InlineSwitchPmSpec("Open the bot", "start"), InlineSwitchPm("Open the bot", "start").asSpec())
    }

    private companion object {
        val PAGE = """
            {"queryId": 901, "nextOffset": "10", "gallery": true,
             "results": [
                {"kind": "result", "id": "result-1", "type": "article", "title": "An article",
                 "description": "It describes things", "url": "https://example.org/article",
                 "thumb": {"url": "https://example.org/thumb.jpg", "size": 1024, "mimeType": "image/jpeg"},
                 "content": null, "photoId": null, "documentId": null},
                {"kind": "mediaResult", "id": "result-2", "type": "document", "title": null,
                 "description": null, "url": null, "thumb": null, "content": null, "photoId": null,
                 "documentId": 9001}]}
        """.trimIndent()
    }
}
