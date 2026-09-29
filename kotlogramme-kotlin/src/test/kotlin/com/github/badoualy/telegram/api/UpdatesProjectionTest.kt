package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.NextRawUpdateResult
import org.kotlogramme.protocol.Update as BridgeUpdate
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade carries the whole update projection.
 *
 * The documents are the ones `UpdatesProtocolTest` pins on the wire side, so a field added to
 * `native/src/dto/update.rs` without reaching this layer fails here.
 */
class UpdatesProjectionTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a typed update projects every field the facade gained`() {
        val update = json.decodeFromString<BridgeUpdate>(UPDATE_CALLBACK_QUERY).toTypedCompatibility()

        assertEquals("callbackQuery", update.kind)
        assertEquals(1_700_000_000_000, update.state?.date)
        assertEquals(12, update.state?.seq)
        assertEquals(UpdateMessageBox("channel", 3, -1_000_007), update.state?.messageBox)
        assertEquals("where is", update.inlineQuery?.text)
        assertEquals("broadcast", update.inlineQuery?.peerType)
        assertEquals("result-1", update.inlineSend?.resultId)
        assertEquals(InlineMessageId(2, 77, 88), update.inlineSend?.messageId)
        assertEquals("updateUserTyping", update.rawUpdate?.name)
    }

    @Test
    fun `a typed update carries the same kind and message the two-field projection does`() {
        val typed = json.decodeFromString<BridgeUpdate>(UPDATE_NEW_MESSAGE).toTypedCompatibility()
        val compatibility = json.decodeFromString<BridgeUpdate>(UPDATE_NEW_MESSAGE).toCompatibility()

        assertEquals("newMessage", typed.kind)
        assertEquals(31, typed.message?.id)
        assertEquals(compatibility, TelegramUpdate(typed.kind, typed.message))
    }

    @Test
    fun `a kind with no payload projects every field as absent`() {
        val update = json.decodeFromString<BridgeUpdate>(UPDATE_UNKNOWN).toTypedCompatibility()

        assertEquals("unknown", update.kind)
        assertNull(update.message)
        assertNull(update.state)
        assertNull(update.deletedMessageIds)
        assertNull(update.deletedChannelId)
        assertNull(update.callbackQuery)
        assertNull(update.inlineQuery)
        assertNull(update.inlineSend)
        assertEquals("updateUserTyping", update.rawUpdate?.name)
    }

    @Test
    fun `a callback query decodes its button data and names the message it belongs to`() {
        val query = assertNotNull(
            json.decodeFromString<BridgeUpdate>(UPDATE_CALLBACK_QUERY).toTypedCompatibility().callbackQuery,
        )

        assertContentEquals("data".toByteArray(), query.data)
        assertEquals(900, query.queryId)
        assertEquals(31, query.messageId)
        assertNull(query.inlineMessageId)
        assertFalse(query.isFromInline)
        assertEquals("channel", query.peer.kind)
        assertEquals(7, query.sender.id)
    }

    @Test
    fun `an inline callback query names its inline message instead of a chat message`() {
        val query = assertNotNull(
            json.decodeFromString<BridgeUpdate>(UPDATE_INLINE_CALLBACK).toTypedCompatibility().callbackQuery,
        )

        assertTrue(query.isFromInline)
        assertNull(query.messageId)
        assertEquals(InlineMessageId(2, 77, 88), query.inlineMessageId)
        assertEquals("user", query.peer.kind)
    }

    @Test
    fun `a raw update decodes the TL bytes its name was read from`() {
        val raw = assertNotNull(
            json.decodeFromString<BridgeUpdate>(UPDATE_UNKNOWN).toTypedCompatibility().rawUpdate,
        )

        // An update variant this build cannot name reports `unknown` and carries the bytes, so the
        // variant is still readable from the constructor the name came from.
        assertEquals("updateUserTyping", raw.name)
        assertContentEquals(UPDATE_USER_TYPING_BYTES, raw.data)
        assertEquals("XL8XKgAAAAAHAAAAAAAAAE50vxY=", Base64.getEncoder().encodeToString(raw.data))
    }

    @Test
    fun `a raw update entry projects the state and the peers it was delivered with`() {
        val entry = assertNotNull(json.decodeFromString<NextRawUpdateResult>(RAW_UPDATE_RESULT).update)

        assertEquals("updateUserTyping", entry.update.name)
        assertEquals(12, entry.state.seq)
        assertEquals(-1_000_007, entry.state.messageBox?.channelId)
        // The native side sorts the peer map by id, so the facade sees a stable order.
        assertEquals(listOf(7L, -1_000_007L), entry.peers.map { it.id })
        assertEquals(listOf("user", "channel"), entry.peers.map { it.kind })
    }

    @Test
    fun `a timed-out stream projects to no update`() {
        assertNull(json.decodeFromString<NextRawUpdateResult>("""{"update": null}""").update)
    }

    @Test
    fun `the update callback takes the projected update rather than only the client`() {
        // The signature is what makes the callback useful, so pin it: a handler has to be able to
        // read the payload, and it is handed the client so it can answer on the same session.
        val onUpdate = UpdateCallback::class.java.methods.single { it.name == "onUpdate" }

        assertEquals(
            listOf(TelegramClient::class.java, TypedUpdate::class.java),
            onUpdate.parameterTypes.toList(),
        )
    }

    private companion object {
        val USER = """
            {"id": 7, "username": "someone", "firstName": "Some", "lastName": null,
             "fullName": "Some", "usernames": [], "phone": null, "photoId": null, "status": "offline",
             "statusExpires": null, "lastSeen": null, "statusByMe": false, "langCode": null,
             "isSelf": false, "contact": false, "mutualContact": false, "deleted": false,
             "isBot": true, "botPrivacy": false, "botSupportsChats": false, "botInlineGeo": false,
             "botInlinePlaceholder": null, "verified": false, "restricted": false, "support": false,
             "scam": false, "restrictionReasons": []}
        """.trimIndent()

        val PEER_CHANNEL = """
            {"nativeHandle": 12, "id": -1000007, "kind": "channel", "username": "channel",
             "name": "A Channel", "usernames": [], "isMegagroup": null, "hasPhoto": true,
             "permissions": null}
        """.trimIndent()

        val PEER_USER = """
            {"nativeHandle": 1, "id": 7, "kind": "user", "username": "someone", "name": "Some",
             "usernames": [], "isMegagroup": null, "hasPhoto": false, "permissions": null}
        """.trimIndent()

        val UPDATE_CALLBACK_QUERY = """
            {"kind": "callbackQuery", "message": null,
             "state": {"date": 1700000000000, "seq": 12,
                       "messageBox": {"kind": "channel", "pts": 3, "channelId": -1000007}},
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
             "rawUpdate": {"name": "updateUserTyping", "data": "XL8XKgAAAAAHAAAAAAAAAE50vxY="}}
        """.trimIndent()

        val UPDATE_INLINE_CALLBACK = """
            {"kind": "callbackQuery", "message": null, "state": null, "deletedMessageIds": null,
             "deletedChannelId": null,
             "callbackQuery": {
                "data": "ZGF0YQ==", "isFromInline": true, "queryId": 902, "messageId": null,
                "inlineMessageId": {"dcId": 2, "accessHash": 77, "id": 88},
                "peer": ${PEER_USER.trimIndent()}, "sender": ${USER.trimIndent()}},
             "inlineQuery": null, "inlineSend": null, "rawUpdate": null}
        """.trimIndent()

        val UPDATE_NEW_MESSAGE = """
            {"kind": "newMessage",
             "message": {"id": 31, "text": "hello", "outgoing": false, "replyToMessageId": null,
                         "peerId": 7, "senderId": 7, "date": 1700000000000, "editDate": null,
                         "mentioned": false, "mediaUnread": false, "silent": false, "pinned": false,
                         "fromChannelPost": false, "fromScheduled": false, "editHide": false,
                         "viaBotId": null, "postAuthor": null, "groupedId": null, "viewCount": null,
                         "forwardCount": null, "replyCount": null, "reactionCount": null,
                         "media": null},
             "state": {"date": 1700000000000, "seq": 1,
                       "messageBox": {"kind": "common", "pts": 7, "channelId": null}},
             "deletedMessageIds": null, "deletedChannelId": null, "callbackQuery": null,
             "inlineQuery": null, "inlineSend": null, "rawUpdate": null}
        """.trimIndent()

        val UPDATE_UNKNOWN = """
            {"kind": "unknown", "message": null, "state": null, "deletedMessageIds": null,
             "deletedChannelId": null, "callbackQuery": null, "inlineQuery": null,
             "inlineSend": null,
             "rawUpdate": {"name": "updateUserTyping", "data": "XL8XKgAAAAAHAAAAAAAAAE50vxY="}}
        """.trimIndent()

        /** `updateUserTyping` for user 7: its constructor id, the id, and the typing action. */
        val UPDATE_USER_TYPING_BYTES = byteArrayOf(
            0x5c, 0xbf.toByte(), 0x17, 0x2a, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0,
            0x4e, 0x74, 0xbf.toByte(), 0x16,
        )

        val RAW_UPDATE_RESULT = """
            {"update": {"update": {"name": "updateUserTyping",
                                   "data": "XL8XKgAAAAAHAAAAAAAAAE50vxY="},
                        "state": {"date": 1700000000000, "seq": 12,
                                  "messageBox": {"kind": "channel", "pts": 3, "channelId": -1000007}},
                        "peers": [${PEER_USER.trimIndent()}, ${PEER_CHANNEL.trimIndent()}]}}
        """.trimIndent()
    }
}
