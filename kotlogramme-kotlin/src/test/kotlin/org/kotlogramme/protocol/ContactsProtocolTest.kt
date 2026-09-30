package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the typed contacts models.
 *
 * Each document below is exactly what the Rust projection in `native/src/dto/contacts.rs` emits,
 * so a field renamed on one side breaks a test here rather than a live session. The Rust side pins
 * the same documents in its own test module.
 */
class ContactsProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** The wire codec, which drops a field whose value is its default. */
    private val plain = Json { ignoreUnknownKeys = true }

    @Test
    fun `a contacts page decodes the entries and the users`() {
        val page = json.decodeFromString<ContactsPage>(PAGE)

        assertEquals(false, page.notModified)
        assertEquals(1, page.savedCount)
        assertEquals(7, page.contacts[0].userId)
        assertTrue(page.contacts[0].mutual)
        assertEquals("Some One", page.contacts[0].user?.fullName)
        assertEquals(7, page.contacts[0].peer?.id)
        assertEquals(1, page.users.size)
        assertEquals(12, page.users[0].peer.nativeHandle)
    }

    @Test
    fun `a contacts page round-trips through the wire codec`() {
        val decoded = plain.decodeFromString<ContactsPage>(PAGE)
        assertEquals(decoded, plain.decodeFromString(plain.encodeToString(decoded)))
    }

    @Test
    fun `a not-modified page carries no list`() {
        val page = json.decodeFromString<ContactsPage>(NOT_MODIFIED)
        assertTrue(page.notModified)
        assertEquals(0, page.savedCount)
        assertTrue(page.contacts.isEmpty() && page.users.isEmpty())
    }

    @Test
    fun `an empty contacts page decodes with defaults`() {
        val page = plain.decodeFromString<ContactsPage>("{}")
        assertEquals(false, page.notModified)
        assertEquals(0, page.savedCount)
        assertTrue(page.contacts.isEmpty())
    }

    @Test
    fun `an imported-contacts result decodes the imports and retries`() {
        val result = json.decodeFromString<ImportedContacts>(IMPORTED)

        assertEquals(1, result.imported.size)
        assertEquals(7, result.imported[0].userId)
        assertEquals(42, result.imported[0].clientId)
        assertEquals(listOf(43L), result.retryContacts)
        assertEquals(44, result.popularInvites[0].clientId)
        assertEquals(3, result.popularInvites[0].importers)
    }

    @Test
    fun `a blocked result decodes the count and the dates`() {
        val result = json.decodeFromString<BlockedContacts>(BLOCKED)

        assertEquals(9, result.count)
        assertEquals(1_700_000_000_000, result.blocked[0].date)
        assertEquals("Some One", result.blocked[0].user?.fullName)
        assertEquals(7, result.blocked[0].peer.id)
    }

    @Test
    fun `a full blocked result leaves the count null`() {
        val result = plain.decodeFromString<BlockedContacts>("""{"blocked": []}""")
        assertNull(result.count)
    }

    @Test
    fun `a found result decodes both result sets`() {
        val result = json.decodeFromString<FoundContacts>(FOUND)

        assertEquals(1, result.myResults.size)
        assertEquals(7, result.myResults[0].peer.id)
        assertTrue(result.results.isEmpty())
        assertEquals(1, result.users.size)
    }

    private companion object {
        val PAGE = """
            {"notModified": false, "savedCount": 1,
             "contacts": [{"userId": 7, "mutual": true,
                           "user": {"id": 7, "fullName": "Some One"},
                           "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}],
             "users": [{"user": {"id": 7, "fullName": "Some One"},
                        "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}]}
        """.trimIndent()

        val NOT_MODIFIED = """{"notModified": true, "savedCount": 0, "contacts": [], "users": []}"""

        val IMPORTED = """
            {"imported": [{"userId": 7, "clientId": 42, "user": {"id": 7},
                           "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}],
             "retryContacts": [43],
             "popularInvites": [{"clientId": 44, "importers": 3}],
             "users": []}
        """.trimIndent()

        val BLOCKED = """
            {"count": 9,
             "blocked": [{"date": 1700000000000, "user": {"id": 7, "fullName": "Some One"},
                          "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}],
             "users": []}
        """.trimIndent()

        val FOUND = """
            {"myResults": [{"user": {"id": 7},
                            "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}],
             "results": [],
             "users": [{"user": {"id": 7}, "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}]}
        """.trimIndent()
    }
}
