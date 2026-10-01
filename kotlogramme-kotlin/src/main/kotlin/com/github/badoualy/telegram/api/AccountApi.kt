package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.PeerTarget as BridgePeerTarget

/** Account profile, active sessions, two-factor password, privacy and notification operations. */
interface AccountApi : BridgeApi {
    /** Updates the account's first name, last name and bio, answering the updated account. */
    fun accountUpdateProfile(
        firstName: String? = null,
        lastName: String? = null,
        about: String? = null,
    ): User = bridge.updateProfile(firstName, lastName, about).toCompatibility()

    /** Replaces the account's username, answering the updated account. Empty removes it. */
    fun accountUpdateUsername(username: String): User =
        bridge.updateUsername(username).toCompatibility()

    /** Reports whether a username is available. */
    fun accountCheckUsername(username: String): Boolean = bridge.checkUsername(username)

    /** Reports the account online or offline, which is how a caller hides presence. */
    fun accountUpdateStatus(offline: Boolean) {
        bridge.updateStatus(offline)
    }

    /** Lists the account's active sessions. */
    fun accountGetAuthorizations(): Authorizations = bridge.getAuthorizations().toCompatibility()

    /** Drops one session by the hash `accountGetAuthorizations` reports for it. */
    fun accountResetAuthorization(hash: Long) {
        bridge.resetAuthorization(hash)
    }

    /** Drops every session but the one this call is made from. */
    fun accountResetAuthorizations() {
        bridge.resetAuthorizations()
    }

    /** Reports whether the account has a two-factor password and what Telegram shows about it. */
    fun accountGetPassword(): PasswordSettings = bridge.getPassword().toCompatibility()

    /**
     * Reads one curated privacy setting.
     *
     * [key] is one of the [`AccountPrivacyKey`] values; the native side curates the same three.
     */
    fun accountGetPrivacy(key: AccountPrivacyKey): PrivacyRules =
        bridge.getPrivacy(key.wire).toCompatibility()

    /** Writes one curated privacy setting, answering the rules in place afterwards. */
    fun accountSetPrivacy(key: AccountPrivacyKey, rules: List<PrivacyRule>): PrivacyRules =
        bridge.setPrivacy(key.wire, rules.map { it.toBridge() }).toCompatibility()

    /**
     * Reads the notification settings of one scope.
     *
     * [AccountNotifyScope.Account] asks for the account-wide settings, which is what a caller wants
     * when it has no particular chat in mind. A [`AccountNotifyScope.Peer`], `ForumTopic` or
     * `Community` scope also needs [peer]; [topMsgId] only carries for a `ForumTopic` scope. The
     * answer echoes the scope, because Telegram's own does not.
     */
    fun accountGetNotifySettings(
        scope: AccountNotifyScope,
        peer: TelegramPeer? = null,
        topMsgId: Int? = null,
    ): AccountNotifySettings =
        bridge.getNotifySettings(scope.bridge, BridgePeerTarget(peer?.native?.nativeHandle), topMsgId)
            .toCompatibility()

    /**
     * Writes the notification settings of one scope, taking the same [scope], [peer] and [topMsgId]
     * as [accountGetNotifySettings].
     *
     * [settings] carries only what should change: an absent field leaves that one setting as
     * Telegram has it, which is how a single setting is written without resetting the rest.
     */
    fun accountUpdateNotifySettings(
        scope: AccountNotifyScope,
        peer: TelegramPeer? = null,
        topMsgId: Int? = null,
        settings: NotifySettings = NotifySettings(),
    ) {
        bridge.updateNotifySettings(
            scope.bridge,
            BridgePeerTarget(peer?.native?.nativeHandle),
            topMsgId,
            settings.toBridge(),
        )
    }
}
