package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the bridge models.
 *
 * Each document below is exactly what the Rust projection in `native/src/dto` emits, so a field
 * renamed on one side, a timestamp turned from milliseconds into seconds or a field added to one
 * projection only breaks a test here rather than a live session. The Rust side pins the same
 * documents in `native/src/dto/tests.rs`.
 */
class ProtocolModelsTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `a projected user decodes and re-encodes unchanged`() {
        roundTrips<User>(USER)
    }

    @Test
    fun `a projected channel peer decodes and re-encodes unchanged`() {
        val peer = roundTrips<Peer>(PEER_CHANNEL)
        assertEquals("channel", peer.kind)
        assertEquals(listOf("channel_alt"), peer.usernames)
    }

    @Test
    fun `a group peer reports its megagroup flag and carries no rights`() {
        val peer = roundTrips<Peer>(PEER_GROUP)
        assertEquals(true, peer.isMegagroup)
        assertNull(peer.permissions)
    }

    @Test
    fun `a projected message with a document decodes and re-encodes unchanged`() {
        val message = roundTrips<Message>(MESSAGE_WITH_DOCUMENT)
        assertEquals("document", message.media?.kind)
        assertEquals("application/pdf", message.media?.mimeType)
        assertEquals(1_700_000_000_000L, message.date)
    }

    @Test
    fun `a projected message without media decodes and re-encodes unchanged`() {
        val message = roundTrips<Message>(MESSAGE_WITHOUT_MEDIA)
        assertNull(message.media)
        assertNull(message.peerId)
    }

    @Test
    fun `a projected dialog decodes and re-encodes unchanged`() {
        val dialog = roundTrips<Dialog>(DIALOG)
        assertEquals("unsent", dialog.draftText)
        assertEquals(2, dialog.unreadCount)
        assertEquals(31, dialog.topMessage)
    }

    @Test
    fun `a folder dialog carries no counts and no draft`() {
        val dialog = roundTrips<Dialog>(DIALOG_FOLDER)
        assertTrue(dialog.isFolder)
        assertNull(dialog.unreadCount)
        assertNull(dialog.unreadMentionsCount)
        assertNull(dialog.draftText)
    }

    @Test
    fun `a projected admin participant decodes and re-encodes unchanged`() {
        val participant = roundTrips<Participant>(PARTICIPANT_ADMIN)
        assertEquals("admin", participant.role)
        assertEquals(true, participant.canEdit)
        assertEquals("Owner", participant.rank)
    }

    @Test
    fun `a banned participant carries restrictions and no rights`() {
        val participant = roundTrips<Participant>(PARTICIPANT_BANNED)
        assertEquals("banned", participant.role)
        assertNull(participant.permissions)
        assertEquals(1_700_000_000_000L, participant.restrictions?.untilDate)
        assertEquals(true, participant.restrictions?.sendMedia)
    }

    @Test
    fun `media declares exactly the fields the native projection emits`() {
        val names = (json.parseToJsonElement(MEDIA_DOCUMENT) as JsonObject).keys.sorted()
        assertEquals(MEDIA_FIELDS.sorted(), names)
    }

    @Test
    fun `unknown media still carries an object rather than nothing`() {
        val media = json.decodeFromString<Media>(MEDIA_UNKNOWN)
        assertEquals("unknown", media.kind)
        assertNull(media.id)
        assertNull(media.url)
    }

    @Test
    fun `the minimal documents an older bridge emits still decode`() {
        val user = json.decodeFromString<User>("""{"id": 7}""")
        assertEquals(7, user.id)
        assertEquals("", user.fullName)
        assertEquals("unknown", user.status)
        assertEquals(emptyList(), user.usernames)
        assertEquals(emptyList(), user.restrictionReasons)
        assertTrue(!user.isBot)

        val message = json.decodeFromString<Message>(
            """{"id": 31, "text": "hello", "outgoing": false, "replyToMessageId": null}""",
        )
        assertEquals("hello", message.text)
        assertNull(message.peerId)
        assertEquals(0L, message.date)
        assertTrue(!message.silent)
        assertNull(message.media)

        val peer = json.decodeFromString<Peer>(
            """{"nativeHandle": 1, "id": 2, "kind": "user", "username": "someone", "name": "Some One"}""",
        )
        assertEquals("user", peer.kind)
        assertEquals(emptyList(), peer.usernames)
        assertNull(peer.isMegagroup)
        assertTrue(!peer.hasPhoto)
        assertNull(peer.permissions)

        val dialog = json.decodeFromString<Dialog>(
            """{"peer": {"nativeHandle": 1, "id": 2, "kind": "user", "username": null, "name": null}}""",
        )
        assertNull(dialog.lastMessage)
        assertEquals(0, dialog.topMessage)
        assertTrue(!dialog.isFolder)

        val participant = json.decodeFromString<Participant>(
            """{"user": {"id": 7}, "role": "member"}""",
        )
        assertEquals("member", participant.role)
        assertNull(participant.permissions)
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
        val PERMISSIONS_OWNER = """
            {"changeInfo": true, "postMessages": true, "editMessages": false,
             "deleteMessages": false, "banUsers": false, "inviteUsers": false,
             "pinMessages": false, "addAdmins": false, "anonymous": false, "manageCall": false}
        """.trimIndent()

        val USER = """
            {"id": 7, "username": "someone", "firstName": "Some", "lastName": "One",
             "fullName": "Some One", "usernames": ["someone_alt"], "phone": "+10000000000",
             "photoId": 4242, "status": "offline", "statusExpires": null,
             "lastSeen": 1700000000000, "statusByMe": false, "langCode": "en", "isSelf": true,
             "contact": true, "mutualContact": false, "deleted": false, "isBot": true,
             "botPrivacy": true, "botSupportsChats": false, "botInlineGeo": false,
             "botInlinePlaceholder": "pick a bot", "verified": true, "restricted": true,
             "support": false, "scam": false,
             "restrictionReasons": [{"platforms": ["all", "ios"], "reason": "spam",
                                     "text": "Reported as spam"}]}
        """.trimIndent()

        val PEER_CHANNEL = """
            {"nativeHandle": 12, "id": -1000007, "kind": "channel", "username": "channel",
             "name": "A Channel", "usernames": ["channel_alt"], "isMegagroup": null,
             "hasPhoto": true, "permissions": ${PERMISSIONS_OWNER.trimIndent()}}
        """.trimIndent()

        val PEER_GROUP = """
            {"nativeHandle": 13, "id": -1000008, "kind": "group", "username": null,
             "name": "A Group", "usernames": [], "isMegagroup": true, "hasPhoto": false,
             "permissions": null}
        """.trimIndent()

        /** Every field of a media document, populated the way a document populates it. */
        val MEDIA_DOCUMENT = """
            {"kind": "document", "id": 5150, "size": 12345, "width": null, "height": null,
             "spoiler": false, "ttlSeconds": null, "name": "report.pdf",
             "mimeType": "application/pdf", "creationDate": 1700000000000, "duration": null,
             "resolutionWidth": null, "resolutionHeight": null, "audioTitle": null,
             "performer": null, "emoji": null, "isAnimated": false, "phoneNumber": null,
             "firstName": null, "lastName": null, "vcard": null, "question": null, "isQuiz": null,
             "closed": null, "totalVoters": null, "latitude": null, "longitude": null,
             "accuracyRadius": null, "title": null, "address": null, "provider": null,
             "venueId": null, "venueType": null, "heading": null, "period": null,
             "proximityNotificationRadius": null, "value": null, "url": null, "displayUrl": null,
             "siteName": null, "description": null, "pageType": null, "author": null}
        """.trimIndent()

        val MEDIA_UNKNOWN = """
            {"kind": "unknown", "id": null, "size": null, "width": null, "height": null,
             "spoiler": null, "ttlSeconds": null, "name": null, "mimeType": null,
             "creationDate": null, "duration": null, "resolutionWidth": null,
             "resolutionHeight": null, "audioTitle": null, "performer": null, "emoji": null,
             "isAnimated": null, "phoneNumber": null, "firstName": null, "lastName": null,
             "vcard": null, "question": null, "isQuiz": null, "closed": null, "totalVoters": null,
             "latitude": null, "longitude": null, "accuracyRadius": null, "title": null,
             "address": null, "provider": null, "venueId": null, "venueType": null,
             "heading": null, "period": null, "proximityNotificationRadius": null, "value": null,
             "url": null, "displayUrl": null, "siteName": null, "description": null,
             "pageType": null, "author": null}
        """.trimIndent()

        val MESSAGE_WITH_DOCUMENT = """
            {"id": 31, "text": "hello", "outgoing": true, "replyToMessageId": 30,
             "peerId": -1000007, "senderId": 7, "date": 1700000000000, "editDate": 1700000060000,
             "mentioned": true, "mediaUnread": false, "silent": true, "pinned": true,
             "fromChannelPost": false, "fromScheduled": true, "editHide": false, "viaBotId": 99,
             "postAuthor": "Author", "groupedId": 88, "viewCount": 5, "forwardCount": 4,
             "replyCount": 3, "reactionCount": 2, "media": ${MEDIA_DOCUMENT.trimIndent()}}
        """.trimIndent()

        val MESSAGE_WITHOUT_MEDIA = """
            {"id": 1, "text": "", "outgoing": false, "replyToMessageId": null, "peerId": null,
             "senderId": null, "date": 1700000000000, "editDate": null, "mentioned": false,
             "mediaUnread": false, "silent": false, "pinned": false, "fromChannelPost": false,
             "fromScheduled": false, "editHide": false, "viaBotId": null, "postAuthor": null,
             "groupedId": null, "viewCount": null, "forwardCount": null, "replyCount": null,
             "reactionCount": null, "media": null}
        """.trimIndent()

        val DIALOG = """
            {"peer": ${PEER_CHANNEL.trimIndent()},
             "lastMessage": ${MESSAGE_WITHOUT_MEDIA.trimIndent()},
             "pinned": true, "topMessage": 31, "unreadCount": 2, "unreadMentionsCount": 1,
             "draftText": "unsent", "folderId": 1, "isFolder": false}
        """.trimIndent()

        val DIALOG_FOLDER = """
            {"peer": ${PEER_GROUP.trimIndent()}, "lastMessage": null, "pinned": false,
             "topMessage": 0, "unreadCount": null, "unreadMentionsCount": null,
             "draftText": null, "folderId": null, "isFolder": true}
        """.trimIndent()

        val PARTICIPANT_ADMIN = """
            {"user": ${USER.trimIndent()}, "role": "admin", "rank": "Owner",
             "date": 1700000000000, "invitedBy": 7, "promotedBy": 8, "kickedBy": null,
             "canEdit": true, "left": null, "permissions": ${PERMISSIONS_OWNER.trimIndent()},
             "restrictions": null}
        """.trimIndent()

        val PARTICIPANT_BANNED = """
            {"user": ${USER.trimIndent()}, "role": "banned", "rank": null,
             "date": 1700000000000, "invitedBy": null, "promotedBy": null, "kickedBy": 8,
             "canEdit": null, "left": true, "permissions": null,
             "restrictions": {"viewMessages": true, "sendMessages": false, "sendMedia": true,
                              "sendStickers": false, "sendGifs": true, "sendGames": false,
                              "sendInline": true, "embedLinks": false, "sendPolls": true,
                              "changeInfo": false, "inviteUsers": true, "pinMessages": false,
                              "untilDate": 1700000000000}}
        """.trimIndent()

        /** The names a media document carries, kept in step with `MEDIA_FIELDS` in the Rust tests. */
        val MEDIA_FIELDS = listOf(
            "accuracyRadius", "address", "author", "audioTitle", "closed", "creationDate",
            "description", "displayUrl", "duration", "emoji", "firstName", "heading", "height",
            "id", "isAnimated", "isQuiz", "kind", "latitude", "lastName", "longitude", "mimeType",
            "name", "pageType", "performer", "period", "phoneNumber", "provider",
            "proximityNotificationRadius", "question", "resolutionHeight", "resolutionWidth",
            "siteName", "size", "spoiler", "title", "totalVoters", "ttlSeconds", "url", "value",
            "venueId", "venueType", "vcard", "width",
        )
    }
}
