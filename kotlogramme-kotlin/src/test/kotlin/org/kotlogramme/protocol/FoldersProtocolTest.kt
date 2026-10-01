package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the typed dialog-filter models.
 *
 * Each document below is exactly what the Rust projection in `native/src/dto/folders.rs` emits, so a
 * field renamed on one side breaks a test here rather than a live session. The Rust side pins the
 * same documents in its own test module.
 */
class FoldersProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** The wire codec, which drops a field whose value is its default. */
    private val plain = Json { ignoreUnknownKeys = true }

    @Test
    fun `a folders result decodes the tags flag and the filters`() {
        val result = json.decodeFromString<DialogFoldersResult>(FOLDERS)

        assertTrue(result.tagsEnabled)
        assertEquals(1, result.filters.size)
        val folder = result.filters[0]
        assertEquals(2, folder.id)
        assertEquals("filter", folder.kind)
        assertEquals("News", folder.title)
        assertTrue(folder.contacts && folder.groups && folder.excludeMuted && folder.excludeArchived)
        assertTrue(!folder.nonContacts && !folder.broadcasts && !folder.bots && !folder.excludeRead)
        assertEquals(7, folder.pinnedPeers[0].id)
        assertEquals(12, folder.pinnedPeers[0].nativeHandle)
        assertTrue(folder.includePeers.isEmpty() && folder.excludePeers.isEmpty())
    }

    @Test
    fun `a folders result round-trips through the wire codec`() {
        val decoded = plain.decodeFromString<DialogFoldersResult>(FOLDERS)
        assertEquals(decoded, plain.decodeFromString(plain.encodeToString(decoded)))
    }

    @Test
    fun `an empty folders result decodes with defaults`() {
        val result = plain.decodeFromString<DialogFoldersResult>("{}")
        assertEquals(false, result.tagsEnabled)
        assertTrue(result.filters.isEmpty())
    }

    @Test
    fun `a chatlist folder carries its kind and no excludes`() {
        val result = json.decodeFromString<DialogFoldersResult>(CHATLIST)
        val folder = result.filters[0]

        assertEquals("chatlist", folder.kind)
        assertTrue(folder.hasMyInvites)
        assertTrue(folder.excludePeers.isEmpty())
    }

    @Test
    fun `an update with a filter reads the spec`() {
        val payload = json.decodeFromString<UpdateDialogFilterPayload>(UPDATE)

        assertEquals(2, payload.id)
        val spec = payload.filter!!
        assertEquals("News", spec.title)
        assertTrue(spec.contacts && spec.excludeRead)
        assertEquals(12, spec.includePeers[0].peerHandle)
    }

    @Test
    fun `an update without a filter is the delete form`() {
        assertNull(plain.decodeFromString<UpdateDialogFilterPayload>("""{"id": 2}""").filter)
        assertNull(
            plain.decodeFromString<UpdateDialogFilterPayload>("""{"id": 2, "filter": null}""")
                .filter,
        )
    }

    @Test
    fun `a spec without peers decodes with empty lists and false flags`() {
        val spec = json.decodeFromString<UpdateDialogFilterPayload>(BARE).filter!!

        assertEquals("Bare", spec.title)
        assertTrue(!spec.contacts && !spec.nonContacts && !spec.groups && !spec.broadcasts)
        assertTrue(spec.pinnedPeers.isEmpty() && spec.includePeers.isEmpty())
    }

    @Test
    fun `an order payload reads the id list`() {
        val payload = json.decodeFromString<UpdateDialogFiltersOrderPayload>("""{"order": [3, 1, 2]}""")
        assertEquals(listOf(3, 1, 2), payload.order)
    }

    @Test
    fun `an exported invites result decodes the invites and the objects`() {
        val result = json.decodeFromString<ExportedInvitesResult>(INVITES)

        val invite = result.invites.single()
        assertEquals("News", invite.title)
        assertEquals("AbCdEf", invite.slug)
        assertEquals("https://t.me/+AbCdEf", invite.url)
        assertEquals(-1000001000007L, invite.peers.single().id)
        assertEquals("channel", invite.peers.single().kind)
        assertEquals("Some Channel", result.chats.single().name)
        assertEquals(7L, result.users.single().id)
    }

    @Test
    fun `an exported invites result round-trips through the wire codec`() {
        val decoded = plain.decodeFromString<ExportedInvitesResult>(INVITES)
        assertEquals(decoded, plain.decodeFromString(plain.encodeToString(decoded)))
    }

    @Test
    fun `a new invite result carries its title entities and emoticon`() {
        val result = json.decodeFromString<ChatlistInviteResult>(NEW_INVITE)

        assertEquals("new", result.kind)
        assertTrue(result.titleNoanimate)
        assertEquals("News", result.title)
        assertEquals("bold", result.titleEntities.single().type)
        assertEquals("\uD83D\uDCF0", result.emoticon)
        assertEquals(7L, result.peers.single().id)
        assertTrue(result.missingPeers.isEmpty() && result.alreadyPeers.isEmpty())
    }

    @Test
    fun `an already joined invite result carries the folder id and the sorted peers`() {
        val result = json.decodeFromString<ChatlistInviteResult>(JOINED_INVITE)

        assertEquals("already", result.kind)
        assertEquals(4, result.filterId)
        assertTrue(result.missingPeers.single().kind == "user")
        assertTrue(result.alreadyPeers.single().kind == "channel")
        assertEquals("", result.title)
        assertTrue(result.titleEntities.isEmpty())
    }

    @Test
    fun `a chatlist updates result decodes the missing peers`() {
        val result = json.decodeFromString<ChatlistUpdatesResult>(UPDATES)

        assertEquals(7L, result.missingPeers.single().id)
        assertEquals("Some One", result.users.single().fullName)
    }

    @Test
    fun `an updates acknowledgement decodes the ok flag and the peers`() {
        val ack = json.decodeFromString<ChatlistUpdatesAck>(ACK)

        assertTrue(ack.ok)
        assertEquals(7L, ack.peers.single().id)
    }

    @Test
    fun `a leave suggestions result decodes the bare peers`() {
        val result = json.decodeFromString<LeaveChatlistSuggestionsResult>(
            """{"peers": [{"id": 42, "kind": "user"}, {"id": -7, "kind": "chat"}]}""",
        )

        assertEquals(2, result.peers.size)
        assertEquals(42L, result.peers[0].id)
        assertEquals("chat", result.peers[1].kind)
    }

    @Test
    fun `an edit payload leaves an absent title and peers null`() {
        val payload = json.decodeFromString<EditExportedInvitePayload>(
            """{"filterId": 4, "slug": "AbCdEf"}""",
        )

        assertEquals(4, payload.filterId)
        assertEquals("AbCdEf", payload.slug)
        assertNull(payload.title)
        assertNull(payload.peers)
    }

    @Test
    fun `an export payload reads the title and the peers`() {
        val payload = json.decodeFromString<ExportChatlistInvitePayload>(
            """{"filterId": 4, "title": "News", "peers": [{"peerHandle": 12}]}""",
        )

        assertEquals("News", payload.title)
        assertEquals(12L, payload.peers.single().peerHandle)
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

        val UPDATE = """
            {"id": 2, "filter": {"title": "News", "contacts": true, "excludeRead": true,
                                 "includePeers": [{"peerHandle": 12}]}}
        """.trimIndent()

        val BARE = """{"id": 5, "filter": {"title": "Bare"}}"""

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
             "peers": [{"nativeHandle": 12, "id": 7, "kind": "user"}],
             "chats": [], "users": []}
        """.trimIndent()

        val JOINED_INVITE = """
            {"kind": "already", "filterId": 4,
             "missingPeers": [{"nativeHandle": 12, "id": 7, "kind": "user"}],
             "alreadyPeers": [{"nativeHandle": 3, "id": -1000001000007, "kind": "channel"}],
             "chats": [], "users": []}
        """.trimIndent()

        val UPDATES = """
            {"missingPeers": [{"nativeHandle": 12, "id": 7, "kind": "user"}],
             "chats": [], "users": [{"id": 7, "fullName": "Some One"}]}
        """.trimIndent()

        val ACK = """
            {"ok": true, "peers": [{"nativeHandle": 12, "id": 7, "kind": "user"}]}
        """.trimIndent()
    }
}
