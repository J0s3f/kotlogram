package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.AuthorizationsResult
import org.kotlogramme.protocol.EmptyPayload
import org.kotlogramme.protocol.GetNotifySettingsPayload
import org.kotlogramme.protocol.NotifyScope
import org.kotlogramme.protocol.NotifySettingsResult
import org.kotlogramme.protocol.NotifySettingsSpec
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.PasswordSettings
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.PrivacyKeyPayload
import org.kotlogramme.protocol.PrivacyRulesResult
import org.kotlogramme.protocol.PrivacyRuleSpec
import org.kotlogramme.protocol.ResetAuthorizationPayload
import org.kotlogramme.protocol.SetPrivacyPayload
import org.kotlogramme.protocol.UpdateNotifySettingsPayload
import org.kotlogramme.protocol.UpdateProfilePayload
import org.kotlogramme.protocol.UpdateStatusPayload
import org.kotlogramme.protocol.User
import org.kotlogramme.protocol.UsernameAvailability
import org.kotlogramme.protocol.UsernamePayload

/** Account profile, active sessions, two-factor password, privacy and notification settings. */
internal interface AccountBridge {
    val transport: Transport

    /** Updates the account's names and bio, answering the updated account. */
    @Operation("accountUpdateProfile")
    fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        about: String? = null,
    ): User = transport.request(
        "accountUpdateProfile",
        UpdateProfilePayload(firstName, lastName, about),
    )

    /** Replaces the account's username, answering the updated account. Empty removes it. */
    @Operation("accountUpdateUsername")
    fun updateUsername(username: String): User =
        transport.request("accountUpdateUsername", UsernamePayload(username))

    /** Reports whether a username is available. */
    @Operation("accountCheckUsername")
    fun checkUsername(username: String): Boolean =
        transport.request<UsernamePayload, UsernameAvailability>(
            "accountCheckUsername",
            UsernamePayload(username),
        ).available

    /** Reports the account online or offline, which is how a caller hides presence. */
    @Operation("accountUpdateStatus")
    fun updateStatus(offline: Boolean) {
        transport.request<UpdateStatusPayload, OperationResult>(
            "accountUpdateStatus",
            UpdateStatusPayload(offline),
        )
    }

    /** Lists the account's active sessions. */
    @Operation("accountGetAuthorizations")
    fun getAuthorizations(): AuthorizationsResult =
        transport.request("accountGetAuthorizations", EmptyPayload)

    /** Drops one session by the hash `accountGetAuthorizations` reports for it. */
    @Operation("accountResetAuthorization")
    fun resetAuthorization(hash: Long) {
        transport.request<ResetAuthorizationPayload, OperationResult>(
            "accountResetAuthorization",
            ResetAuthorizationPayload(hash),
        )
    }

    /** Drops every session but the current one. */
    @Operation("accountResetAuthorizations")
    fun resetAuthorizations() {
        transport.request<EmptyPayload, OperationResult>("accountResetAuthorizations", EmptyPayload)
    }

    /** Reports whether the account has a two-factor password and what Telegram shows about it. */
    @Operation("accountGetPassword")
    fun getPassword(): PasswordSettings = transport.request("accountGetPassword", EmptyPayload)

    /**
     * Reads one curated privacy setting.
     *
     * [key] is one of the `AccountPrivacyKey` wire names; the native side rejects anything else.
     */
    @Operation("accountGetPrivacy")
    fun getPrivacy(key: String): PrivacyRulesResult =
        transport.request("accountGetPrivacy", PrivacyKeyPayload(key))

    /** Writes one curated privacy setting, answering the rules in place afterwards. */
    @Operation("accountSetPrivacy")
    fun setPrivacy(key: String, rules: List<PrivacyRuleSpec>): PrivacyRulesResult =
        transport.request("accountSetPrivacy", SetPrivacyPayload(key, rules))

    /**
     * Reads the notification settings of one scope.
     *
     * [scope] says which notifications are asked about. `Account` is the account-wide scope, which
     * the layer spells as the logged-in user; a `Peer`, `ForumTopic` or `Community` scope also
     * needs [peer]. The answer echoes the scope, because the layer's own does not.
     */
    @Operation("accountGetNotifySettings")
    fun getNotifySettings(
        scope: NotifyScope,
        peer: PeerTarget = PeerTarget(),
        topMsgId: Int? = null,
    ): NotifySettingsResult = transport.request(
        "accountGetNotifySettings",
        GetNotifySettingsPayload(scope, peer, topMsgId),
    )

    /**
     * Writes the notification settings of one scope, taking the same [scope] and [peer] as
     * [getNotifySettings].
     *
     * Every field of [settings] is optional, because an absent one is what leaves that single
     * setting as Telegram has it.
     */
    @Operation("accountUpdateNotifySettings")
    fun updateNotifySettings(
        scope: NotifyScope,
        peer: PeerTarget = PeerTarget(),
        topMsgId: Int? = null,
        settings: NotifySettingsSpec = NotifySettingsSpec(),
    ) {
        transport.request<UpdateNotifySettingsPayload, OperationResult>(
            "accountUpdateNotifySettings",
            UpdateNotifySettingsPayload(scope, peer, topMsgId, settings),
        )
    }
}

/** The [AccountBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class AccountOperations(override val transport: Transport) : AccountBridge
