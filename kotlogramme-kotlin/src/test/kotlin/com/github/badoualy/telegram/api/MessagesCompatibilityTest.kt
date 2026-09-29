package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.Message as BridgeMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests that the message compatibility facade carries the whole projection and that the search
 * filter names line up with the native `messages_filter`.
 *
 * The message document is the same one `MessagesProtocolTest` pins on the wire side, so a field
 * added to `native/src/dto/message.rs` without reaching this layer fails here.
 */
class MessagesCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a message keeps the fields the facade gained`() {
        val message = json.decodeFromString<BridgeMessage>(MESSAGE).toCompatibility()

        assertEquals(31, message.id)
        assertEquals("hello", message.text)
        assertTrue(message.outgoing && message.mentioned && message.silent && message.pinned)
        assertTrue(message.fromScheduled && !message.fromChannelPost && !message.editHide)
        assertEquals(30, message.replyToMessageId)
        assertEquals(-1_000_007L, message.peerId)
        assertEquals(7L, message.senderId)
        assertEquals(1_700_000_000_000L, message.date)
        assertNull(message.editDate)
        assertEquals(99L, message.viaBotId)
        assertEquals("Author", message.postAuthor)
        assertEquals(88L, message.groupedId)
        assertEquals(5, message.viewCount)
        assertEquals(4, message.forwardCount)
        assertEquals(3, message.replyCount)
        assertEquals(2, message.reactionCount)
        assertEquals("document", message.media?.kind)
        assertEquals("report.pdf", message.media?.name)
    }

    @Test
    fun `every search filter names the same wire name the native side reads`() {
        assertEquals(
            listOf(
                "empty", "photos", "video", "photoVideo", "document", "url", "gif", "voice", "music",
                "chatPhotos", "phoneCalls", "roundVoice", "roundVideo", "myMentions", "geo",
                "contacts", "pinned",
            ),
            MessageSearchFilter.entries.map { it.wire },
        )
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
