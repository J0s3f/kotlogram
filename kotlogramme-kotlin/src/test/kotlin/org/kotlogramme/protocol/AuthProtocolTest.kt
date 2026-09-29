package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Wire-contract tests for the account-identity models.
 *
 * The documents below are exactly what the projections in `native/src/dto/auth.rs` emit, which
 * `dto::auth::tests` pins on the Rust side, so a field renamed on one side fails here instead of a
 * live session.
 */
class AuthProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `a full getMe carries the account and the data centre it lives on`() {
        val me = roundTrips<Me>(ME)

        assertEquals(7L, me.user.id)
        assertEquals("Some One", me.user.fullName)
        assertEquals("+10000000000", me.user.phone)
        assertEquals(2, me.dataCentreId)
    }

    @Test
    fun `a full getMe declares exactly the account and its data centre`() {
        val names = (json.parseToJsonElement(ME) as JsonObject).keys.sorted()
        assertEquals(listOf("dataCentreId", "user"), names)
    }

    @Test
    fun `a data centre result decodes its identifier`() {
        val result = roundTrips<DataCentreResult>(DATA_CENTRE)
        assertEquals(4, result.dataCentreId)
    }

    @Test
    fun `a sign out result decodes the shared operation result`() {
        val result = roundTrips<OperationResult>(SIGN_OUT)
        assertEquals(true, result.ok)
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
        val ME = """
            {"user": {"id": 7, "username": "someone", "firstName": "Some", "lastName": "One",
             "fullName": "Some One", "usernames": ["someone_alt"], "phone": "+10000000000",
             "photoId": 4242, "status": "offline", "statusExpires": null,
             "lastSeen": 1700000000000, "statusByMe": false, "langCode": "en", "isSelf": true,
             "contact": true, "mutualContact": false, "deleted": false, "isBot": false,
             "botPrivacy": false, "botSupportsChats": false, "botInlineGeo": false,
             "botInlinePlaceholder": null, "verified": false, "restricted": false,
             "support": false, "scam": false, "restrictionReasons": []},
             "dataCentreId": 2}
        """.trimIndent()

        val DATA_CENTRE = """{"dataCentreId": 4}"""

        val SIGN_OUT = """{"ok": true}"""
    }
}
