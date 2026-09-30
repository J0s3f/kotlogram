package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.AuthorizationsResult as BridgeAuthorizationsResult
import org.kotlogramme.protocol.PasswordSettings as BridgePasswordSettings
import org.kotlogramme.protocol.PrivacyRulesResult as BridgePrivacyRulesResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade carries the account projections the bridge gained.
 *
 * The documents are the same ones `AccountProtocolTest` pins on the wire side, so a field added to
 * the native projection without reaching this layer fails here.
 */
class AccountCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an authorization projects every session field`() {
        val authorizations =
            json.decodeFromString<BridgeAuthorizationsResult>(AUTHORIZATIONS).toCompatibility()

        assertEquals(180, authorizations.authorizationTtlDays)
        val session = authorizations.authorizations.single()
        assertEquals(8_867_911_212_683_051_761L, session.hash)
        assertEquals("Pixel 9", session.deviceModel)
        assertEquals("Android", session.platform)
        assertEquals("15", session.systemVersion)
        assertEquals(2_040, session.apiId)
        assertEquals("Kotlogram", session.appName)
        assertEquals("1.2.3", session.appVersion)
        assertEquals(1_700_000_000_000L, session.dateCreated)
        assertEquals(1_700_000_100_000L, session.dateActive)
        assertEquals("203.0.113.7", session.ip)
        assertEquals("Ireland", session.country)
        assertEquals("Leinster", session.region)
        assertTrue(session.current && session.officialApp && session.encryptedRequestsDisabled)
        assertTrue(!session.passwordPending && !session.callRequestsDisabled && !session.unconfirmed)
    }

    @Test
    fun `password settings project the descriptive fields`() {
        val settings = json.decodeFromString<BridgePasswordSettings>(PASSWORD).toCompatibility()

        assertTrue(settings.hasPassword && settings.hasRecovery && settings.hasSecureValues)
        assertEquals("mother", settings.hint)
        assertEquals("a***@example.com", settings.emailUnconfirmedPattern)
        assertEquals("l***@example.com", settings.loginEmailPattern)
        assertEquals(1_700_000_000_000L, settings.pendingResetDate)
    }

    @Test
    fun `privacy rules project the key, the rules and the entities`() {
        val rules = json.decodeFromString<BridgePrivacyRulesResult>(PRIVACY).toCompatibility()

        assertEquals("statusTimestamp", rules.key)
        assertEquals(listOf("allowContacts"), rules.rules.map { it.kind })
        assertEquals(listOf(7L), rules.users)
        assertEquals(listOf(42L), rules.chats)
    }

    @Test
    fun `a requested privacy rule crosses to the bridge with its kind and chats`() {
        val bridge = PrivacyRule("disallowChatParticipants", listOf(42)).toBridge()

        assertEquals("disallowChatParticipants", bridge.kind)
        assertEquals(listOf(42L), bridge.chats)
    }

    @Test
    fun `the curated privacy keys name the wire keys the native side maps`() {
        assertEquals(
            mapOf(
                "statusTimestamp" to AccountPrivacyKey.StatusTimestamp,
                "chatInvite" to AccountPrivacyKey.ChatInvite,
                "phoneNumber" to AccountPrivacyKey.PhoneNumber,
            ),
            AccountPrivacyKey.entries.associateBy { it.wire },
        )
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
