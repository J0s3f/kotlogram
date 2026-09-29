package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the update-stream models.
 *
 * Each document below is exactly what the Rust projection in `native/src/dto/update.rs` emits, so a
 * field renamed on one side, a date turned from milliseconds into seconds, or a payload field added
 * to one projection only breaks a test here rather than a live session. The Rust side pins the same
 * documents in the `dto::update` test module.
 */
class UpdatesProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `a callback query update decodes and re-encodes unchanged`() {
        val update = roundTrips<Update>(UPDATE_CALLBACK_QUERY)
        assertEquals("callbackQuery", update.kind)
        assertNull(update.message)
        assertEquals(12, update.state?.seq)
        assertEquals(1_700_000_000_000, update.state?.date)
        assertEquals("updateUserTyping", update.rawUpdate?.name)
    }

    @Test
    fun `an update declares exactly the fields the native projection emits`() {
        val names = (json.parseToJsonElement(UPDATE_CALLBACK_QUERY) as JsonObject).keys.sorted()
        assertEquals(UPDATE_FIELDS.sorted(), names)
    }

    @Test
    fun `a message update carries a message and a state and nothing else`() {
        val update = roundTrips<Update>(UPDATE_NEW_MESSAGE)
        assertEquals("newMessage", update.kind)
        assertEquals(31, update.message?.id)
        assertEquals("common", update.state?.messageBox?.kind)
        // Every field is always present, so a kind is told apart by which ones stop being null.
        assertNull(update.deletedMessageIds)
        assertNull(update.deletedChannelId)
        assertNull(update.callbackQuery)
        assertNull(update.inlineQuery)
        assertNull(update.inlineSend)
        assertNull(update.rawUpdate)
    }

    @Test
    fun `a channel deletion names the deleted ids and the channel`() {
        val update = roundTrips<Update>(UPDATE_DELETED_CHANNEL)
        assertEquals("messageDeleted", update.kind)
        assertEquals(listOf(31), update.deletedMessageIds)
        assertEquals(-1_000_007, update.deletedChannelId)
        assertNull(update.message)
    }

    @Test
    fun `a chat deletion carries no channel`() {
        val update = roundTrips<Update>(UPDATE_DELETED_GROUP)
        assertEquals(listOf(31, 32), update.deletedMessageIds)
        assertNull(update.deletedChannelId)
    }

    @Test
    fun `an update the bridge cannot name still carries its raw bytes`() {
        val update = roundTrips<Update>(UPDATE_UNKNOWN)
        assertEquals("unknown", update.kind)
        assertEquals("updateUserTyping", update.rawUpdate?.name)
        assertTrue(update.rawUpdate?.data?.isNotEmpty() == true)
    }

    @Test
    fun `a state reports the message box grammers split it into`() {
        assertEquals(
            UpdateState(1_700_000_000_000, 12, UpdateMessageBox("channel", 3, -1_000_007)),
            roundTrips<UpdateState>(STATE_CHANNEL),
        )
        // The secondary sequence has its own counter and no channel, and the common one names a
        // chat rather than a channel.
        assertEquals(UpdateMessageBox("secondary", 5, null), roundTrips<UpdateState>(STATE_SECONDARY).messageBox)
        assertEquals(UpdateMessageBox("common", 7, null), roundTrips<UpdateState>(STATE_COMMON).messageBox)
        // An update that does not pertain to a message-related sequence has no box at all.
        assertEquals(UpdateState(1_700_000_000_000, 4, null), roundTrips<UpdateState>(STATE_NO_BOX))
    }

    @Test
    fun `a callback query reports the button data and one of the two message identifiers`() {
        val chat = roundTrips<CallbackQueryUpdate>(CALLBACK_QUERY_CHAT)
        assertEquals("ZGF0YQ==", chat.data)
        assertEquals(900, chat.queryId)
        assertEquals(31, chat.messageId)
        assertNull(chat.inlineMessageId)
        assertTrue(!chat.isFromInline)
        assertEquals("channel", chat.peer.kind)

        val inline = roundTrips<CallbackQueryUpdate>(CALLBACK_QUERY_INLINE)
        assertTrue(inline.isFromInline)
        assertNull(inline.messageId)
        assertEquals(InlineMessageId(2, 77, 88), inline.inlineMessageId)
    }

    @Test
    fun `an inline query and an inline send report the query they belong to`() {
        val query = roundTrips<InlineQueryUpdate>(INLINE_QUERY)
        assertEquals("where is", query.text)
        assertEquals("10", query.offset)
        assertEquals(901, query.queryId)
        assertEquals("broadcast", query.peerType)
        assertEquals(7, query.sender.id)
        assertNull(roundTrips<InlineQueryUpdate>(INLINE_QUERY_NO_PEER).peerType)

        val send = roundTrips<InlineSendUpdate>(INLINE_SEND)
        assertEquals("result-1", send.resultId)
        assertEquals(InlineMessageId(2, 77, 88), send.messageId)
        assertNull(roundTrips<InlineSendUpdate>(INLINE_SEND_NO_MESSAGE).messageId)
    }

    @Test
    fun `a raw update is a constructor name and its TL bytes`() {
        val raw = roundTrips<RawUpdate>(RAW_UPDATE)
        // The generated TL types carry no serde derives, so the bytes travel base64-encoded rather
        // than as an object; the constructor id they start with is the one the name came from.
        assertEquals("updateUserTyping", raw.name)
        assertEquals("XL8XKgAAAAAHAAAAAAAAAE50vxY=", raw.data)
        // `updateUserTyping` is 0x2a17bf5c, then the user id, the absent optional flag and the
        // typing action.
        assertContentEquals(
            byteArrayOf(
                0x5c, 0xbf.toByte(), 0x17, 0x2a, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0,
                0x4e, 0x74, 0xbf.toByte(), 0x16,
            ),
            Base64.getDecoder().decode(raw.data),
        )
    }

    @Test
    fun `a raw update entry keeps the state and the peers it was delivered with`() {
        val entry = assertNotNull(roundTrips<NextRawUpdateResult>(RAW_UPDATE_RESULT).update)
        assertEquals("updateUserTyping", entry.update.name)
        assertEquals(12, entry.state.seq)
        assertEquals(-1_000_007, entry.state.messageBox?.channelId)
        // The native side sorts the peer map by id, so the document is stable.
        assertEquals(listOf(7L, -1_000_007L), entry.peers.map { it.id })
    }

    @Test
    fun `a stream that stays quiet decodes to no update`() {
        assertNull(json.decodeFromString<NextUpdateResult>("""{"update": null}""").update)
        assertNull(json.decodeFromString<NextRawUpdateResult>("""{"update": null}""").update)
        assertTrue(json.decodeFromString<OperationResult>("""{"ok": true}""").ok)
    }

    @Test
    fun `the minimal documents an older bridge emits still decode`() {
        val update = json.decodeFromString<Update>("""{"kind": "newMessage"}""")
        assertEquals("newMessage", update.kind)
        assertNull(update.message)
        assertNull(update.state)
        assertNull(update.deletedMessageIds)
        assertNull(update.rawUpdate)

        val state = json.decodeFromString<UpdateState>("""{"date": 1700000000000, "seq": 4}""")
        assertEquals(4, state.seq)
        assertNull(state.messageBox)
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
        val USER = """
            {"id": 7, "username": "someone", "firstName": "Some", "lastName": null,
             "fullName": "Some", "usernames": ["someone_alt"], "phone": null, "photoId": null,
             "status": "offline", "statusExpires": null, "lastSeen": null, "statusByMe": false,
             "langCode": null, "isSelf": false, "contact": false, "mutualContact": false,
             "deleted": false, "isBot": true, "botPrivacy": false, "botSupportsChats": false,
             "botInlineGeo": false, "botInlinePlaceholder": null, "verified": false,
             "restricted": false, "support": false, "scam": false, "restrictionReasons": []}
        """.trimIndent()

        val PEER_CHANNEL = """
            {"nativeHandle": 12, "id": -1000007, "kind": "channel", "username": "channel",
             "name": "A Channel", "usernames": ["channel_alt"], "isMegagroup": null,
             "hasPhoto": true, "permissions": null}
        """.trimIndent()

        val PEER_USER = """
            {"nativeHandle": 1, "id": 7, "kind": "user", "username": "someone", "name": "Some",
             "usernames": [], "isMegagroup": null, "hasPhoto": false, "permissions": null}
        """.trimIndent()

        val MESSAGE = """
            {"id": 31, "text": "hello", "outgoing": false, "replyToMessageId": null, "peerId": 7,
             "senderId": 7, "date": 1700000000000, "editDate": null, "mentioned": false,
             "mediaUnread": false, "silent": false, "pinned": false, "fromChannelPost": false,
             "fromScheduled": false, "editHide": false, "viaBotId": null, "postAuthor": null,
             "groupedId": null, "viewCount": null, "forwardCount": null, "replyCount": null,
             "reactionCount": null, "media": null}
        """.trimIndent()

        val STATE_CHANNEL = """
            {"date": 1700000000000, "seq": 12,
             "messageBox": {"kind": "channel", "pts": 3, "channelId": -1000007}}
        """.trimIndent()

        val STATE_SECONDARY = """
            {"date": 1700000000000, "seq": 0, "messageBox": {"kind": "secondary", "pts": 5, "channelId": null}}
        """.trimIndent()

        val STATE_COMMON = """
            {"date": 1700000000000, "seq": 1, "messageBox": {"kind": "common", "pts": 7, "channelId": null}}
        """.trimIndent()

        val STATE_NO_BOX = """{"date": 1700000000000, "seq": 4, "messageBox": null}"""

        val RAW_UPDATE = """{"name": "updateUserTyping", "data": "XL8XKgAAAAAHAAAAAAAAAE50vxY="}"""

        /** Every field of an update document, kept in step with `UPDATE_FIELDS` in the Rust tests. */
        val UPDATE_CALLBACK_QUERY = """
            {"kind": "callbackQuery", "message": null, "state": ${STATE_CHANNEL.trimIndent()},
             "deletedMessageIds": null, "deletedChannelId": null,
             "callbackQuery": {
                "data": "ZGF0YQ==", "isFromInline": false, "queryId": 900, "messageId": 31,
                "inlineMessageId": null, "peer": ${PEER_CHANNEL.trimIndent()},
                "sender": ${USER.trimIndent()}},
             "inlineQuery": {
                "sender": ${USER.trimIndent()}, "text": "where is", "offset": "10", "queryId": 901,
                "peerType": "broadcast"},
             "inlineSend": {
                "sender": ${USER.trimIndent()}, "text": "where is", "resultId": "result-1",
                "messageId": {"dcId": 2, "accessHash": 77, "id": 88}},
             "rawUpdate": ${RAW_UPDATE.trimIndent()}}
        """.trimIndent()

        val UPDATE_NEW_MESSAGE = """
            {"kind": "newMessage", "message": ${MESSAGE.trimIndent()},
             "state": {"date": 1700000000000, "seq": 1,
                       "messageBox": {"kind": "common", "pts": 7, "channelId": null}},
             "deletedMessageIds": null, "deletedChannelId": null, "callbackQuery": null,
             "inlineQuery": null, "inlineSend": null, "rawUpdate": null}
        """.trimIndent()

        val UPDATE_DELETED_GROUP = """
            {"kind": "messageDeleted", "message": null,
             "state": {"date": 1700000000000, "seq": 2,
                       "messageBox": {"kind": "common", "pts": 8, "channelId": null}},
             "deletedMessageIds": [31, 32], "deletedChannelId": null, "callbackQuery": null,
             "inlineQuery": null, "inlineSend": null, "rawUpdate": null}
        """.trimIndent()

        val UPDATE_DELETED_CHANNEL = """
            {"kind": "messageDeleted", "message": null,
             "state": {"date": 1700000000000, "seq": 3,
                       "messageBox": {"kind": "channel", "pts": 3, "channelId": -1000007}},
             "deletedMessageIds": [31], "deletedChannelId": -1000007, "callbackQuery": null,
             "inlineQuery": null, "inlineSend": null, "rawUpdate": null}
        """.trimIndent()

        val UPDATE_UNKNOWN = """
            {"kind": "unknown", "message": null, "state": null, "deletedMessageIds": null,
             "deletedChannelId": null, "callbackQuery": null, "inlineQuery": null,
             "inlineSend": null, "rawUpdate": ${RAW_UPDATE.trimIndent()}}
        """.trimIndent()

        val CALLBACK_QUERY_CHAT = """
            {"data": "ZGF0YQ==", "isFromInline": false, "queryId": 900, "messageId": 31,
             "inlineMessageId": null, "peer": ${PEER_CHANNEL.trimIndent()},
             "sender": ${USER.trimIndent()}}
        """.trimIndent()

        val CALLBACK_QUERY_INLINE = """
            {"data": "ZGF0YQ==", "isFromInline": true, "queryId": 902, "messageId": null,
             "inlineMessageId": {"dcId": 2, "accessHash": 77, "id": 88},
             "peer": ${PEER_USER.trimIndent()}, "sender": ${USER.trimIndent()}}
        """.trimIndent()

        val INLINE_QUERY = """
            {"sender": ${USER.trimIndent()}, "text": "where is", "offset": "10", "queryId": 901,
             "peerType": "broadcast"}
        """.trimIndent()

        val INLINE_QUERY_NO_PEER = """
            {"sender": ${USER.trimIndent()}, "text": "where is", "offset": "", "queryId": 903,
             "peerType": null}
        """.trimIndent()

        val INLINE_SEND = """
            {"sender": ${USER.trimIndent()}, "text": "where is", "resultId": "result-1",
             "messageId": {"dcId": 2, "accessHash": 77, "id": 88}}
        """.trimIndent()

        val INLINE_SEND_NO_MESSAGE = """
            {"sender": ${USER.trimIndent()}, "text": "where is", "resultId": "result-2",
             "messageId": null}
        """.trimIndent()

        /** The peers are sorted by id on the native side, so the document is stable. */
        val RAW_UPDATE_RESULT = """
            {"update": {"update": ${RAW_UPDATE.trimIndent()}, "state": ${STATE_CHANNEL.trimIndent()},
                        "peers": [${PEER_USER.trimIndent()}, ${PEER_CHANNEL.trimIndent()}]}}
        """.trimIndent()

        val UPDATE_FIELDS = listOf(
            "callbackQuery", "deletedChannelId", "deletedMessageIds", "inlineQuery", "inlineSend",
            "kind", "message", "rawUpdate", "state",
        )
    }
}
