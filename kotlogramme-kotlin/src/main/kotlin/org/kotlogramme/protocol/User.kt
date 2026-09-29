package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** A Telegram account, as far as the bridge projects it. */

/**
 * One reason a user is restricted, mirroring grammers' `RestrictionReason`.
 *
 * [platforms] names the clients the restriction applies on: `all`, `android`, `ios`,
 * `windowsPhone`, or whatever platform name Telegram reported.
 */
@Serializable
data class RestrictionReason(
    val platforms: List<String> = emptyList(),
    val reason: String = "",
    val text: String = "",
)

/**
 * A Telegram account, mirroring the accessors grammers exposes on a user.
 *
 * grammers has no accessor for the layer's `premium`, `fake`, `bot_info_version` or
 * `bot_description` flags, so those are absent rather than read out of the raw user.
 *
 * [status] is the grammers presence status, lowerCamelCased, with `unknown` for `userStatusEmpty`.
 * [statusExpires] and [lastSeen] are epoch milliseconds, and [statusByMe] marks the coarse
 * statuses (`recently`, `lastWeek`, `lastMonth`) that are relative to the viewer.
 */
@Serializable
data class User(
    val id: Long,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String = "",
    /** The collectible usernames, which grammers reports separately from [username]. */
    val usernames: List<String> = emptyList(),
    val phone: String? = null,
    /** The identifier of the profile photo, for chat and profile-photo downloads. */
    val photoId: Long? = null,
    val status: String = "unknown",
    val statusExpires: Long? = null,
    val lastSeen: Long? = null,
    val statusByMe: Boolean = false,
    val langCode: String? = null,
    val isSelf: Boolean = false,
    val contact: Boolean = false,
    val mutualContact: Boolean = false,
    val deleted: Boolean = false,
    val isBot: Boolean = false,
    val botPrivacy: Boolean = false,
    val botSupportsChats: Boolean = false,
    val botInlineGeo: Boolean = false,
    val botInlinePlaceholder: String? = null,
    val verified: Boolean = false,
    val restricted: Boolean = false,
    val support: Boolean = false,
    val scam: Boolean = false,
    val restrictionReasons: List<RestrictionReason> = emptyList(),
)
