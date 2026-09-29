package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Wire-contract tests for the chat-action models.
 *
 * The documents below are exactly what the projection in `native/src/dto/action.rs` emits, which
 * `dto::action::tests` pins on the Rust side, so a field renamed on one side fails here instead of
 * a live session.
 */
class ActionsProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `a service action decodes and re-encodes unchanged`() {
        val action = roundTrips<MessageAction>(ACTION)
        assertEquals(31, action.messageId)
        assertEquals(7L, action.senderId)
        assertEquals("pinMessage", action.kind)
    }

    @Test
    fun `a service action without a sender carries a null one`() {
        val action = roundTrips<MessageAction>(ACTION_WITHOUT_SENDER)
        assertNull(action.senderId)
        assertEquals("historyClear", action.kind)
    }

    @Test
    fun `an ordinary message reports no action rather than an error`() {
        val populated = json.decodeFromString<GetMessageActionResult>(RESULT)
        assertEquals(8L, populated.action?.senderId)
        assertEquals("chatCreate", populated.action?.kind)

        val empty = json.decodeFromString<GetMessageActionResult>(RESULT_WITHOUT_ACTION)
        assertNull(empty.action)
    }

    @Test
    fun `a message action declares exactly the three projected fields`() {
        val names = (json.parseToJsonElement(ACTION) as JsonObject).keys.sorted()
        assertEquals(listOf("kind", "messageId", "senderId"), names)
    }

    /** Decodes a native document and asserts that re-encoding it reproduces the same document. */
    private inline fun <reified T> roundTrips(document: String): T {
        val decoded = json.decodeFromString<T>(document)
        assertEquals(
            json.parseToJsonElement(document),
            json.parseToJsonElement(json.encodeToString(decoded)),
            "the projection changed the document on the way back",
        )
        return decoded
    }

    private companion object {
        val ACTION = """{"messageId": 31, "senderId": 7, "kind": "pinMessage"}"""

        val ACTION_WITHOUT_SENDER = """{"messageId": 4, "senderId": null, "kind": "historyClear"}"""

        val RESULT = """{"action": {"messageId": 9, "senderId": 8, "kind": "chatCreate"}}"""

        val RESULT_WITHOUT_ACTION = """{"action": null}"""
    }
}
