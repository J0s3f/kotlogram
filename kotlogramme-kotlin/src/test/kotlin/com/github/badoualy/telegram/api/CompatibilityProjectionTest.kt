package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.Dialog as BridgeDialog
import org.kotlogramme.protocol.Media as BridgeMedia
import org.kotlogramme.protocol.Message as BridgeMessage
import org.kotlogramme.protocol.Participant as BridgeParticipant
import org.kotlogramme.protocol.Peer as BridgePeer
import org.kotlogramme.protocol.User as BridgeUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade carries the whole projection, not just the four fields it
 * started with.
 *
 * The documents are the same ones `ProtocolModelsTest` pins on the wire side, so a field added to
 * `native/src/dto` without reaching this layer fails here.
 */
class CompatibilityProjectionTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a user keeps the fields the facade gained`() {
        val user = json.decodeFromString<BridgeUser>(USER).toCompatibility()

        assertEquals("Some One", user.fullName)
        assertEquals(listOf("someone_alt"), user.usernames)
        assertEquals("+10000000000", user.phone)
        assertEquals(4242, user.photoId)
        assertEquals("offline", user.status)
        assertEquals(1_700_000_000_000, user.lastSeen)
        assertTrue(user.isSelf && user.contact && user.isBot && user.verified && user.restricted)
        assertTrue(!user.scam && !user.support && !user.statusByMe)
        assertEquals(
            listOf(RestrictionReason(listOf("all", "ios"), "spam", "Reported as spam")),
            user.restrictionReasons,
        )
    }

    @Test
    fun `a peer keeps the fields the facade gained and its opaque handle stays internal`() {
        val peer = json.decodeFromString<BridgePeer>(PEER_CHANNEL).toCompatibility()

        assertEquals("channel", peer.kind)
        assertEquals(listOf("channel_alt"), peer.usernames)
        assertTrue(peer.hasPhoto)
        assertNull(peer.isMegagroup)
        assertEquals(
            ChatPermissions(true, true, false, false, false, false, false, false, false, false),
            peer.permissions,
        )
    }

    @Test
    fun `a message projects its media and the fields the facade gained`() {
        val message = json.decodeFromString<BridgeMessage>(MESSAGE).toCompatibility()

        assertEquals(-1_000_007, message.peerId)
        assertEquals(7, message.senderId)
        assertEquals(1_700_000_000_000, message.date)
        assertTrue(message.mentioned && message.silent && message.pinned && message.fromScheduled)
        assertTrue(!message.fromChannelPost && !message.editHide && !message.mediaUnread)
        assertEquals(88, message.groupedId)
        assertEquals(2, message.reactionCount)
        assertEquals("document", message.media?.kind)
        assertEquals("report.pdf", message.media?.name)
    }

    @Test
    fun `a dialog projects the state the layer used to drop`() {
        val dialog = json.decodeFromString<BridgeDialog>(DIALOG).toCompatibility()

        assertTrue(dialog.pinned)
        assertEquals(31, dialog.topMessage)
        assertEquals(2, dialog.unreadCount)
        assertEquals(1, dialog.unreadMentionsCount)
        assertEquals("unsent", dialog.draftText)
        assertEquals(1, dialog.folderId)
        assertTrue(!dialog.isFolder)
        assertEquals("channel", dialog.peer.kind)
    }

    @Test
    fun `a folder dialog reports itself as one and carries no counts`() {
        val dialog = json.decodeFromString<BridgeDialog>(DIALOG_FOLDER).toCompatibility()

        assertTrue(dialog.isFolder)
        assertNull(dialog.unreadCount)
        assertNull(dialog.draftText)
    }

    @Test
    fun `a participant projects the role detail the layer used to drop`() {
        val participant = json.decodeFromString<BridgeParticipant>(PARTICIPANT).toCompatibility()

        assertEquals("admin", participant.role)
        assertEquals("Owner", participant.rank)
        assertEquals(1_700_000_000_000, participant.date)
        assertEquals(7, participant.invitedBy)
        assertEquals(8, participant.promotedBy)
        assertTrue(participant.canEdit == true)
        assertNull(participant.restrictions)
        assertTrue(participant.permissions?.changeInfo == true)
    }

    @Test
    fun `unknown media survives the projection with its kind`() {
        val media = json.decodeFromString<BridgeMedia>("""{"kind": "unknown"}""").toCompatibility()

        assertEquals("unknown", media.kind)
        assertNull(media.id)
    }

    private companion object {
        val USER = """
            {"id": 7, "username": "someone", "firstName": "Some", "lastName": "One",
             "fullName": "Some One", "usernames": ["someone_alt"], "phone": "+10000000000",
             "photoId": 4242, "status": "offline", "lastSeen": 1700000000000, "isSelf": true,
             "contact": true, "isBot": true, "verified": true, "restricted": true,
             "restrictionReasons": [{"platforms": ["all", "ios"], "reason": "spam",
                                     "text": "Reported as spam"}]}
        """.trimIndent()

        val PEER_CHANNEL = """
            {"nativeHandle": 12, "id": -1000007, "kind": "channel", "username": "channel",
             "name": "A Channel", "usernames": ["channel_alt"], "isMegagroup": null,
             "hasPhoto": true,
             "permissions": {"changeInfo": true, "postMessages": true, "editMessages": false,
                             "deleteMessages": false, "banUsers": false, "inviteUsers": false,
                             "pinMessages": false, "addAdmins": false, "anonymous": false,
                             "manageCall": false}}
        """.trimIndent()

        val MESSAGE = """
            {"id": 31, "text": "hello", "outgoing": true, "replyToMessageId": 30,
             "peerId": -1000007, "senderId": 7, "date": 1700000000000, "mentioned": true,
             "silent": true, "pinned": true, "fromScheduled": true, "groupedId": 88,
             "reactionCount": 2,
             "media": {"kind": "document", "id": 5150, "name": "report.pdf"}}
        """.trimIndent()

        val DIALOG = """
            {"peer": ${PEER_CHANNEL.trimIndent()}, "lastMessage": null, "pinned": true,
             "topMessage": 31, "unreadCount": 2, "unreadMentionsCount": 1, "draftText": "unsent",
             "folderId": 1, "isFolder": false}
        """.trimIndent()

        val DIALOG_FOLDER = """
            {"peer": ${PEER_CHANNEL.trimIndent()}, "lastMessage": null, "pinned": false,
             "topMessage": 0, "unreadCount": null, "unreadMentionsCount": null,
             "draftText": null, "folderId": null, "isFolder": true}
        """.trimIndent()

        val PARTICIPANT = """
            {"user": ${USER.trimIndent()}, "role": "admin", "rank": "Owner",
             "date": 1700000000000, "invitedBy": 7, "promotedBy": 8, "canEdit": true,
             "permissions": {"changeInfo": true}}
        """.trimIndent()
    }
}
