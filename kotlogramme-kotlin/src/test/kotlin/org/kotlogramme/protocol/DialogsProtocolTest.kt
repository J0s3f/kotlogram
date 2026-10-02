package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the dialog metadata models and payloads.
 *
 * Each document below is exactly what the Rust side emits in `native/src/dto/dialog_meta.rs` and
 * `native/src/ops/dialogs.rs`, so a field renamed on one side or a nested object renamed breaks a
 * test here rather than a live session. The Rust side pins the same documents in its own test
 * modules.
 */
class DialogsProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** The transport's own codec, which leaves a field at its default out of a request payload. */
    private val requests = Json { ignoreUnknownKeys = true }

    @Test
    fun `a listing document decodes without metadata and re-encodes unchanged`() {
        // `getDialogs` never emits `meta`; because it is encoded only when set, re-encoding a plain
        // listing must reproduce the document exactly.
        val dialog = json.decodeFromString<Dialog>(LISTING)

        assertNull(dialog.meta)
        assertEquals(
            json.parseToJsonElement(LISTING),
            json.parseToJsonElement(json.encodeToString(dialog)),
            "a plain listing gained a field",
        )
    }

    @Test
    fun `a metadata document carries the state the listing leaves out`() {
        val dialog = roundTrips<Dialog>(WITH_META)

        assertTrue(dialog.pinned)
        assertEquals(2, dialog.unreadCount)
        val meta = assertNotNull(dialog.meta)
        assertEquals(true, meta.unreadMark)
        assertEquals(true, meta.viewForumAsMessages)
        assertEquals(30, meta.readInboxMaxId)
        assertEquals(29, meta.readOutboxMaxId)
        assertEquals(3, meta.unreadReactionsCount)
        assertEquals(44, meta.pts)
        assertEquals(86_400, meta.ttlPeriod)
        assertEquals(1_700_000_000_000L, meta.notifySettings?.muteUntil)
        assertEquals(false, meta.notifySettings?.showPreviews)
        assertEquals(true, meta.notifySettings?.silent)
        // A regular dialog leaves every folder-only field unset.
        assertNull(meta.folderTitle)
        assertNull(meta.unreadMutedPeersCount)
        assertNull(meta.unreadUnmutedMessagesCount)
    }

    @Test
    fun `a folder row carries the folder title and counters and no notify settings`() {
        val dialog = roundTrips<Dialog>(FOLDER_META)

        assertTrue(dialog.isFolder)
        val meta = assertNotNull(dialog.meta)
        assertEquals("News", meta.folderTitle)
        assertEquals(true, meta.autofillNewBroadcasts)
        assertEquals(false, meta.autofillPublicGroups)
        assertEquals(true, meta.autofillNewCorrespondents)
        assertEquals(4, meta.unreadMutedPeersCount)
        assertEquals(5, meta.unreadUnmutedPeersCount)
        assertEquals(6, meta.unreadMutedMessagesCount)
        assertEquals(7, meta.unreadUnmutedMessagesCount)
        assertNull(meta.notifySettings)
        assertNull(meta.unreadMark)
    }

    @Test
    fun `the metadata declares exactly the fields the native projection emits`() {
        val meta = assertNotNull(json.decodeFromString<Dialog>(WITH_META).meta)
        assertEquals(
            META_FIELDS.sorted(),
            (json.parseToJsonElement(json.encodeToString(meta)) as JsonObject).keys.sorted(),
        )
    }

    @Test
    fun `an older bridge document still decodes with no metadata`() {
        val dialog = json.decodeFromString<Dialog>(
            """{"peer": {"nativeHandle": 1, "id": 2, "kind": "user", "username": null, "name": null}}""",
        )

        assertNull(dialog.meta)
        assertNull(dialog.lastMessage)
        assertEquals(0, dialog.topMessage)
    }

    @Test
    fun `the total result decodes and the limit payload names its field`() {
        assertEquals(7L, requests.decodeFromString<DialogsTotal>("""{"total":7}""").total)
        assertEquals("""{"limit":25}""", requests.encodeToString(LimitPayload(25)))
    }

    @Test
    fun `the dialog cursor payload carries the three cursor values together`() {
        // Without a cursor the payload is exactly what it always was.
        assertEquals("""{"limit":25}""", requests.encodeToString(LimitPayload(25)))
        // The cursor travels as the three values the native side applies as one.
        assertEquals(
            """{"limit":25,"offsetPeer":-1000007,"offsetId":31,"offsetDate":1700000000000}""",
            requests.encodeToString(LimitPayload(25, -100_000_7, 31, 1_700_000_000_000)),
        )
        // The cursor decodes back from the wire.
        val payload = requests.decodeFromString<LimitPayload>(
            """{"limit":25,"offsetPeer":-1000007,"offsetId":31,"offsetDate":1700000000000}""",
        )
        assertEquals(-100_000_7, payload.offsetPeer)
        assertEquals(31, payload.offsetId)
        assertEquals(1_700_000_000_000, payload.offsetDate)
    }

    @Test
    fun `the dialog payload carries the all flag`() {
        // Absent and explicit false both mean "one page", which is the old behaviour.
        assertEquals("""{"limit":25}""", requests.encodeToString(LimitPayload(25)))
        assertEquals("""{"limit":25}""", requests.encodeToString(LimitPayload(25, all = false)))
        // `all` travels as the wire's `all` and wins over the limit and cursor.
        assertEquals(
            """{"limit":25,"all":true}""",
            requests.encodeToString(LimitPayload(25, all = true)),
        )
        val payload = requests.decodeFromString<LimitPayload>("""{"limit":25,"all":true}""")
        assertTrue(payload.all)
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
        val PEER = """
            {"nativeHandle": 12, "id": -1000007, "kind": "channel", "username": "channel",
             "name": "A Channel", "usernames": [], "isMegagroup": null, "hasPhoto": false,
             "permissions": null}
        """.trimIndent()

        val LISTING = """
            {"peer": ${PEER.trimIndent()}, "lastMessage": null, "pinned": true, "topMessage": 31,
             "unreadCount": 2, "unreadMentionsCount": 1, "draftText": null, "folderId": 1,
             "isFolder": false}
        """.trimIndent()

        val WITH_META = """
            {"peer": ${PEER.trimIndent()}, "lastMessage": null, "pinned": true, "topMessage": 31,
             "unreadCount": 2, "unreadMentionsCount": 1, "draftText": null, "folderId": 1,
             "isFolder": false,
             "meta": {"unreadMark": true, "viewForumAsMessages": true, "readInboxMaxId": 30,
                      "readOutboxMaxId": 29, "unreadReactionsCount": 3,
                      "notifySettings": {"showPreviews": false, "silent": true,
                                         "muteUntil": 1700000000000},
                      "pts": 44, "ttlPeriod": 86400, "folderTitle": null,
                      "autofillNewBroadcasts": null, "autofillPublicGroups": null,
                      "autofillNewCorrespondents": null, "unreadMutedPeersCount": null,
                      "unreadUnmutedPeersCount": null, "unreadMutedMessagesCount": null,
                      "unreadUnmutedMessagesCount": null}}
        """.trimIndent()

        val FOLDER_META = """
            {"peer": ${PEER.trimIndent()}, "lastMessage": null, "pinned": true, "topMessage": 0,
             "unreadCount": null, "unreadMentionsCount": null, "draftText": null, "folderId": null,
             "isFolder": true,
             "meta": {"unreadMark": null, "viewForumAsMessages": null, "readInboxMaxId": null,
                      "readOutboxMaxId": null, "unreadReactionsCount": null,
                      "notifySettings": null, "pts": null, "ttlPeriod": null,
                      "folderTitle": "News", "autofillNewBroadcasts": true,
                      "autofillPublicGroups": false, "autofillNewCorrespondents": true,
                      "unreadMutedPeersCount": 4, "unreadUnmutedPeersCount": 5,
                      "unreadMutedMessagesCount": 6, "unreadUnmutedMessagesCount": 7}}
        """.trimIndent()

        /** The names a metadata document carries, kept in step with the native projection. */
        val META_FIELDS = listOf(
            "autofillNewBroadcasts", "autofillNewCorrespondents", "autofillPublicGroups",
            "folderTitle", "notifySettings", "pts", "readInboxMaxId", "readOutboxMaxId",
            "ttlPeriod", "unreadMark", "unreadMutedMessagesCount", "unreadMutedPeersCount",
            "unreadReactionsCount", "unreadUnmutedMessagesCount", "unreadUnmutedPeersCount",
            "viewForumAsMessages",
        )
    }
}
