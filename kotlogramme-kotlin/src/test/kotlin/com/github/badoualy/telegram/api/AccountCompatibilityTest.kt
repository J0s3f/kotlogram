package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.AuthorizationsResult as BridgeAuthorizationsResult
import org.kotlogramme.protocol.NotifySettingsResult as BridgeNotifySettingsResult
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

    @Test
    fun `an account-wide notify result projects the scope and leaves every flag unset`() {
        val notify = json.decodeFromString<BridgeNotifySettingsResult>(NOTIFY_ACCOUNT)
            .toCompatibility()

        assertEquals("account", notify.scope)
        assertEquals(null, notify.settings.silent)
        assertEquals(null, notify.settings.muteUntil)
        assertEquals(null, notify.settings.iosSound)
    }

    @Test
    fun `a peer notify result projects the flags, the mute and every sound`() {
        val notify = json.decodeFromString<BridgeNotifySettingsResult>(NOTIFY_PEER).toCompatibility()

        assertEquals("peer", notify.scope)
        val settings = notify.settings
        assertEquals(false, settings.showPreviews)
        assertEquals(true, settings.silent)
        assertEquals(1_700_000_000_000L, settings.muteUntil)
        assertEquals(NotificationSound("ringtone", id = 5150L), settings.androidSound)
        assertEquals(
            NotificationSound("local", title = "Ping", data = "blob"),
            settings.iosSound,
        )
        assertTrue(settings.storiesMuted == true && settings.storiesHideSender == false)
        assertEquals(null, settings.otherSound)
    }

    @Test
    fun `requested notify settings cross to the bridge with only what was set`() {
        val bridge = NotifySettings(silent = true, muteUntil = 1_700_000_000_000L).toBridge()

        assertEquals(true, bridge.silent)
        assertEquals(1_700_000_000_000L, bridge.muteUntil)
        assertEquals(null, bridge.showPreviews)
        assertEquals(null, bridge.sound)

        val sound = NotifySettings(
            sound = NotificationSound("ringtone", id = 5150L),
            storiesSound = NotificationSound("none"),
        ).toBridge()
        assertEquals("ringtone", sound.sound?.kind)
        assertEquals(5150L, sound.sound?.id)
        assertEquals("none", sound.storiesSound?.kind)
    }

    @Test
    fun `the notify scopes name the wire scopes the native side maps`() {
        assertEquals(
            mapOf(
                "account" to AccountNotifyScope.Account,
                "peer" to AccountNotifyScope.Peer,
                "users" to AccountNotifyScope.Users,
                "chats" to AccountNotifyScope.Chats,
                "broadcasts" to AccountNotifyScope.Broadcasts,
                "forumTopic" to AccountNotifyScope.ForumTopic,
                "community" to AccountNotifyScope.Community,
            ),
            AccountNotifyScope.entries.associateBy { it.bridge.wire },
        )
    }

    private companion object {
        val NOTIFY_ACCOUNT = """
            {"scope": "account",
             "settings": {"showPreviews": null, "silent": null, "muteUntil": null,
              "iosSound": null, "androidSound": null, "otherSound": null, "storiesMuted": null,
              "storiesHideSender": null, "storiesIosSound": null, "storiesAndroidSound": null,
              "storiesOtherSound": null}}
        """.trimIndent()

        val NOTIFY_PEER = """
            {"scope": "peer",
             "settings": {"showPreviews": false, "silent": true, "muteUntil": 1700000000000,
              "iosSound": {"kind": "local", "id": null, "title": "Ping", "data": "blob"},
              "androidSound": {"kind": "ringtone", "id": 5150, "title": null, "data": null},
              "otherSound": null, "storiesMuted": true, "storiesHideSender": false,
              "storiesIosSound": null, "storiesAndroidSound": null, "storiesOtherSound": null}}
        """.trimIndent()
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
