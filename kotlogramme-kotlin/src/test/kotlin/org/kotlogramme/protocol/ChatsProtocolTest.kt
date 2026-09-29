package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the chat-moderation models.
 *
 * Each document below is exactly what the Rust projection in `native/src/dto` and
 * `native/src/ops/chats.rs` emits, so a field renamed on one side or a flag turned the other way
 * round breaks a test here rather than a live session. The Rust side pins the same documents in its
 * own test modules.
 */
class ChatsProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** The wire codec, which drops a field whose value is its default. */
    private val plain = Json { ignoreUnknownKeys = true }

    @Test
    fun `a participant page decodes the listing and the total`() {
        val page = json.decodeFromString<ParticipantsResult>(PARTICIPANTS)

        assertEquals(1, page.participants.size)
        assertEquals("member", page.participants[0].role)
        assertEquals(7, page.participants[0].user.id)
        assertEquals(1_700_000_000_000, page.participants[0].date)
        assertEquals(4_825, page.total)
    }

    @Test
    fun `a participant page re-encodes unchanged`() {
        val decoded = plain.decodeFromString<ParticipantsResult>(PARTICIPANTS)
        assertEquals(
            plain.parseToJsonElement(PARTICIPANTS),
            plain.parseToJsonElement(plain.encodeToString(decoded)),
            "the projection changed the document on the way back",
        )
    }

    @Test
    fun `participant permissions decode every flag`() {
        val permissions = json.decodeFromString<ParticipantPermissions>(PERMISSIONS)

        assertTrue(permissions.isCreator && permissions.isAdmin && permissions.canAddAdmins)
        assertTrue(!permissions.isBanned && !permissions.hasLeft && !permissions.hasDefaultPermissions)
    }

    @Test
    fun `participant permissions re-encode unchanged`() {
        val decoded = json.decodeFromString<ParticipantPermissions>(PERMISSIONS)
        assertEquals(
            json.parseToJsonElement(PERMISSIONS),
            json.parseToJsonElement(json.encodeToString(decoded)),
        )
    }

    @Test
    fun `an invite link result carries the hash or says there is none`() {
        assertEquals("AbCdEf", json.decodeFromString<InviteLinkResult>(INVITE).hash)
        assertNull(json.decodeFromString<InviteLinkResult>(NO_INVITE).hash)
    }

    @Test
    fun `a partial participant page still decodes`() {
        val page = json.decodeFromString<ParticipantsResult>(
            """{"participants": [{"role": "creator", "user": {"id": 7}}], "total": 1}""",
        )
        assertEquals("creator", page.participants[0].role)
        assertEquals(1, page.total)
    }

    private companion object {
        val PARTICIPANTS = """
            {"participants": [{"user": {"id": 7}, "role": "member", "date": 1700000000000,
             "invitedBy": 8}], "total": 4825}
        """.trimIndent()

        val PERMISSIONS = """
            {"isCreator": true, "isAdmin": true, "isBanned": false, "hasLeft": false,
             "hasDefaultPermissions": false, "canAddAdmins": true}
        """.trimIndent()

        val INVITE = """{"hash": "AbCdEf"}"""

        val NO_INVITE = """{"hash": null}"""
    }
}
