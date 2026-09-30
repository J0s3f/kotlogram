package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the user-by-id lookup.
 *
 * The documents below are exactly what the Rust handler in `native/src/ops/users.rs` sends and
 * emits: the `ids` payload and the contacts-shaped list of resolved accounts.
 */
class UsersProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** The wire codec, which drops a field whose value is its default. */
    private val plain = Json { ignoreUnknownKeys = true }

    @Test
    fun `a get-users payload carries the ids in order and tolerates duplicates`() {
        val encoded = plain.encodeToString(GetUsersPayload(listOf(7, 8, 7)))
        assertEquals("""{"ids":[7,8,7]}""", encoded)
    }

    @Test
    fun `an empty id list encodes as an empty array`() {
        assertEquals("""{"ids":[]}""", plain.encodeToString(GetUsersPayload(emptyList())))
    }

    @Test
    fun `a resolved-user list decodes the accounts and the handles`() {
        val users = json.decodeFromString<List<ContactUser>>(RESOLVED)

        assertEquals(2, users.size)
        assertEquals(7, users[0].user.id)
        assertEquals("bold", users[0].user.username)
        assertEquals("Bold", users[0].user.fullName)
        assertEquals(12, users[0].peer.nativeHandle)
        assertEquals(8, users[1].user.id)
    }

    @Test
    fun `an answer that resolved nothing decodes to an empty list`() {
        assertTrue(plain.decodeFromString<List<ContactUser>>("[]").isEmpty())
    }

    private companion object {
        val RESOLVED = """
            [{"user": {"id": 7, "username": "bold", "fullName": "Bold"},
              "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}},
             {"user": {"id": 8, "username": "quiet", "fullName": "Quiet"},
              "peer": {"nativeHandle": 13, "id": 8, "kind": "user"}}]
        """.trimIndent()
    }
}
