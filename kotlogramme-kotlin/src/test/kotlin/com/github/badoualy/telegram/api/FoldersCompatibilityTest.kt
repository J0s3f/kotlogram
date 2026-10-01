package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.ChatlistInviteResult as BridgeChatlistInviteResult
import org.kotlogramme.protocol.ChatlistUpdatesAck as BridgeChatlistUpdatesAck
import org.kotlogramme.protocol.ChatlistUpdatesResult as BridgeChatlistUpdatesResult
import org.kotlogramme.protocol.DialogFoldersResult as BridgeDialogFoldersResult
import org.kotlogramme.protocol.ExportedInvitesResult as BridgeExportedInvitesResult
import org.kotlogramme.protocol.LeaveChatlistSuggestionsResult as BridgeLeaveChatlistSuggestionsResult
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

    @Test
    fun `an exported invites result projects the invites and the objects`() {
        val invites =
            json.decodeFromString<BridgeExportedInvitesResult>(INVITES).toCompatibility()

        val invite = invites.invites.single()
        assertEquals("News", invite.title)
        assertEquals("AbCdEf", invite.slug)
        assertEquals(-1000001000007L, invite.peers.single().id)
        assertEquals("channel", invite.peers.single().kind)
        assertEquals("Some Channel", invites.chats.single().name)
        assertEquals(7L, invites.users.single().id)
    }

    @Test
    fun `a new invite projects into the unjoined case`() {
        val check =
            json.decodeFromString<BridgeChatlistInviteResult>(NEW_INVITE).toCompatibility()
                as FolderInviteCheck.New

        assertTrue(check.titleNoanimate)
        assertEquals("News", check.title)
        assertEquals("bold", check.titleEntities.single().type)
        assertEquals("\uD83D\uDCF0", check.emoticon)
        assertEquals(7L, check.peers.single().id)
    }

    @Test
    fun `an already joined invite projects into the joined case`() {
        val check =
            json.decodeFromString<BridgeChatlistInviteResult>(JOINED_INVITE).toCompatibility()
                as FolderInviteCheck.Already

        assertEquals(4, check.filterId)
        assertEquals("user", check.missingPeers.single().kind)
        assertEquals("channel", check.alreadyPeers.single().kind)
    }

    @Test
    fun `a chatlist updates result projects the missing peers and the objects`() {
        val updates =
            json.decodeFromString<BridgeChatlistUpdatesResult>(UPDATES).toCompatibility()

        assertEquals(7L, updates.missingPeers.single().id)
        assertEquals("Some One", updates.users.single().fullName)
    }

    @Test
    fun `an updates acknowledgement projects the ok flag and the peers`() {
        val ack = json.decodeFromString<BridgeChatlistUpdatesAck>(ACK).toCompatibility()

        assertTrue(ack.ok)
        assertEquals(7L, ack.peers.single().id)
    }

    @Test
    fun `a leave suggestions result projects the bare peers`() {
        val suggestions = json.decodeFromString<BridgeLeaveChatlistSuggestionsResult>(
            """{"peers": [{"id": 42, "kind": "user"}]}""",
        ).toCompatibility()

        assertEquals(42L, suggestions.peers.single().id)
        assertEquals("user", suggestions.peers.single().kind)
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

        val INVITES = """
            {"invites": [{"title": "News", "slug": "AbCdEf", "url": "https://t.me/+AbCdEf",
                          "peers": [{"id": -1000001000007, "kind": "channel"}]}],
             "chats": [{"nativeHandle": 3, "id": -7, "kind": "group", "name": "Some Channel"}],
             "users": [{"id": 7, "fullName": "Some One"}]}
        """.trimIndent()

        val NEW_INVITE = """
            {"kind": "new", "titleNoanimate": true, "title": "News",
             "titleEntities": [{"type": "bold", "offset": 0, "length": 4}],
             "emoticon": "\uD83D\uDCF0",
             "peers": [{"nativeHandle": 12, "id": 7, "kind": "user"}]}
        """.trimIndent()

        val JOINED_INVITE = """
            {"kind": "already", "filterId": 4,
             "missingPeers": [{"nativeHandle": 12, "id": 7, "kind": "user"}],
             "alreadyPeers": [{"nativeHandle": 3, "id": -1000001000007, "kind": "channel"}]}
        """.trimIndent()

        val UPDATES = """
            {"missingPeers": [{"nativeHandle": 12, "id": 7, "kind": "user"}],
             "chats": [], "users": [{"id": 7, "fullName": "Some One"}]}
        """.trimIndent()

        val ACK = """{"ok": true, "peers": [{"nativeHandle": 12, "id": 7, "kind": "user"}]}"""
    }
}
