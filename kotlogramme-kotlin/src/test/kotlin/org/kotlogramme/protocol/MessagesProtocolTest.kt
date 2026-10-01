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
        assertEquals("<b>hello</b>", decoded.htmlText)
        assertEquals("**hello**", decoded.markdownText)
        assertEquals(4, decoded.entities.size)
    }

    @Test
    fun `a message document decodes an entity of each kind that carries an extra`() {
        val decoded = json.decodeFromString<Message>(MESSAGE)

        // Every field travels for every kind; only the extra a kind carries stops being null.
        val pre = decoded.entities[0]
        assertEquals("pre", pre.type)
        assertEquals(0, pre.offset)
        assertEquals(4, pre.length)
        assertEquals("rust", pre.language)
        assertNull(pre.url)
        assertNull(pre.userId)
        assertNull(pre.customEmojiId)

        val textUrl = decoded.entities[1]
        assertEquals("textUrl", textUrl.type)
        assertEquals("https://example.org", textUrl.url)
        assertNull(textUrl.language)
        assertNull(textUrl.userId)
        assertNull(textUrl.customEmojiId)

        val mentionName = decoded.entities[2]
        assertEquals("mentionName", mentionName.type)
        assertEquals(42L, mentionName.userId)
        assertNull(mentionName.url)
        assertNull(mentionName.language)
        assertNull(mentionName.customEmojiId)

        val customEmoji = decoded.entities[3]
        assertEquals("customEmoji", customEmoji.type)
        assertEquals(5_150L, customEmoji.customEmojiId)
        assertNull(customEmoji.url)
        assertNull(customEmoji.userId)
        assertNull(customEmoji.language)
    }

    @Test
    fun `a message without entities decodes an empty list, not null`() {
        val decoded = json.decodeFromString<Message>(
            """{"id": 1, "text": "hi", "outgoing": false, "replyToMessageId": null}""",
        )

        assertEquals(0, decoded.entities.size)
        assertEquals("", decoded.htmlText)
        assertEquals("", decoded.markdownText)
    }

    @Test
    fun `a message document decodes the quoted text with its own entities`() {
        val decoded = json.decodeFromString<Message>(MESSAGE)

        // The quote carries its own entity list and its own renderings, grouped as one value.
        val quote = decoded.quote
        assertEquals("see docs", quote?.text)
        assertEquals("<b>see</b> <a href=\"https://example.org\">docs</a>", quote?.htmlText)
        assertEquals("**see** [docs](https://example.org)", quote?.markdownText)
        assertEquals(listOf("bold", "textUrl"), quote?.entities?.map { it.type })
        assertEquals("https://example.org", quote?.entities?.get(1)?.url)
    }

    @Test
    fun `a message that quotes nothing decodes a null quote`() {
        // A message that is not a reply, and a reply whose header carries no text, both leave the
        // quote out rather than sending four empty strings.
        val decoded = json.decodeFromString<Message>(
            """{"id": 1, "text": "hi", "outgoing": false, "replyToMessageId": null}""",
        )

        assertNull(decoded.quote)
    }

    @Test
    fun `a message document decodes the enriched fields`() {
        val decoded = json.decodeFromString<Message>(MESSAGE)

        // Forward header.
        val forward = decoded.forwardHeader
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
        val reply = decoded.replyHeader
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
        assertEquals(7L, reply?.replyFrom?.fromId)
        assertEquals("Some One", reply?.replyFrom?.fromName)

        // Restriction reasons.
        assertEquals(1, decoded.restrictionReasons.size)
        assertEquals(listOf("all", "ios"), decoded.restrictionReasons[0].platforms)
        assertEquals("spam", decoded.restrictionReasons[0].reason)
        assertEquals("Reported as spam", decoded.restrictionReasons[0].text)

        // Action.
        assertEquals(31, decoded.action?.messageId)
        assertEquals(7L, decoded.action?.senderId)
        assertEquals("pinMessage", decoded.action?.kind)

        // Reply markup.
        val markup = decoded.replyMarkup
        assertEquals("inline", markup?.kind)
        assertEquals(1, markup?.rows?.size)
        val button = markup?.rows?.get(0)?.get(0)
        assertEquals("url", button?.kind)
        assertEquals("Open", button?.text)
        assertEquals("https://example.org", button?.url)

        // Peer and sender.
        assertEquals(-1_000_007L, decoded.peer?.id)
        assertEquals("channel", decoded.peer?.kind)
        assertEquals(7L, decoded.sender?.id)
        assertEquals("someone", decoded.sender?.username)
    }

    @Test
    fun `a send payload carries its parse mode and its entities`() {
        val payload = SendMessagePayload(
            PeerTarget(peerHandle = 7L),
            text = "<b>hi</b>",
            replyToMessageId = null,
            silent = false,
            linkPreview = true,
            parseMode = "html",
            entities = listOf(EntitySpec(offset = 0, length = 2, type = "bold")),
        )

        assertEquals(
            """{"peerHandle":7,"text":"<b>hi</b>","parseMode":"html",""" +
                """"entities":[{"offset":0,"length":2,"type":"bold"}]}""",
            requests.encodeToString(payload),
        )

        // Both are absent by default, so a plain send stays plain.
        assertEquals(
            """{"peerHandle":7,"text":"hi"}""",
            requests.encodeToString(
                SendMessagePayload(PeerTarget(peerHandle = 7L), "hi", null, false, true),
            ),
        )
    }

    @Test
    fun `a send-file payload names either a path or an upload handle`() {
        assertEquals(
            """{"peerHandle":7,"path":"/tmp/a.pdf","caption":"c","asPhoto":true}""",
            requests.encodeToString(
                SendFilePayload(
                    PeerTarget(peerHandle = 7L),
                    path = "/tmp/a.pdf",
                    caption = "c",
                    asPhoto = true,
                    replyToMessageId = null,
                    silent = false,
                ),
            ),
        )
        assertEquals(
            """{"peerHandle":7,"caption":"","asPhoto":false,"fileHandle":12}""",
            requests.encodeToString(
                SendFilePayload(
                    PeerTarget(peerHandle = 7L),
                    path = null,
                    caption = "",
                    asPhoto = false,
                    replyToMessageId = null,
                    silent = false,
                    fileHandle = 12,
                ),
            ),
        )
    }

    @Test
    fun `an album item names either a path or an upload handle`() {
        assertEquals(
            """{"peerHandle":7,"items":[""" +
                """{"path":"/tmp/a.pdf","caption":"","asPhoto":false},""" +
                """{"caption":"","asPhoto":false,"fileHandle":12}]}""",
            requests.encodeToString(
                SendAlbumPayload(
                    PeerTarget(peerHandle = 7L),
                    listOf(
                        AlbumItemPayload(path = "/tmp/a.pdf", caption = "", asPhoto = false),
                        AlbumItemPayload(caption = "", asPhoto = false, fileHandle = 12),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `an edit payload carries every rich option`() {
        val payload = EditMessagePayload(
            peer = PeerTarget(peerHandle = 7L),
            messageId = 31,
            text = "<b>hi</b>",
            linkPreview = false,
            parseMode = "html",
            entities = listOf(EntitySpec(offset = 0, length = 2, type = "italic")),
            invertMedia = true,
            ttlSeconds = 30,
            markup = MarkupSpec.Hide(selective = true),
            media = EditMediaSpec(path = "/tmp/a.pdf", kind = "file"),
        )

        assertEquals(
            """{"peerHandle":7,"messageId":31,"text":"<b>hi</b>","linkPreview":false,""" +
                """"parseMode":"html","entities":[{"offset":0,"length":2,"type":"italic"}],""" +
                """"invertMedia":true,"ttlSeconds":30,"markup":{"kind":"hide","selective":true},""" +
                """"media":{"path":"/tmp/a.pdf","kind":"file"}}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `an edit payload leaves the rich options out by default`() {
        val payload = EditMessagePayload(PeerTarget(peerHandle = 7L), messageId = 31)

        assertEquals("""{"peerHandle":7,"messageId":31}""", requests.encodeToString(payload))
    }

    @Test
    fun `an edit media carries the video streaming dimensions`() {
        val payload = EditMediaSpec(
            path = "/tmp/movie.mp4",
            kind = "video",
            durationSeconds = 12.5,
            width = 1920,
            height = 1080,
        )

        assertEquals(
            """{"path":"/tmp/movie.mp4","kind":"video","durationSeconds":12.5,"width":1920,"height":1080}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `an edit media names a copy of another message`() {
        val payload = EditMediaSpec(
            copyOf = CopyOfSpec(peer = PeerTarget(peerHandle = 2L), messageId = 7),
        )

        assertEquals(
            """{"copyOf":{"peer":{"peerHandle":2},"messageId":7}}""",
            requests.encodeToString(payload),
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
             "quote": {"text": "see docs",
                       "entities": [{"type": "bold", "offset": 0, "length": 3, "url": null,
                                     "userId": null, "language": null, "customEmojiId": null},
                                    {"type": "textUrl", "offset": 4, "length": 4,
                                     "url": "https://example.org", "userId": null,
                                     "language": null, "customEmojiId": null}],
                       "htmlText": "<b>see</b> <a href=\"https://example.org\">docs</a>",
                       "markdownText": "**see** [docs](https://example.org)"},
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
