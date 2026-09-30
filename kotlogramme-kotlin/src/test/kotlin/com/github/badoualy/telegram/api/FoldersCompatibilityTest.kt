package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.DialogFoldersResult as BridgeDialogFoldersResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade carries the dialog-filter projection the bridge gained,
 * including the registered peer handles and the write-back conversion.
 *
 * The document is the same one `FoldersProtocolTest` pins on the wire side, so a field added to the
 * native projection without reaching this layer fails here.
 */
class FoldersCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a folders result projects every flag and the peers`() {
        val folders = json.decodeFromString<BridgeDialogFoldersResult>(FOLDERS).toCompatibility()

        assertTrue(folders.tagsEnabled)
        val folder = folders.filters.single()
        assertEquals(2, folder.id)
        assertEquals("filter", folder.kind)
        assertEquals("News", folder.title)
        assertTrue(folder.contacts && folder.groups && folder.excludeMuted && folder.excludeArchived)
        assertTrue(!folder.nonContacts && !folder.broadcasts && !folder.bots && !folder.excludeRead)
        assertEquals(7, folder.pinnedPeers.single().id)
        assertTrue(folder.includePeers.isEmpty() && folder.excludePeers.isEmpty())
    }

    @Test
    fun `a chatlist folder keeps its kind and its invites flag`() {
        val folder =
            json.decodeFromString<BridgeDialogFoldersResult>(CHATLIST).toCompatibility().filters
                .single()

        assertEquals("chatlist", folder.kind)
        assertTrue(folder.hasMyInvites)
    }

    @Test
    fun `a spec converts its peers into the bridge targets`() {
        val peer =
            json.decodeFromString<BridgeDialogFoldersResult>(FOLDERS).toCompatibility()
                .filters.single().pinnedPeers.single()

        val bridge = DialogFolderSpec("News", contacts = true, pinnedPeers = listOf(peer)).toBridge()

        assertEquals("News", bridge.title)
        assertTrue(bridge.contacts)
        assertTrue(!bridge.groups)
        assertEquals(12, bridge.pinnedPeers.single().peerHandle)
        assertTrue(bridge.includePeers.isEmpty() && bridge.excludePeers.isEmpty())
    }

    private companion object {
        val FOLDERS = """
            {"tagsEnabled": true,
             "filters": [{"id": 2, "kind": "filter", "title": "News", "contacts": true,
                          "nonContacts": false, "groups": true, "broadcasts": false, "bots": false,
                          "excludeMuted": true, "excludeRead": false, "excludeArchived": true,
                          "hasMyInvites": false,
                          "pinnedPeers": [{"nativeHandle": 12, "id": 7, "kind": "user"}],
                          "includePeers": [], "excludePeers": []}]}
        """.trimIndent()

        val CHATLIST = """
            {"tagsEnabled": false,
             "filters": [{"id": 4, "kind": "chatlist", "title": "Chats", "hasMyInvites": true,
                          "pinnedPeers": [], "includePeers": [], "excludePeers": []}]}
        """.trimIndent()
    }
}
