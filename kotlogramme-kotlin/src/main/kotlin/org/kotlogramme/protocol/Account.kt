package org.kotlogramme.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Payloads and results of the account profile, session, password, privacy and notify operations. */

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

/** The scope a notification operation addresses, as the native side names it. */
@Serializable
internal enum class NotifyScope(val wire: String) {
    @SerialName("account")
    Account("account"),

    @SerialName("peer")
    Peer("peer"),

    @SerialName("users")
    Users("users"),

    @SerialName("chats")
    Chats("chats"),

    @SerialName("broadcasts")
    Broadcasts("broadcasts"),

    @SerialName("forumTopic")
    ForumTopic("forumTopic"),

    @SerialName("community")
    Community("community"),
}

/** Payload of `accountGetNotifySettings`. */
@Serializable
internal data class GetNotifySettingsPayload(
    val scope: NotifyScope,
    val peerHandle: Long? = null,
    val username: String? = null,
    /** The forum topic's top message id, only meaningful for `forumTopic`. */
    val topMsgId: Int? = null,
) {
    constructor(
        scope: NotifyScope,
        peer: PeerTarget,
        topMsgId: Int? = null,
    ) : this(scope, peer.peerHandle, peer.username, topMsgId)
}

/** One requested notification sound, mirroring the layer's constructors. */
@Serializable
internal data class NotifySoundSpec(
    /** `default`, `none`, `local` or `ringtone`. */
    val kind: String,
    /** A ringtone's identifier; only a `ringtone` sound carries it. */
    val id: Long? = null,
    /** A local sound's shown title; only a `local` sound carries it. */
    val title: String? = null,
    /** A local sound's data blob; only a `local` sound carries it. */
    val data: String? = null,
)

/** Settings to write via `accountUpdateNotifySettings`. Every field is optional. */
@Serializable
internal data class NotifySettingsSpec(
    val showPreviews: Boolean? = null,
    val silent: Boolean? = null,
    /** Epoch milliseconds at which the mute lifts; `0` unmutes now. */
    val muteUntil: Long? = null,
    val sound: NotifySoundSpec? = null,
    val storiesMuted: Boolean? = null,
    val storiesHideSender: Boolean? = null,
    val storiesSound: NotifySoundSpec? = null,
)

/** Payload of `accountUpdateNotifySettings`. */
@Serializable
internal data class UpdateNotifySettingsPayload(
    val scope: NotifyScope,
    val peerHandle: Long? = null,
    val username: String? = null,
    val topMsgId: Int? = null,
    val settings: NotifySettingsSpec,
) {
    constructor(
        scope: NotifyScope,
        peer: PeerTarget,
        topMsgId: Int? = null,
        settings: NotifySettingsSpec,
    ) : this(scope, peer.peerHandle, peer.username, topMsgId, settings)
}

/** One notification sound, as the native side projects it. */
@Serializable
internal data class NotificationSound(
    /** `default`, `none`, `local` or `ringtone`. */
    val kind: String,
    val id: Long? = null,
    val title: String? = null,
    val data: String? = null,
)

/** The notification settings of one scope. */
@Serializable
internal data class PeerNotifySettings(
    val showPreviews: Boolean? = null,
    val silent: Boolean? = null,
    /** Epoch milliseconds, converted from the layer's whole-second `mute_until`. */
    val muteUntil: Long? = null,
    val iosSound: NotificationSound? = null,
    val androidSound: NotificationSound? = null,
    val otherSound: NotificationSound? = null,
    val storiesMuted: Boolean? = null,
    val storiesHideSender: Boolean? = null,
    val storiesIosSound: NotificationSound? = null,
    val storiesAndroidSound: NotificationSound? = null,
    val storiesOtherSound: NotificationSound? = null,
)

/** Result of `accountGetNotifySettings`. */
@Serializable
internal data class NotifySettingsResult(
    /** The scope that was asked for. */
    val scope: String,
    val settings: PeerNotifySettings,
)
