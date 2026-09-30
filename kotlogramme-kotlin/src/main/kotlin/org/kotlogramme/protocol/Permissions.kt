package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Chat rights, as reported by grammers. */

/**
 * The admin rights of a chat member, mirroring grammers' `Permissions` (TL `chatAdminRights`).
 *
 * Every right the pinned layer carries is carried, including the newer topic, story and rank flags
 * that grammers' accessors do not name. All fields default to `false`.
 */
@Serializable
data class ChatPermissions(
    val changeInfo: Boolean = false,
    val postMessages: Boolean = false,
    val editMessages: Boolean = false,
    val deleteMessages: Boolean = false,
    val banUsers: Boolean = false,
    val inviteUsers: Boolean = false,
    val pinMessages: Boolean = false,
    val addAdmins: Boolean = false,
    val anonymous: Boolean = false,
    val manageCall: Boolean = false,
    val manageTopics: Boolean = false,
    val postStories: Boolean = false,
    val editStories: Boolean = false,
    val deleteStories: Boolean = false,
    val manageDirectMessages: Boolean = false,
    val manageRanks: Boolean = false,
    val manageLinkedPeers: Boolean = false,
    val manageWelcomeMessages: Boolean = false,
    val other: Boolean = false,
)

/**
 * The restrictions applied to a banned or restricted member, mirroring grammers' `Restrictions`
 * (TL `chatBannedRights`).
 *
 * A right is `true` when it is *denied*; the layer spells the same flags the same way round.
 * [untilDate] is epoch milliseconds, and the epoch itself means the ban never expires.
 */
@Serializable
data class ChatRestrictions(
    val viewMessages: Boolean = false,
    val sendMessages: Boolean = false,
    val sendMedia: Boolean = false,
    val sendStickers: Boolean = false,
    val sendGifs: Boolean = false,
    val sendGames: Boolean = false,
    val sendInline: Boolean = false,
    val embedLinks: Boolean = false,
    val sendPolls: Boolean = false,
    val changeInfo: Boolean = false,
    val inviteUsers: Boolean = false,
    val pinMessages: Boolean = false,
    val manageTopics: Boolean = false,
    val sendPhotos: Boolean = false,
    val sendVideos: Boolean = false,
    val sendRoundvideos: Boolean = false,
    val sendAudios: Boolean = false,
    val sendVoices: Boolean = false,
    val sendDocs: Boolean = false,
    val sendPlain: Boolean = false,
    val editRank: Boolean = false,
    val sendReactions: Boolean = false,
    val manageLinkedPeers: Boolean = false,
    val untilDate: Long = 0,
)
