package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.ContactUser as BridgeContactUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade carries the user-by-id projection the bridge gained.
 *
 * The documents are the same ones `UsersProtocolTest` pins on the wire side, and the mapping is the
 * one `UsersApi.usersGetUsers` applies, so a field added to the native projection without reaching
 * this layer fails here.
 */
class UsersCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a resolved-user list projects to the compatibility accounts`() {
        val users = json.decodeFromString<List<BridgeContactUser>>(RESOLVED)
            .map { it.user.toCompatibility() }

        assertEquals(2, users.size)
        assertEquals(7, users[0].id)
        assertEquals("bold", users[0].username)
        assertEquals("Bold", users[0].fullName)
        assertEquals("quiet", users[1].username)
    }

    @Test
    fun `an id that resolved to nothing is absent from the answer`() {
        val users = json.decodeFromString<List<BridgeContactUser>>(ONE_RESOLVED)
            .map { it.user.toCompatibility() }

        assertEquals(1, users.size)
        assertEquals(7, users[0].id)
    }

    @Test
    fun `an empty answer projects to an empty list`() {
        val users = json.decodeFromString<List<BridgeContactUser>>("[]")
            .map { it.user.toCompatibility() }

        assertTrue(users.isEmpty())
    }

    private companion object {
        val RESOLVED = """
            [{"user": {"id": 7, "username": "bold", "fullName": "Bold"},
              "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}},
             {"user": {"id": 8, "username": "quiet", "fullName": "Quiet"},
              "peer": {"nativeHandle": 13, "id": 8, "kind": "user"}}]
        """.trimIndent()

        val ONE_RESOLVED = """
            [{"user": {"id": 7, "username": "bold", "fullName": "Bold"},
              "peer": {"nativeHandle": 12, "id": 7, "kind": "user"}}]
        """.trimIndent()
    }
}
