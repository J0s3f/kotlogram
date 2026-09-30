package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.BlockedContacts as BridgeBlockedContacts
import org.kotlogramme.protocol.ContactsPage as BridgeContactsPage
import org.kotlogramme.protocol.FoundContacts as BridgeFoundContacts
import org.kotlogramme.protocol.ImportedContacts as BridgeImportedContacts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade carries the contacts projection the bridge gained, including
 * the registered peer handles.
 *
 * The documents are the same ones `ContactsProtocolTest` pins on the wire side, so a field added
 * to the native projection without reaching this layer fails here.
 */
class ContactsCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a contacts page projects the entries and the handles`() {
        val page = json.decodeFromString<BridgeContactsPage>(PAGE).toCompatibility()

        assertEquals(1, page.savedCount)
        assertEquals(false, page.notModified)
        assertEquals(7, page.contacts[0].userId)
        assertTrue(page.contacts[0].mutual)
        assertEquals("Some One", page.contacts[0].user?.fullName)
        assertEquals(7, page.contacts[0].peer?.id)
        assertEquals(1, page.users.size)
        assertEquals("Some One", page.users[0].user.fullName)
    }

    @Test
    fun `a not-modified page keeps the flag`() {
        val page = json.decodeFromString<BridgeContactsPage>(NOT_MODIFIED).toCompatibility()
        assertTrue(page.notModified)
        assertTrue(page.contacts.isEmpty())
    }

    @Test
    fun `an imported-contacts result projects the imports and retries`() {
        val result = json.decodeFromString<BridgeImportedContacts>(IMPORTED).toCompatibility()

        assertEquals(1, result.imported.size)
        assertEquals(7, result.imported[0].userId)
        assertEquals(42, result.imported[0].clientId)
        assertEquals(listOf(43L), result.retryContacts)
        assertEquals(3, result.popularInvites[0].importers)
    }

    @Test
    fun `a blocked result projects the peers and keeps the count`() {
        val result = json.decodeFromString<BridgeBlockedContacts>(BLOCKED).toCompatibility()

        assertEquals(9, result.count)
        assertEquals(1_700_000_000_000, result.blocked[0].date)
        assertEquals(7, result.blocked[0].peer.id)
    }

    @Test
    fun `a full blocked result keeps a null count`() {
        val result =
            json.decodeFromString<BridgeBlockedContacts>("""{"blocked": []}""").toCompatibility()
        assertNull(result.count)
    }

    @Test
    fun `a found result projects both result sets`() {
        val result = json.decodeFromString<BridgeFoundContacts>(FOUND).toCompatibility()

        assertEquals(1, result.myResults.size)
        assertEquals(7, result.myResults[0].peer.id)
        assertTrue(result.results.isEmpty())
    }

    @Test
    fun `an import request crosses to the bridge unchanged`() {
        val request = ContactImport(42, "+10000000000", "Some", "One").toBridge()

        assertEquals(42, request.clientId)
        assertEquals("+10000000000", request.phone)
        assertEquals("Some", request.firstName)
        assertEquals("One", request.lastName)
    }

    private companion object {
        val PAGE = """
            {"notModified": false, "savedCount": 1,
             "contacts": [{"userId": 7, "mutual": true, "user": {"id": 7, "fullName": "Some One"},
                           "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}],
             "users": [{"user": {"id": 7, "fullName": "Some One"},
                        "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}]}
        """.trimIndent()

        val NOT_MODIFIED = """{"notModified": true, "savedCount": 0, "contacts": [], "users": []}"""

        val IMPORTED = """
            {"imported": [{"userId": 7, "clientId": 42}],
             "retryContacts": [43],
             "popularInvites": [{"clientId": 44, "importers": 3}],
             "users": []}
        """.trimIndent()

        val BLOCKED = """
            {"count": 9,
             "blocked": [{"date": 1700000000000, "user": {"id": 7},
                          "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}],
             "users": []}
        """.trimIndent()

        val FOUND = """
            {"myResults": [{"user": {"id": 7},
                            "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}],
             "results": [], "users": []}
        """.trimIndent()
    }
}
