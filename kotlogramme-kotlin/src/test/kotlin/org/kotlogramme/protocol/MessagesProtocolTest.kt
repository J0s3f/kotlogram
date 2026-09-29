package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Wire-contract tests for the message-operation payloads and results.
 *
 * The documents below are exactly what `native/src/ops/messages.rs` reads and emits, which the
 * Rust tests in that module pin on their side, so a field renamed on one side fails here instead of
 * a live session.
 */
class MessagesProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** The transport's own codec, which leaves a field at its default out of a request payload. */
    private val requests = Json { ignoreUnknownKeys = true }

    @Test
    fun `a message count decodes the document the native side emits`() {
        val count = json.decodeFromString<MessageCount>("""{"total": 3}""")

        assertEquals(3, count.total)
    }

    @Test
    fun `history paging reaches the payload`() {
        val payload = HistoryPayload(PeerTarget(peerHandle = 7L), limit = 20, offsetId = 31, maxDate = 1_700_000_000_000L)

        assertEquals(
            """{"peerHandle":7,"limit":20,"offsetId":31,"maxDate":1700000000000}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `a history page without paging leaves the native defaults to fill`() {
        assertEquals(
            """{"peerHandle":7,"limit":20}""",
            requests.encodeToString(HistoryPayload(PeerTarget(peerHandle = 7L), limit = 20)),
        )
    }

    @Test
    fun `the history paging document a newer bridge emits decodes`() {
        val payload = json.decodeFromString<HistoryPayload>(
            """{"peerHandle":7,"limit":20,"offsetId":31,"maxDate":1700000000000}""",
        )

        assertEquals(20, payload.limit)
        assertEquals(31, payload.offsetId)
        assertEquals(1_700_000_000_000L, payload.maxDate)
    }

    @Test
    fun `every search option reaches the payload`() {
        val payload = SearchMessagesPayload(
            PeerTarget(peerHandle = 1L),
            query = "hi",
            limit = 10,
            offsetId = 5,
            sentBySelf = true,
            minDate = 1_000L,
            maxDate = 2_000L,
            filter = "photos",
        )

        assertEquals(
            """{"peerHandle":1,"query":"hi","limit":10,"offsetId":5,"sentBySelf":true,""" +
                """"minDate":1000,"maxDate":2000,"filter":"photos"}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `a bare search leaves the optional options to the native defaults`() {
        assertEquals(
            """{"peerHandle":1,"query":"hi","limit":10}""",
            requests.encodeToString(SearchMessagesPayload(PeerTarget(peerHandle = 1L), "hi", 10)),
        )
    }

    @Test
    fun `the search options a newer bridge emits decode`() {
        val payload = json.decodeFromString<SearchMessagesPayload>(
            """{"peerHandle":1,"query":"hi","limit":10,"offsetId":5,"sentBySelf":true,""" +
                """"minDate":1000,"maxDate":2000,"filter":"chatPhotos"}""",
        )

        assertEquals("hi", payload.query)
        assertEquals(5, payload.offsetId)
        assertEquals(true, payload.sentBySelf)
        assertEquals(1_000L, payload.minDate)
        assertEquals(2_000L, payload.maxDate)
        assertEquals("chatPhotos", payload.filter)
    }

    @Test
    fun `a global search carries its paging and its filter`() {
        assertEquals(
            """{"query":"cats","limit":20,"offsetId":5,"filter":"pinned"}""",
            requests.encodeToString(GlobalSearchPayload("cats", 20, offsetId = 5, filter = "pinned")),
        )
        assertEquals(
            """{"query":"cats","limit":20}""",
            requests.encodeToString(GlobalSearchPayload("cats", 20)),
        )
        assertEquals(
            """{"query":"cats","filter":"video"}""",
            requests.encodeToString(GlobalSearchTotalPayload("cats", filter = "video")),
        )
    }

    @Test
    fun `a message document decodes with every projected field`() {
        val decoded = json.decodeFromString<Message>(MESSAGE)

        assertEquals(31, decoded.id)
        assertEquals(1_700_000_000_000L, decoded.date)
        assertEquals(5, decoded.viewCount)
        assertEquals(2, decoded.reactionCount)
        assertEquals("document", decoded.media?.kind)
        assertNull(decoded.editDate)
    }

    private companion object {
        val MESSAGE = """
            {"id": 31, "text": "hello", "outgoing": true, "replyToMessageId": 30,
             "peerId": -1000007, "senderId": 7, "date": 1700000000000, "editDate": null,
             "mentioned": true, "mediaUnread": false, "silent": true, "pinned": true,
             "fromChannelPost": false, "fromScheduled": true, "editHide": false, "viaBotId": 99,
             "postAuthor": "Author", "groupedId": 88, "viewCount": 5, "forwardCount": 4,
             "replyCount": 3, "reactionCount": 2,
             "media": {"kind": "document", "id": 5150, "name": "report.pdf"}}
        """.trimIndent()
    }
}
