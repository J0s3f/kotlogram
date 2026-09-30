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
    }
}
