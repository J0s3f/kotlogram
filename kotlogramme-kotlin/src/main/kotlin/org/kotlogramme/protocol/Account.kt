package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the account profile, session, password and privacy operations. */

/** Payload of `accountUpdateProfile`; an absent field leaves the current value unchanged. */
@Serializable
internal data class UpdateProfilePayload(
    val firstName: String? = null,
    val lastName: String? = null,
    val about: String? = null,
)

/** Payload of `accountUpdateUsername` and `accountCheckUsername`. */
@Serializable
internal data class UsernamePayload(val username: String)

/** Payload of `accountUpdateStatus`; `offline` hides or shows the account's presence. */
@Serializable
internal data class UpdateStatusPayload(val offline: Boolean)

/** Payload of `accountResetAuthorization`. */
@Serializable
internal data class ResetAuthorizationPayload(val hash: Long)

/** Result of `accountCheckUsername`: whether the name can be registered. */
@Serializable
internal data class UsernameAvailability(val available: Boolean)

/**
 * One active session on the account, mirroring the layer's `authorization` constructor.
 *
 * [hash] is the identifier `accountResetAuthorization` takes, [dateCreated] and [dateActive] are
 * epoch milliseconds, and [current] marks the session this call was made from.
 */
@Serializable
internal data class Authorization(
    val current: Boolean = false,
    val officialApp: Boolean = false,
    val passwordPending: Boolean = false,
    val encryptedRequestsDisabled: Boolean = false,
    val callRequestsDisabled: Boolean = false,
    val unconfirmed: Boolean = false,
    val hash: Long,
    val deviceModel: String,
    val platform: String,
    val systemVersion: String,
    val apiId: Int,
    val appName: String,
    val appVersion: String,
    val dateCreated: Long,
    val dateActive: Long,
    val ip: String,
    val country: String,
    val region: String,
)

/** Result of `accountGetAuthorizations`: the active sessions and Telegram's inactivity window. */
@Serializable
internal data class AuthorizationsResult(
    val authorizationTtlDays: Int,
    val authorizations: List<Authorization>,
)

/**
 * Result of `accountGetPassword`: whether a two-factor password is set and what Telegram shows.
 *
 * The layer's key material (`currentAlgo`, `srpB`, `srpId`, `newAlgo`, `newSecureAlgo`,
 * `secureRandom`) is deliberately not projected, because a caller that can read it can compute the
 * password hash.
 */
@Serializable
internal data class PasswordSettings(
    val hasPassword: Boolean = false,
    val hasRecovery: Boolean = false,
    val hasSecureValues: Boolean = false,
    val hint: String? = null,
    val emailUnconfirmedPattern: String? = null,
    val loginEmailPattern: String? = null,
    /** Epoch milliseconds at which a pending password reset becomes effective. */
    val pendingResetDate: Long? = null,
)

/** Payload of `accountGetPrivacy`. */
@Serializable
internal data class PrivacyKeyPayload(val key: String)

/** Payload of `accountSetPrivacy`. */
@Serializable
internal data class SetPrivacyPayload(
    val key: String,
    val rules: List<PrivacyRuleSpec>,
)

/**
 * One requested privacy rule.
 *
 * [kind] names the layer constructor less its `privacyValue` prefix (`allowAll`, `disallowAll`,
 * `allowContacts`, `allowChatParticipants`, …). [chats] is read by the `*ChatParticipants` kinds;
 * the `*Users` kinds are refused, because the layer needs each user's access hash.
 */
@Serializable
internal data class PrivacyRuleSpec(
    val kind: String,
    val chats: List<Long> = emptyList(),
)

/** One rule a privacy setting currently holds. */
@Serializable
internal data class PrivacyRuleResult(
    val kind: String,
    val users: List<Long> = emptyList(),
    val chats: List<Long> = emptyList(),
)

/** Result of `accountGetPrivacy` and `accountSetPrivacy`. */
@Serializable
internal data class PrivacyRulesResult(
    val key: String,
    val rules: List<PrivacyRuleResult> = emptyList(),
    val chats: List<Long> = emptyList(),
    val users: List<Long> = emptyList(),
)
