package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.Dialog as BridgeDialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade's `Dialog` carries the metadata the plain listing projection
 * does not, and leaves it at its defaults when a listing comes without it.
 *
 * The documents are the same ones `DialogsProtocolTest` pins on the wire side, so a field added to
 * `native/src/dto/dialog_meta.rs` without reaching this layer fails here.
 */
class DialogsCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a metadata dialog projects the fields the facade gained`() {
        val dialog = json.decodeFromString<BridgeDialog>(WITH_META).toCompatibility()

        assertEquals(true, dialog.unreadMark)
        assertEquals(true, dialog.viewForumAsMessages)
        assertEquals(30, dialog.readInboxMaxId)
        assertEquals(29, dialog.readOutboxMaxId)
        assertEquals(3, dialog.unreadReactionsCount)
        assertEquals(44, dialog.pts)
        assertEquals(86_400, dialog.ttlPeriod)
        assertEquals(DialogNotifySettings(false, true, 1_700_000_000_000L), dialog.notifySettings)
        // The listing fields are still there.
        assertEquals(2, dialog.unreadCount)
        assertEquals(31, dialog.topMessage)
        assertEquals("channel", dialog.peer.kind)
    }

    @Test
    fun `a plain dialog leaves every metadata field at its default`() {
        val dialog = json.decodeFromString<BridgeDialog>(LISTING).toCompatibility()

        assertNull(dialog.unreadMark)
        assertNull(dialog.viewForumAsMessages)
        assertNull(dialog.readInboxMaxId)
        assertNull(dialog.readOutboxMaxId)
        assertNull(dialog.unreadReactionsCount)
        assertNull(dialog.notifySettings)
        assertNull(dialog.pts)
        assertNull(dialog.ttlPeriod)
        assertNull(dialog.folderTitle)
        assertNull(dialog.unreadMutedPeersCount)
        assertNull(dialog.unreadUnmutedMessagesCount)
    }

    @Test
    fun `a folder row projects its folder title and counters`() {
        val dialog = json.decodeFromString<BridgeDialog>(FOLDER_META).toCompatibility()

        assertTrue(dialog.isFolder)
        assertEquals("News", dialog.folderTitle)
        assertTrue(dialog.autofillNewBroadcasts == true)
        assertTrue(dialog.autofillPublicGroups == false)
        assertTrue(dialog.autofillNewCorrespondents == true)
        assertEquals(4, dialog.unreadMutedPeersCount)
        assertEquals(5, dialog.unreadUnmutedPeersCount)
        assertEquals(6, dialog.unreadMutedMessagesCount)
        assertEquals(7, dialog.unreadUnmutedMessagesCount)
        assertNull(dialog.notifySettings)
        assertNull(dialog.unreadMark)
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
    }
}
