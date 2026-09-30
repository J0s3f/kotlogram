package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.EditMediaSpec
import org.kotlogramme.protocol.EntitySpec
import org.kotlogramme.protocol.Message as BridgeMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.nio.file.Path

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
        assertEquals("<b>hello</b>", message.htmlText)
        assertEquals("**hello**", message.markdownText)
        assertEquals(4, message.entities.size)
    }

    @Test
    fun `an entity of each kind carries its extra through the facade`() {
        val message = json.decodeFromString<BridgeMessage>(MESSAGE).toCompatibility()

        // Every field travels for every kind; only the extra a kind carries stops being null.
        val pre = message.entities[0]
        assertEquals("pre", pre.type)
        assertEquals(0, pre.offset)
        assertEquals(4, pre.length)
        assertEquals("rust", pre.language)
        assertNull(pre.url)
        assertNull(pre.userId)
        assertNull(pre.customEmojiId)

        val textUrl = message.entities[1]
        assertEquals("textUrl", textUrl.type)
        assertEquals("https://example.org", textUrl.url)
        assertNull(textUrl.language)
        assertNull(textUrl.userId)
        assertNull(textUrl.customEmojiId)

        val mentionName = message.entities[2]
        assertEquals("mentionName", mentionName.type)
        assertEquals(42L, mentionName.userId)
        assertNull(mentionName.url)
        assertNull(mentionName.language)
        assertNull(mentionName.customEmojiId)

        val customEmoji = message.entities[3]
        assertEquals("customEmoji", customEmoji.type)
        assertEquals(5_150L, customEmoji.customEmojiId)
        assertNull(customEmoji.url)
        assertNull(customEmoji.userId)
        assertNull(customEmoji.language)
    }

    @Test
    fun `a message without entities keeps an empty list, not null`() {
        val message = json.decodeFromString<BridgeMessage>(
            """{"id": 1, "text": "hi", "outgoing": false, "replyToMessageId": null}""",
        ).toCompatibility()

        assertTrue(message.entities.isEmpty())
        assertEquals("", message.htmlText)
        assertEquals("", message.markdownText)
    }

    @Test
    fun `a message keeps the enriched fields the facade gained`() {
        val message = json.decodeFromString<BridgeMessage>(MESSAGE).toCompatibility()

        // Forward header.
        val forward = message.forwardHeader
        assertEquals(true, forward?.imported)
        assertEquals(true, forward?.savedOut)
        assertEquals(7L, forward?.fromId)
        assertEquals("Some One", forward?.fromName)
        assertEquals(1_700_000_000_000L, forward?.date)
        assertEquals(42, forward?.channelPost)
        assertEquals("Author", forward?.postAuthor)
        assertEquals(-1_000_007L, forward?.savedFromPeer)
        assertEquals(30, forward?.savedFromMsgId)
        assertEquals(8L, forward?.savedFromId)
        assertEquals("Original", forward?.savedFromName)
        assertEquals(1_700_000_060_000L, forward?.savedDate)
        assertEquals("psa_type", forward?.psaType)

        // Reply header.
        val reply = message.replyHeader
        assertEquals("header", reply?.kind)
        assertEquals(true, reply?.replyToScheduled)
        assertEquals(true, reply?.forumTopic)
        assertEquals(true, reply?.quote)
        assertEquals(true, reply?.replyToEphemeral)
        assertEquals(30, reply?.replyToMsgId)
        assertEquals(-1_000_007L, reply?.replyToPeerId)
        assertEquals("quoted", reply?.quoteText)
        assertEquals(3, reply?.quoteOffset)
        assertEquals(5, reply?.todoItemId)
        assertEquals("AQID", reply?.pollOption)

        // Restriction reasons.
        assertEquals(1, message.restrictionReasons.size)
        assertEquals(listOf("all", "ios"), message.restrictionReasons[0].platforms)
        assertEquals("spam", message.restrictionReasons[0].reason)
        assertEquals("Reported as spam", message.restrictionReasons[0].text)

        // Action.
        assertEquals(31, message.action?.messageId)
        assertEquals(7L, message.action?.senderId)
        assertEquals("pinMessage", message.action?.kind)

        // Reply markup.
        val markup = message.replyMarkup
        assertEquals("inline", markup?.kind)
        val inline = markup as? ReplyMarkup.Inline
        assertEquals(1, inline?.rows?.size)
        val button = inline?.rows?.get(0)?.get(0) as? Button.Url
        assertEquals("url", button?.kind)
        assertEquals("Open", button?.text)
        assertEquals("https://example.org", button?.url)

        // Peer and sender.
        assertEquals(-1_000_007L, message.peer?.id)
        assertEquals("channel", message.peer?.kind)
        assertEquals(7L, message.sender?.id)
        assertEquals("someone", message.sender?.username)
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

    @Test
    fun `an entity and a media source become the specs the bridge sends`() {
        assertEquals(
            EntitySpec(
                offset = 0,
                length = 2,
                type = "textUrl",
                url = "https://example.org",
                userId = null,
                language = null,
                customEmojiId = null,
            ),
            MessageEntity(type = "textUrl", offset = 0, length = 2, url = "https://example.org").asSpec(),
        )

        assertEquals(
            EditMediaSpec(path = Path.of("/tmp/a.pdf").toAbsolutePath().toString(), kind = "file"),
            EditMedia.File(Path.of("/tmp/a.pdf"), MediaKind.FILE).asSpec(),
        )
        assertEquals(
            EditMediaSpec(url = "https://example.org/a.jpg", kind = "photo"),
            EditMedia.Url("https://example.org/a.jpg", MediaKind.PHOTO).asSpec(),
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
             "media": {"kind": "document", "id": 5150, "name": "report.pdf"},
             "entities": [
                 {"type": "pre", "offset": 0, "length": 4, "url": null, "userId": null,
                  "language": "rust", "customEmojiId": null},
                 {"type": "textUrl", "offset": 5, "length": 4, "url": "https://example.org",
                  "userId": null, "language": null, "customEmojiId": null},
                 {"type": "mentionName", "offset": 10, "length": 3, "url": null, "userId": 42,
                  "language": null, "customEmojiId": null},
                 {"type": "customEmoji", "offset": 14, "length": 1, "url": null, "userId": null,
                  "language": null, "customEmojiId": 5150}],
             "htmlText": "<b>hello</b>",
             "markdownText": "**hello**",
             "forwardHeader": {
                 "imported": true, "savedOut": true, "fromId": 7, "fromName": "Some One",
                 "date": 1700000000000, "channelPost": 42, "postAuthor": "Author",
                 "savedFromPeer": -1000007, "savedFromMsgId": 30, "savedFromId": 8,
                 "savedFromName": "Original", "savedDate": 1700000060000,
                 "psaType": "psa_type"},
             "replyHeader": {
                 "kind": "header", "replyToScheduled": true, "forumTopic": true,
                 "quote": true, "replyToEphemeral": true, "replyToMsgId": 30,
                 "replyToPeerId": -1000007, "replyFrom": {
                     "imported": true, "savedOut": true, "fromId": 7, "fromName": "Some One",
                     "date": 1700000000000, "channelPost": 42, "postAuthor": "Author",
                     "savedFromPeer": -1000007, "savedFromMsgId": 30, "savedFromId": 8,
                     "savedFromName": "Original", "savedDate": 1700000060000,
                     "psaType": "psa_type"},
                 "replyMedia": null, "replyToTopId": 29, "quoteText": "quoted",
                 "quoteOffset": 3, "todoItemId": 5, "pollOption": "AQID",
                 "storyPeer": null, "storyId": null},
             "restrictionReasons": [
                 {"platforms": ["all", "ios"], "reason": "spam", "text": "Reported as spam"}],
             "action": {"messageId": 31, "senderId": 7, "kind": "pinMessage"},
             "replyMarkup": {
                 "kind": "inline",
                 "rows": [[{"kind": "url", "text": "Open", "url": "https://example.org",
                            "data": null, "requiresPassword": null, "fwdText": null,
                            "buttonId": null, "query": null, "samePeer": null,
                            "peerTypes": null, "quiz": null, "userId": null,
                            "copyText": null, "maxQuantity": null,
                            "requestWriteAccess": null}]],
                 "fitSize": false, "singleUse": false, "selective": false,
                 "persistent": false, "placeholder": null},
             "peer": {"nativeHandle": 12, "id": -1000007, "kind": "channel",
                      "username": "channel", "name": "A Channel",
                      "usernames": ["channel_alt"], "isMegagroup": null,
                      "hasPhoto": true, "permissions": null},
             "sender": {"id": 7, "username": "someone", "firstName": "Some", "lastName": "One",
                        "fullName": "Some One", "usernames": ["someone_alt"], "phone": null,
                        "photoId": null, "status": "offline", "statusExpires": null,
                        "lastSeen": null, "statusByMe": false, "langCode": null,
                        "isSelf": false, "contact": false, "mutualContact": false,
                        "deleted": false, "isBot": true, "botPrivacy": false,
                        "botSupportsChats": false, "botInlineGeo": false,
                        "botInlinePlaceholder": null, "verified": false,
                        "restricted": false, "support": false, "scam": false,
                        "restrictionReasons": []}}
        """.trimIndent()
    }
}
