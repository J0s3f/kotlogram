package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Wire-contract tests for the media payloads.
 *
 * Each document below is exactly what the native side expects in `native/src/ops/media.rs`, so a
 * key renamed on one side, a payload field added or the peer fields left nested where the native
 * [`PeerTarget`] is flattened breaks a test here rather than a live session. The Rust side pins the
 * same payload shapes in its own test module.
 */
class MediaProtocolTest {
    /** The transport's codec, which leaves a field at its default out of a request payload. */
    private val requests = Json { ignoreUnknownKeys = true }

    @Test
    fun `a full send-media request encodes every option`() {
        val payload = SendMediaPayload(
            peerHandle = 12L,
            path = "/tmp/report.pdf",
            kind = "file",
            caption = "hi",
            parseMode = "html",
            spoiler = true,
            mimeType = "application/pdf",
            ttlSeconds = 30,
            invertMedia = true,
            silent = true,
            replyToMessageId = 31,
            scheduleDate = 1_700_000_000_000L,
            scheduleOnceOnline = true,
        )

        assertEquals(
            """{"peerHandle":12,"path":"/tmp/report.pdf","kind":"file","caption":"hi",""" +
                """"parseMode":"html","spoiler":true,"mimeType":"application/pdf",""" +
                """"ttlSeconds":30,"invertMedia":true,"silent":true,"replyToMessageId":31,""" +
                """"scheduleDate":1700000000000,"scheduleOnceOnline":true}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `a minimal send-media request leaves the defaults to the native side`() {
        assertEquals(
            """{"path":"/tmp/a.bin"}""",
            requests.encodeToString(SendMediaPayload(path = "/tmp/a.bin")),
        )
    }

    @Test
    fun `a send-media request can name an upload handle instead of a path`() {
        assertEquals(
            """{"peerHandle":12,"kind":"photo","fileHandle":7}""",
            requests.encodeToString(
                SendMediaPayload(peerHandle = 12L, path = null, kind = "photo", fileHandle = 7),
            ),
        )
    }

    @Test
    fun `a full url request encodes every option it carries`() {
        val payload = SendMediaUrlPayload(
            peerHandle = 1L,
            url = "https://example.org/a.jpg",
            kind = "photo",
            caption = "c",
            parseMode = "markdown",
            spoiler = true,
            ttlSeconds = 5,
            silent = true,
        )

        assertEquals(
            """{"peerHandle":1,"url":"https://example.org/a.jpg","kind":"photo","caption":"c",""" +
                """"parseMode":"markdown","spoiler":true,"ttlSeconds":5,"silent":true}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `a url request leaves the kind a document when it is not named`() {
        assertEquals(
            """{"url":"https://example.org/a.pdf"}""",
            requests.encodeToString(SendMediaUrlPayload(url = "https://example.org/a.pdf")),
        )
    }

    @Test
    fun `a copy request names both ends and the new caption`() {
        val payload = CopyMediaPayload(
            destination = PeerTarget(peerHandle = 1L),
            source = PeerTarget(username = "channel"),
            messageId = 31,
            caption = "copied",
            parseMode = "html",
            silent = true,
        )

        assertEquals(
            """{"destination":{"peerHandle":1},"source":{"username":"channel"},"messageId":31,""" +
                """"caption":"copied","parseMode":"html","silent":true}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `a copy request defaults to a plain caption and no reply`() {
        val payload = CopyMediaPayload(
            destination = PeerTarget(peerHandle = 1L),
            source = PeerTarget(peerHandle = 2L),
            messageId = 7,
        )

        assertEquals(
            """{"destination":{"peerHandle":1},"source":{"peerHandle":2},"messageId":7}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `a message with a spoiler document decodes the media options this domain sends`() {
        val message = requests.decodeFromString<Message>(MESSAGE)

        assertEquals("document", message.media?.kind)
        assertEquals("application/pdf", message.media?.mimeType)
        assertEquals(true, message.media?.spoiler)
        assertEquals(60, message.media?.ttlSeconds)
        assertEquals(1_700_000_000_000L, message.date)
    }

    private companion object {
        val MESSAGE = """
            {"id": 31, "text": "hello", "outgoing": true, "replyToMessageId": null,
             "date": 1700000000000, "media":
             {"kind": "document", "id": 1, "size": 2, "name": "report.pdf",
              "mimeType": "application/pdf", "spoiler": true, "ttlSeconds": 60}}
        """.trimIndent()
    }
}
