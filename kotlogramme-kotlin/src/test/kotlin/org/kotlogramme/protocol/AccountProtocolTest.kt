package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Wire-contract tests for the account profile, session, password and privacy models.
 *
 * The documents below are exactly what the projections in `native/src/dto/account.rs` emit and what
 * its handlers consume, which the Rust tests pin on their side, so a field renamed on one side
 * fails here instead of against a live session.
 */
class AccountProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Encodes a payload the way `Transport` does, dropping fields left at their default. */
    private val wire = Json { ignoreUnknownKeys = true }

    @Test
    fun `a profile payload carries the fields it is given`() {
        val document = wire.encodeToString(UpdateProfilePayload("Some", null, "hi"))
        assertEquals(
            wire.parseToJsonElement("""{"firstName": "Some", "about": "hi"}"""),
            wire.parseToJsonElement(document),
        )
    }

    @Test
    fun `a bare profile payload encodes as an empty object`() {
        assertEquals("{}", wire.encodeToString(UpdateProfilePayload()))
    }

    @Test
    fun `a username payload encodes its username`() {
        assertEquals("""{"username":"someone"}""", wire.encodeToString(UsernamePayload("someone")))
    }

    @Test
    fun `a status payload encodes its offline flag`() {
        assertEquals("""{"offline":true}""", wire.encodeToString(UpdateStatusPayload(true)))
    }

    @Test
    fun `a reset payload encodes its hash`() {
        assertEquals("""{"hash":7}""", wire.encodeToString(ResetAuthorizationPayload(7)))
    }

    @Test
    fun `a get privacy payload encodes only its key`() {
        assertEquals("""{"key":"chatInvite"}""", wire.encodeToString(PrivacyKeyPayload("chatInvite")))
    }

    @Test
    fun `a set privacy payload carries the rules and drops the default id lists`() {
        val document = wire.encodeToString(
            SetPrivacyPayload(
                "statusTimestamp",
                listOf(PrivacyRuleSpec("allowAll"), PrivacyRuleSpec("disallowChatParticipants", listOf(42))),
            ),
        )
        assertEquals(
            wire.parseToJsonElement(
                """{"key": "statusTimestamp",
                    "rules": [{"kind": "allowAll"},
                              {"kind": "disallowChatParticipants", "chats": [42]}]}""",
            ),
            wire.parseToJsonElement(document),
        )
    }

    @Test
    fun `a username availability result carries the flag`() {
        val result = roundTrips<UsernameAvailability>("""{"available": true}""")
        assertEquals(true, result.available)
    }

    @Test
    fun `an authorizations result carries the window and the sessions`() {
        val result = roundTrips<AuthorizationsResult>(AUTHORIZATIONS)

        assertEquals(180, result.authorizationTtlDays)
        assertEquals(1, result.authorizations.size)
        val session = result.authorizations[0]
        assertEquals(8_867_911_212_683_051_761L, session.hash)
        assertEquals("Pixel 9", session.deviceModel)
        assertEquals(2_040, session.apiId)
        assertEquals(1_700_000_000_000L, session.dateCreated)
        assertEquals(1_700_000_100_000L, session.dateActive)
        assertEquals("Ireland", session.country)
        assertEquals(true, session.current)
        assertEquals(false, session.passwordPending)
    }

    @Test
    fun `a full authorizations result declares exactly the two fields it emits`() {
        val names = (json.parseToJsonElement(AUTHORIZATIONS) as JsonObject).keys.sorted()
        assertEquals(listOf("authorizationTtlDays", "authorizations"), names)
    }

    @Test
    fun `a password result carries the descriptive fields and no key material`() {
        val result = roundTrips<PasswordSettings>(PASSWORD)

        assertEquals(true, result.hasPassword)
        assertEquals(true, result.hasRecovery)
        assertEquals(true, result.hasSecureValues)
        assertEquals("mother", result.hint)
        assertEquals("a***@example.com", result.emailUnconfirmedPattern)
        assertEquals("l***@example.com", result.loginEmailPattern)
        assertEquals(1_700_000_000_000L, result.pendingResetDate)
    }

    @Test
    fun `a password result leaves the absent fields null`() {
        val result = roundTrips<PasswordSettings>(
            """{"hasPassword": false, "hasRecovery": false, "hasSecureValues": false,
                "hint": null, "emailUnconfirmedPattern": null, "loginEmailPattern": null,
                "pendingResetDate": null}""",
        )
        assertNull(result.hint)
        assertNull(result.pendingResetDate)
    }

    @Test
    fun `a privacy rules result carries the key, the rules and the entities`() {
        val result = roundTrips<PrivacyRulesResult>(PRIVACY)

        assertEquals("statusTimestamp", result.key)
        assertEquals(listOf("allowContacts"), result.rules.map { it.kind })
        assertEquals(listOf(7L), result.users)
        assertEquals(listOf(42L), result.chats)
    }

    @Test
    fun `a privacy rule names its kind and carries the ids it references`() {
        val result = roundTrips<PrivacyRulesResult>(
            """{"key": "chatInvite",
                "rules": [{"kind": "disallowUsers", "users": [7, 8], "chats": []},
                          {"kind": "allowChatParticipants", "users": [], "chats": [42]}],
                "chats": [], "users": []}""",
        )
        assertEquals(listOf(7L, 8L), result.rules[0].users)
        assertEquals(listOf(42L), result.rules[1].chats)
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
        val AUTHORIZATIONS = """
            {"authorizationTtlDays": 180, "authorizations": [{"current": true,
             "officialApp": true, "passwordPending": false, "encryptedRequestsDisabled": true,
             "callRequestsDisabled": false, "unconfirmed": false,
             "hash": 8867911212683051761, "deviceModel": "Pixel 9", "platform": "Android",
             "systemVersion": "15", "apiId": 2040, "appName": "Kotlogram", "appVersion": "1.2.3",
             "dateCreated": 1700000000000, "dateActive": 1700000100000, "ip": "203.0.113.7",
             "country": "Ireland", "region": "Leinster"}]}
        """.trimIndent()

        val PASSWORD = """
            {"hasPassword": true, "hasRecovery": true, "hasSecureValues": true, "hint": "mother",
             "emailUnconfirmedPattern": "a***@example.com", "loginEmailPattern": "l***@example.com",
             "pendingResetDate": 1700000000000}
        """.trimIndent()

        val PRIVACY = """
            {"key": "statusTimestamp",
             "rules": [{"kind": "allowContacts", "users": [], "chats": []}],
             "chats": [42], "users": [7]}
        """.trimIndent()
    }
}
