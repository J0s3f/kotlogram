package com.github.badoualy.telegram.api

import org.kotlogramme.TelegramException
import org.kotlogramme.protocol.Button as BridgeButton
import org.kotlogramme.protocol.CallbackQueryUpdate as BridgeCallbackQueryUpdate
import org.kotlogramme.protocol.ChatPermissions as BridgeChatPermissions
import org.kotlogramme.protocol.ChatRestrictions as BridgeChatRestrictions
import org.kotlogramme.protocol.Dialog as BridgeDialog
import org.kotlogramme.protocol.DialogNotifySettings as BridgeDialogNotifySettings
import org.kotlogramme.protocol.DownloadResult as BridgeDownloadResult
import org.kotlogramme.protocol.InlineMessageId as BridgeInlineMessageId
import org.kotlogramme.protocol.InlineQueryResults as BridgeInlineQueryResults
import org.kotlogramme.protocol.InlineQueryUpdate as BridgeInlineQueryUpdate
import org.kotlogramme.protocol.InlineResult as BridgeInlineResult
import org.kotlogramme.protocol.InlineSendUpdate as BridgeInlineSendUpdate
import org.kotlogramme.protocol.InlineWebDocument as BridgeInlineWebDocument
import org.kotlogramme.protocol.Me as BridgeMe
import org.kotlogramme.protocol.Media as BridgeMedia
import org.kotlogramme.protocol.MediaChunk as BridgeMediaChunk
import org.kotlogramme.protocol.Message as BridgeMessage
import org.kotlogramme.protocol.Participant as BridgeParticipant
import org.kotlogramme.protocol.ParticipantPermissions as BridgeParticipantPermissions
import org.kotlogramme.protocol.ParticipantsResult as BridgeParticipantsResult
import org.kotlogramme.protocol.Peer as BridgePeer
import org.kotlogramme.protocol.ProfilePhoto as BridgeProfilePhoto
import org.kotlogramme.protocol.RawUpdate as BridgeRawUpdate
import org.kotlogramme.protocol.RawUpdateEntry as BridgeRawUpdateEntry
import org.kotlogramme.protocol.ReplyMarkup as BridgeReplyMarkup
import org.kotlogramme.protocol.RestrictionReason as BridgeRestrictionReason
import org.kotlogramme.protocol.Update as BridgeUpdate
import org.kotlogramme.protocol.UpdateMessageBox as BridgeUpdateMessageBox
import org.kotlogramme.protocol.UpdateState as BridgeUpdateState
import org.kotlogramme.protocol.UploadedFile as BridgeUploadedFile
import org.kotlogramme.protocol.User as BridgeUser
import java.nio.file.Path
import java.util.Base64

/** Public data model of the Kotlogram-shaped facade. */

data class SentCode(val phoneNumber: String, val phoneCodeHash: String)

data class Authorization(val user: User)

/**
 * A Telegram account, as far as grammers projects one.
 *
 * The flags the layer carries but grammers exposes no accessor for — `premium`, `fake`,
 * `bot_info_version` and `bot_description` — are absent rather than guessed. [status] is the
 * grammers presence status, lowerCamelCased, with `unknown` for an empty status; [statusExpires]
 * and [lastSeen] are epoch milliseconds.
 */
data class User(
    val id: Long,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val fullName: String = "",
    /** The collectible usernames, which grammers reports separately from [username]. */
    val usernames: List<String> = emptyList(),
    val phone: String? = null,
    val photoId: Long? = null,
    val status: String = "unknown",
    val statusExpires: Long? = null,
    val lastSeen: Long? = null,
    /** True when a coarse status is relative to the viewer. */
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

/** One reason a user is restricted, on the clients [platforms] names. */
data class RestrictionReason(
    val platforms: List<String>,
    val reason: String,
    val text: String,
)

/**
 * A chat, user or channel in the compatibility surface.
 *
 * [native] is the bridge peer this was projected from. It stays internal so that only the domain
 * interfaces can send it back to the bridge.
 *
 * [isMegagroup] is `null` for anything that is not a group chat, and [permissions] is only
 * reported for a broadcast channel, because that is the only peer grammers exposes admin rights
 * for.
 */
@ConsistentCopyVisibility
data class TelegramPeer internal constructor(
    val id: Long,
    val kind: String,
    val username: String?,
    val name: String?,
    internal val native: BridgePeer,
    val usernames: List<String> = emptyList(),
    val isMegagroup: Boolean? = null,
    val hasPhoto: Boolean = false,
    val permissions: ChatPermissions? = null,
)

/**
 * The admin rights of a chat member, mirroring grammers' `Permissions`.
 *
 * Only the ten rights grammers exposes as accessors are carried; the layer's `other`,
 * `manage_topics` and story rights have no accessor and are absent.
 */
data class ChatPermissions(
    val changeInfo: Boolean,
    val postMessages: Boolean,
    val editMessages: Boolean,
    val deleteMessages: Boolean,
    val banUsers: Boolean,
    val inviteUsers: Boolean,
    val pinMessages: Boolean,
    val addAdmins: Boolean,
    val anonymous: Boolean,
    val manageCall: Boolean,
)

/**
 * The restrictions applied to a banned or restricted member, mirroring grammers' `Restrictions`.
 *
 * A right is `true` when it is *allowed*; the layer spells the same flags the other way round.
 * [untilDate] is epoch milliseconds, and the epoch itself means the ban never expires.
 */
data class ChatRestrictions(
    val viewMessages: Boolean,
    val sendMessages: Boolean,
    val sendMedia: Boolean,
    val sendStickers: Boolean,
    val sendGifs: Boolean,
    val sendGames: Boolean,
    val sendInline: Boolean,
    val embedLinks: Boolean,
    val sendPolls: Boolean,
    val changeInfo: Boolean,
    val inviteUsers: Boolean,
    val pinMessages: Boolean,
    val untilDate: Long,
)

/**
 * The media attached to a message.
 *
 * grammers models an attachment as an enum with no shared accessor surface, so this is the same
 * flat shape the bridge sends: [kind] names the variant and says which of the other fields are
 * populated, the rest being `null`. [kind] is `unknown` for a variant this build cannot name, and
 * it still carries the object, so such an attachment stays distinguishable from no attachment.
 */
data class Media(
    val kind: String,
    val id: Long? = null,
    val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val spoiler: Boolean? = null,
    val ttlSeconds: Int? = null,
    val name: String? = null,
    val mimeType: String? = null,
    val creationDate: Long? = null,
    val duration: Double? = null,
    val resolutionWidth: Int? = null,
    val resolutionHeight: Int? = null,
    val audioTitle: String? = null,
    val performer: String? = null,
    val emoji: String? = null,
    val isAnimated: Boolean? = null,
    val phoneNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val vcard: String? = null,
    val question: String? = null,
    val isQuiz: Boolean? = null,
    val closed: Boolean? = null,
    val totalVoters: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyRadius: Int? = null,
    val title: String? = null,
    val address: String? = null,
    val provider: String? = null,
    val venueId: String? = null,
    val venueType: String? = null,
    val heading: Int? = null,
    val period: Int? = null,
    val proximityNotificationRadius: Int? = null,
    val value: Int? = null,
    val url: String? = null,
    val displayUrl: String? = null,
    val siteName: String? = null,
    val description: String? = null,
    val pageType: String? = null,
    val author: String? = null,
)

/**
 * A Telegram message, as far as grammers projects one.
 *
 * The raw-layer-only accessors — the format entities, the reply markup, the service action, the
 * forward header and the restriction reason — are left for a later phase. grammers has no caption
 * accessor, so the text of a captioned attachment is [text]. [date] and [editDate] are epoch
 * milliseconds, and [peerId] and [senderId] are `null` for a message grammers could not place.
 */
data class Message(
    val id: Int,
    val text: String,
    val outgoing: Boolean,
    val replyToMessageId: Int?,
    val peerId: Long? = null,
    val senderId: Long? = null,
    val date: Long = 0,
    val editDate: Long? = null,
    val mentioned: Boolean = false,
    val mediaUnread: Boolean = false,
    val silent: Boolean = false,
    val pinned: Boolean = false,
    /** True for a post in a broadcast channel, which is grammers' `Message::post()`. */
    val fromChannelPost: Boolean = false,
    val fromScheduled: Boolean = false,
    val editHide: Boolean = false,
    val viaBotId: Long? = null,
    val postAuthor: String? = null,
    /** The album this message belongs to, if it is part of one. */
    val groupedId: Long? = null,
    val viewCount: Int? = null,
    val forwardCount: Int? = null,
    val replyCount: Int? = null,
    val reactionCount: Int? = null,
    val media: Media? = null,
)

/** A current-layer Telegram update delivered by grammers' ordered update stream. */
data class TelegramUpdate(
    val kind: String,
    val message: Message? = null,
)

data class OutgoingMedia(
    val path: Path,
    val caption: String = "",
    val asPhoto: Boolean = false,
)

/**
 * A dialog with its most recent message.
 *
 * A folder row carries no unread counts and no draft, so [unreadCount], [unreadMentionsCount] and
 * [draftText] are `null` for it, and [isFolder] tells the two apart.
 *
 * The fields from [unreadMark] down are the state the plain listing projection does not carry.
 * `messagesGetDialogs` leaves all of them at their defaults; `messagesGetDialogsMeta` fills them
 * from the same rows. A regular dialog populates the first block and leaves [folderTitle] and the
 * counters after it unset; a folder row does the opposite.
 */
data class Dialog(
    val peer: TelegramPeer,
    val lastMessage: Message?,
    val pinned: Boolean = false,
    val topMessage: Int = 0,
    val unreadCount: Int? = null,
    val unreadMentionsCount: Int? = null,
    val draftText: String? = null,
    val folderId: Int? = null,
    val isFolder: Boolean = false,
    /** The dialog is marked as unread by the account. */
    val unreadMark: Boolean? = null,
    /** Show the forum's topics as ordinary messages. */
    val viewForumAsMessages: Boolean? = null,
    val readInboxMaxId: Int? = null,
    val readOutboxMaxId: Int? = null,
    /** Unread reactions, which [unreadCount] does not include. */
    val unreadReactionsCount: Int? = null,
    val notifySettings: DialogNotifySettings? = null,
    /** The update state of the dialog, when the layer carries one. */
    val pts: Int? = null,
    /** The time messages in the dialog are kept, in seconds; `null` when nothing expires. */
    val ttlPeriod: Int? = null,
    /** The folder's title, for a folder row. */
    val folderTitle: String? = null,
    val autofillNewBroadcasts: Boolean? = null,
    val autofillPublicGroups: Boolean? = null,
    val autofillNewCorrespondents: Boolean? = null,
    val unreadMutedPeersCount: Int? = null,
    val unreadUnmutedPeersCount: Int? = null,
    val unreadMutedMessagesCount: Int? = null,
    val unreadUnmutedMessagesCount: Int? = null,
)

/**
 * The notification settings of a dialog.
 *
 * [muteUntil] is epoch milliseconds, converted from the layer's unix-second field. The layer's
 * per-platform sounds and story flags have no grammers accessor and are absent.
 */
data class DialogNotifySettings(
    val showPreviews: Boolean? = null,
    val silent: Boolean? = null,
    val muteUntil: Long? = null,
)

/**
 * A chat member and the role grammers reports for it.
 *
 * The five roles share no accessors, so every role-specific field is `null` for the roles that
 * cannot answer it. [role] is `member`, `creator`, `admin`, `banned`, `left`, or `unknown`.
 */
data class Participant(
    val user: User,
    val role: String,
    val rank: String? = null,
    val date: Long? = null,
    val invitedBy: Long? = null,
    val promotedBy: Long? = null,
    val kickedBy: Long? = null,
    val canEdit: Boolean? = null,
    val left: Boolean? = null,
    val permissions: ChatPermissions? = null,
    val restrictions: ChatRestrictions? = null,
)

class PasswordRequiredException(val hint: String?) : TelegramException("Two-factor authentication password required")

/**
 * A reply markup: the buttons Telegram shows under a message.
 *
 * The four shapes are the four grammers' `reply_markup` module builds, and [kind] is the grammers
 * function each is named after. [Unknown] stands in for a shape a later layer adds, so a markup
 * this build cannot name still arrives whole.
 *
 * A markup built here is not attached to anything: grammers wires a markup into an outgoing message
 * through `InputMessage::reply_markup`, which the message operations own, so [MarkupApi] hands the
 * markup back for the caller to send.
 */
sealed interface ReplyMarkup {
    /** The shape's name, which is the grammers `reply_markup` function that builds it. */
    val kind: String

    /** Buttons attached to the message; grammers' `reply_markup::inline`. */
    data class Inline(val rows: List<List<Button>>) : ReplyMarkup {
        override val kind: String get() = "inline"
    }

    /** A custom keyboard replacing the send button; grammers' `reply_markup::keyboard`. */
    data class Keyboard(
        /** Buttons from top to bottom, each row from left to right, as Telegram renders them. */
        val rows: List<List<Button>>,
        /** Shrink the keyboard to fit its buttons, grammers' `Keyboard::fit_size`. */
        val fitSize: Boolean = false,
        /** Hide the keyboard once it has been used, grammers' `Keyboard::single_use`. */
        val singleUse: Boolean = false,
        /** Show the keyboard only to the users the message replies to. */
        val selective: Boolean = false,
        /**
         * Keep the keyboard after the bot is gone. grammers' builder has no method for the layer's
         * `persistent` flag, so a built keyboard always reports it `false`; only a received markup
         * can report `true`.
         */
        val persistent: Boolean = false,
        /**
         * The input-field placeholder. As with [persistent], grammers' builder cannot set it, so a
         * built keyboard always reports `null`.
         */
        val placeholder: String? = null,
    ) : ReplyMarkup {
        override val kind: String get() = "keyboard"
    }

    /** A one-off prompt to type a reply; grammers' `reply_markup::force_reply`. */
    data class ForceReply(
        val singleUse: Boolean = false,
        val selective: Boolean = false,
        /** As with [Keyboard.placeholder], only a received markup can report one. */
        val placeholder: String? = null,
    ) : ReplyMarkup {
        override val kind: String get() = "forceReply"
    }

    /** Removes the custom keyboard this bot showed earlier; grammers' `reply_markup::hide`. */
    data class Hide(val selective: Boolean = false) : ReplyMarkup {
        override val kind: String get() = "hide"
    }

    /** A markup shape a later layer adds, kept whole so nothing a peer sent is lost. */
    data class Unknown(
        override val kind: String,
        val rows: List<List<Button>> = emptyList(),
        val fitSize: Boolean = false,
        val singleUse: Boolean = false,
        val selective: Boolean = false,
        val persistent: Boolean = false,
        val placeholder: String? = null,
    ) : ReplyMarkup
}

/**
 * One button of a [ReplyMarkup].
 *
 * [kind] is the grammers `button` function that builds the button where there is one, and the Bot
 * API button name otherwise; [Unknown] stands in for a kind a later layer adds. A field is `null`
 * when this kind cannot answer it, and [Callback.data] is `null` as well when the payload Telegram
 * sent was not text, which the JSON wire cannot carry.
 *
 * Only the eight kinds grammers has a builder for — [Text], [Url], [WebView], [Callback],
 * [SwitchInline], [RequestPhone], [RequestGeo] and [RequestPoll] — can be handed to [MarkupApi] to
 * build a markup; the rest are read-only, and so are the fields grammers' constructors have no
 * parameter for, [Callback.requiresPassword] and [SwitchInline.peerTypes].
 */
sealed interface Button {
    /** The label; grammers requires it to be non-empty on every button it builds. */
    val text: String

    /** The button's name on the wire, which is also the grammers function that builds it. */
    val kind: String

    /** A plain label; grammers' `button::text`, valid only on a [ReplyMarkup.Keyboard]. */
    data class Text(override val text: String) : Button {
        override val kind: String get() = "text"
    }

    /** A link; grammers' `button::url`. */
    data class Url(override val text: String, val url: String? = null) : Button {
        override val kind: String get() = "url"
    }

    /** A page opened in a Telegram web view; grammers' `button::webview`. */
    data class WebView(override val text: String, val url: String? = null) : Button {
        override val kind: String get() = "webView"
    }

    /**
     * A button answered with a payload; grammers' `button::inline`.
     *
     * [data] is the payload as text. grammers' builder takes arbitrary bytes, but a payload this
     * library sends has to survive JSON, and one that comes back as binary reads as `null`. The
     * layer caps the payload at 64 bytes.
     */
    data class Callback(
        override val text: String,
        val data: String? = null,
        val requiresPassword: Boolean? = null,
    ) : Button {
        override val kind: String get() = "callback"
    }

    /**
     * An inline query sent to a bot; grammers' `button::switch_inline` when [samePeer] holds and
     * `button::switch_inline_elsewhere` when it does not, which asks the user to pick a peer.
     *
     * [peerTypes] narrows the peers the query may run against, and grammers' builder cannot set it.
     */
    data class SwitchInline(
        override val text: String,
        val query: String? = null,
        val samePeer: Boolean? = null,
        val peerTypes: List<String>? = null,
    ) : Button {
        override val kind: String get() = "switchInline"
    }

    /** Asks the user to send their phone number; grammers' `button::request_phone`. */
    data class RequestPhone(override val text: String) : Button {
        override val kind: String get() = "requestPhone"
    }

    /** Asks the user to send their location; grammers' `button::request_geo`. */
    data class RequestGeo(override val text: String) : Button {
        override val kind: String get() = "requestGeo"
    }

    /**
     * Asks the user to make a poll; grammers' `button::request_quiz` when [quiz] holds and
     * `button::request_poll` otherwise. A received button can report `null` for a poll of
     * unspecified kind.
     */
    data class RequestPoll(override val text: String, val quiz: Boolean? = null) : Button {
        override val kind: String get() = "requestPoll"
    }

    /** A game shortcut Telegram places itself; grammers has no builder for it. */
    data class Game(override val text: String) : Button {
        override val kind: String get() = "game"
    }

    /** The Bot API's pay button; grammers has no builder for it. */
    data class Pay(override val text: String) : Button {
        override val kind: String get() = "pay"
    }

    /** An authorization request, as Telegram itself sends it; grammers has no builder for it. */
    data class UrlAuth(
        override val text: String,
        val url: String? = null,
        val fwdText: String? = null,
        val buttonId: Int? = null,
    ) : Button {
        override val kind: String get() = "urlAuth"
    }

    /**
     * The same authorization request as a bot would send it, which also names the bot asking;
     * grammers has no builder for it.
     *
     * The bot is an account plus an access hash, and this library projects no access hash, so the
     * requesting bot is not carried.
     */
    data class InputUrlAuth(
        override val text: String,
        val url: String? = null,
        val fwdText: String? = null,
        val requestWriteAccess: Boolean? = null,
    ) : Button {
        override val kind: String get() = "inputUrlAuth"
    }

    /** The account a bot would ask to open the profile of; grammers has no builder for it. */
    data class InputUserProfile(override val text: String) : Button {
        override val kind: String get() = "inputUserProfile"
    }

    /** Opens a user's profile; grammers has no builder for it. */
    data class UserProfile(override val text: String, val userId: Long? = null) : Button {
        override val kind: String get() = "userProfile"
    }

    /** A page opened in the user's own browser; grammers has no builder for it. */
    data class SimpleWebView(override val text: String, val url: String? = null) : Button {
        override val kind: String get() = "simpleWebView"
    }

    /** Asks the user to suggest peers of a kind; grammers has no builder for it. */
    data class RequestPeer(
        override val text: String,
        val buttonId: Int? = null,
        val maxQuantity: Int? = null,
    ) : Button {
        override val kind: String get() = "requestPeer"
    }

    /** The same suggestion request as a bot would send it; grammers has no builder for it. */
    data class InputRequestPeer(
        override val text: String,
        val buttonId: Int? = null,
        val maxQuantity: Int? = null,
    ) : Button {
        override val kind: String get() = "inputRequestPeer"
    }

    /** Places [copyText] on the clipboard; grammers has no builder for it. */
    data class Copy(override val text: String, val copyText: String? = null) : Button {
        override val kind: String get() = "copy"
    }

    /** A button kind a later layer adds, kept whole so nothing a peer sent is lost. */
    data class Unknown(
        override val text: String,
        override val kind: String,
        val url: String? = null,
        val data: String? = null,
        val requiresPassword: Boolean? = null,
        val fwdText: String? = null,
        val buttonId: Int? = null,
        val query: String? = null,
        val samePeer: Boolean? = null,
        val peerTypes: List<String>? = null,
        val quiz: Boolean? = null,
        val userId: Long? = null,
        val copyText: String? = null,
        val maxQuantity: Int? = null,
        val requestWriteAccess: Boolean? = null,
    ) : Button
}

internal fun BridgeUser.toCompatibility() = User(
    id, username, firstName, lastName, fullName, usernames, phone, photoId, status, statusExpires,
    lastSeen, statusByMe, langCode, isSelf, contact, mutualContact, deleted, isBot, botPrivacy,
    botSupportsChats, botInlineGeo, botInlinePlaceholder, verified, restricted, support, scam,
    restrictionReasons.map { it.toCompatibility() },
)

internal fun BridgeRestrictionReason.toCompatibility() = RestrictionReason(platforms, reason, text)

internal fun BridgePeer.toCompatibility() = TelegramPeer(
    id, kind, username, name, this, usernames, isMegagroup, hasPhoto, permissions?.toCompatibility(),
)

internal fun BridgeChatPermissions.toCompatibility() = ChatPermissions(
    changeInfo, postMessages, editMessages, deleteMessages, banUsers, inviteUsers, pinMessages,
    addAdmins, anonymous, manageCall,
)

internal fun BridgeChatRestrictions.toCompatibility() = ChatRestrictions(
    viewMessages, sendMessages, sendMedia, sendStickers, sendGifs, sendGames, sendInline,
    embedLinks, sendPolls, changeInfo, inviteUsers, pinMessages, untilDate,
)

internal fun BridgeMedia.toCompatibility() = Media(
    kind, id, size, width, height, spoiler, ttlSeconds, name, mimeType, creationDate, duration,
    resolutionWidth, resolutionHeight, audioTitle, performer, emoji, isAnimated, phoneNumber,
    firstName, lastName, vcard, question, isQuiz, closed, totalVoters, latitude, longitude,
    accuracyRadius, title, address, provider, venueId, venueType, heading, period,
    proximityNotificationRadius, value, url, displayUrl, siteName, description, pageType, author,
)

internal fun BridgeMessage.toCompatibility() = Message(
    id, text, outgoing, replyToMessageId, peerId, senderId, date, editDate, mentioned, mediaUnread,
    silent, pinned, fromChannelPost, fromScheduled, editHide, viaBotId, postAuthor, groupedId,
    viewCount, forwardCount, replyCount, reactionCount, media?.toCompatibility(),
)

internal fun BridgeUpdate.toCompatibility() = TelegramUpdate(kind, message?.toCompatibility())

internal fun BridgeDialog.toCompatibility() = Dialog(
    peer.toCompatibility(), lastMessage?.toCompatibility(), pinned, topMessage, unreadCount,
    unreadMentionsCount, draftText, folderId, isFolder,
    meta?.unreadMark, meta?.viewForumAsMessages, meta?.readInboxMaxId, meta?.readOutboxMaxId,
    meta?.unreadReactionsCount, meta?.notifySettings?.toCompatibility(), meta?.pts, meta?.ttlPeriod,
    meta?.folderTitle, meta?.autofillNewBroadcasts, meta?.autofillPublicGroups,
    meta?.autofillNewCorrespondents, meta?.unreadMutedPeersCount, meta?.unreadUnmutedPeersCount,
    meta?.unreadMutedMessagesCount, meta?.unreadUnmutedMessagesCount,
)

internal fun BridgeDialogNotifySettings.toCompatibility() =
    DialogNotifySettings(showPreviews, silent, muteUntil)

internal fun BridgeParticipant.toCompatibility() = Participant(
    user.toCompatibility(), role, rank, date, invitedBy, promotedBy, kickedBy, canEdit, left,
    permissions?.toCompatibility(), restrictions?.toCompatibility(),
)

/**
 * The full update projection, as opposed to the [kind]-and-[message] pair [TelegramUpdate] keeps.
 *
 * The typed update payloads share almost no accessors, so this is the same flat shape the bridge
 * sends: [kind] names the grammers variant and says which of the other fields are populated, the
 * rest being `null`. [state] is set for every update, and [rawUpdate] carries the TL bytes for the
 * variants grammers does not type.
 */
data class TypedUpdate(
    val kind: String,
    val message: Message? = null,
    val state: UpdateState? = null,
    val deletedMessageIds: List<Int>? = null,
    val deletedChannelId: Long? = null,
    val callbackQuery: CallbackQueryUpdate? = null,
    val inlineQuery: InlineQueryUpdate? = null,
    val inlineSend: InlineSendUpdate? = null,
    val rawUpdate: RawUpdate? = null,
)

/** The state grammers attaches to every update. [date] is epoch milliseconds. */
data class UpdateState(
    val date: Long,
    val seq: Int,
    val messageBox: UpdateMessageBox? = null,
)

/**
 * The message box a state belongs to.
 *
 * [kind] is `common`, `secondary` or `channel`, and [channelId] is set for the channel sequence
 * only.
 */
data class UpdateMessageBox(
    val kind: String,
    val pts: Int,
    val channelId: Long? = null,
)

/**
 * A pressed inline button, mirroring grammers' `CallbackQuery`.
 *
 * [data] is the button's binary payload, decoded from the base64 the wire carries. grammers exposes
 * no accessor for the answer identifier or for the message the button belongs to, so [queryId] and
 * [messageId] are read off the update payload; [messageId] is `null` for an inline callback query
 * and [inlineMessageId] is `null` for a chat one.
 */
data class CallbackQueryUpdate(
    val data: ByteArray,
    val isFromInline: Boolean = false,
    val queryId: Long,
    val messageId: Int? = null,
    val inlineMessageId: InlineMessageId? = null,
    val peer: TelegramPeer,
    val sender: User,
)

/**
 * The identifier of an inline message, which has to be sent back to edit it.
 *
 * grammers only exposes the data centre and the access hash, and the owner its 64-bit constructor
 * carries has no accessor at all, so the identifier is [id] and nothing else.
 */
data class InlineMessageId(
    val dcId: Int,
    val accessHash: Long,
    val id: Long,
)

/** An inline query a user sent to the bot, mirroring grammers' `InlineQuery`. */
data class InlineQueryUpdate(
    val sender: User,
    val text: String,
    val offset: String,
    val queryId: Long,
    val peerType: String? = null,
)

/** A chosen inline result, mirroring grammers' `InlineSend`. [messageId] is set for a result with a keyboard. */
data class InlineSendUpdate(
    val sender: User,
    val text: String,
    val resultId: String,
    val messageId: InlineMessageId? = null,
)

/**
 * One page of results an inline bot answered a query with.
 *
 * [queryId] identifies the query, which an inline answer or a chosen result is sent with, and
 * [nextOffset] is the offset that asks for the page after this one; it is null on the last page.
 * [gallery] asks the client to show the results as a grid rather than a list.
 */
data class InlineQueryResults(
    val queryId: Long,
    val nextOffset: String? = null,
    val gallery: Boolean = false,
    val results: List<InlineResult> = emptyList(),
)

/**
 * One result of an inline query.
 *
 * The layer's two result constructors share an id, a type name and an optional title and
 * description, and differ in how they point at their content: [kind] names which one this is,
 * `result` carries [url], [thumb] and [content] and `mediaResult` carries [photoId] or
 * [documentId]. A field the other variant cannot answer is null.
 */
data class InlineResult(
    /** `result` for a plain result, `mediaResult` for one that carries a photo or a document. */
    val kind: String,
    val id: String,
    /** The layer's result type, for example `article`. */
    val type: String,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val thumb: InlineWebDocument? = null,
    val content: InlineWebDocument? = null,
    val photoId: Long? = null,
    val documentId: Long? = null,
)

/**
 * A web document a result points at.
 *
 * grammers exposes only the URL, the size and the MIME type of the layer's two web-document
 * constructors, so the access hash and the attributes are absent.
 */
data class InlineWebDocument(
    val url: String,
    val size: Int,
    val mimeType: String,
)

/**
 * One article result an inline answer carries, mirroring grammers' `Article` builder.
 *
 * `Article` is the only result grammers 0.8.1 has a builder for; it wraps a text message, so this
 * is the text it sends plus the metadata Telegram renders it with. The reply markup and the format
 * entities an `InputMessage` can also carry are absent, because the markup and message domains own
 * those shapes.
 */
data class InlineArticle(
    val title: String,
    val messageText: String,
    val id: String? = null,
    val description: String? = null,
    val url: String? = null,
    val thumbUrl: String? = null,
    val linkPreview: Boolean = true,
    val invertMedia: Boolean = false,
)

/** The prompt that offers to switch an inline query to the bot's private chat. */
data class InlineSwitchPm(val text: String, val startParam: String)

/**
 * The update exactly as Telegram sent it, which is all grammers exposes for an event it does not
 * type.
 *
 * [name] is the TL constructor name, so the variant is readable without decoding [data]; the
 * constructor id the bytes start with is the one [name] was read from. The generated TL types carry
 * no serde derives, so the bytes travel as a base64 string and are decoded here.
 */
data class RawUpdate(
    val name: String,
    val data: ByteArray,
)

/**
 * One update as Telegram sent it, with the state it advanced to and the peers it named.
 *
 * [peers] is sorted by id on the native side, so the document is stable. This is what a caller
 * needs when [TypedUpdate] does not describe the event.
 */
data class RawUpdateEntry(
    val update: RawUpdate,
    val state: UpdateState,
    val peers: List<TelegramPeer>,
)

internal fun BridgeUpdate.toTypedCompatibility() = TypedUpdate(
    kind, message?.toCompatibility(), state?.toCompatibility(), deletedMessageIds, deletedChannelId,
    callbackQuery?.toCompatibility(), inlineQuery?.toCompatibility(), inlineSend?.toCompatibility(),
    rawUpdate?.toCompatibility(),
)

internal fun BridgeUpdateState.toCompatibility() =
    UpdateState(date, seq, messageBox?.toCompatibility())

internal fun BridgeUpdateMessageBox.toCompatibility() = UpdateMessageBox(kind, pts, channelId)

internal fun BridgeCallbackQueryUpdate.toCompatibility() = CallbackQueryUpdate(
    Base64.getDecoder().decode(data), isFromInline, queryId, messageId,
    inlineMessageId?.toCompatibility(), peer.toCompatibility(), sender.toCompatibility(),
)

internal fun BridgeInlineMessageId.toCompatibility() = InlineMessageId(dcId, accessHash, id)

internal fun BridgeInlineQueryUpdate.toCompatibility() =
    InlineQueryUpdate(sender.toCompatibility(), text, offset, queryId, peerType)

internal fun BridgeInlineSendUpdate.toCompatibility() =
    InlineSendUpdate(sender.toCompatibility(), text, resultId, messageId?.toCompatibility())

internal fun BridgeInlineQueryResults.toCompatibility() =
    InlineQueryResults(queryId, nextOffset, gallery, results.map { it.toCompatibility() })

internal fun BridgeInlineResult.toCompatibility() = InlineResult(
    kind, id, type, title, description, url, thumb?.toCompatibility(), content?.toCompatibility(),
    photoId, documentId,
)

internal fun BridgeInlineWebDocument.toCompatibility() = InlineWebDocument(url, size, mimeType)

internal fun BridgeRawUpdate.toCompatibility() = RawUpdate(name, Base64.getDecoder().decode(data))

internal fun BridgeRawUpdateEntry.toCompatibility() = RawUpdateEntry(
    update.toCompatibility(), state.toCompatibility(), peers.map { it.toCompatibility() },
)

internal fun BridgeReplyMarkup.toCompatibility(): ReplyMarkup = when (kind) {
    "inline" -> ReplyMarkup.Inline(rows.map { row -> row.map { it.toCompatibility() } })
    "keyboard" -> ReplyMarkup.Keyboard(
        rows.map { row -> row.map { it.toCompatibility() } }, fitSize, singleUse, selective, persistent,
        placeholder,
    )
    "forceReply" -> ReplyMarkup.ForceReply(singleUse, selective, placeholder)
    "hide" -> ReplyMarkup.Hide(selective)
    else -> ReplyMarkup.Unknown(
        kind, rows.map { row -> row.map { it.toCompatibility() } }, fitSize, singleUse, selective,
        persistent, placeholder,
    )
}

internal fun BridgeButton.toCompatibility(): Button = when (kind) {
    "text" -> Button.Text(text)
    "url" -> Button.Url(text, url)
    "webView" -> Button.WebView(text, url)
    "callback" -> Button.Callback(text, data, requiresPassword)
    "switchInline" -> Button.SwitchInline(text, query, samePeer, peerTypes)
    "requestPhone" -> Button.RequestPhone(text)
    "requestGeo" -> Button.RequestGeo(text)
    "requestPoll" -> Button.RequestPoll(text, quiz)
    "game" -> Button.Game(text)
    "pay" -> Button.Pay(text)
    "urlAuth" -> Button.UrlAuth(text, url, fwdText, buttonId)
    "inputUrlAuth" -> Button.InputUrlAuth(text, url, fwdText, requestWriteAccess)
    "inputUserProfile" -> Button.InputUserProfile(text)
    "userProfile" -> Button.UserProfile(text, userId)
    "simpleWebView" -> Button.SimpleWebView(text, url)
    "requestPeer" -> Button.RequestPeer(text, buttonId, maxQuantity)
    "inputRequestPeer" -> Button.InputRequestPeer(text, buttonId, maxQuantity)
    "copy" -> Button.Copy(text, copyText)
    else -> Button.Unknown(
        text, kind, url, data, requiresPassword, fwdText, buttonId, query, samePeer, peerTypes, quiz,
        userId, copyText, maxQuantity, requestWriteAccess,
    )
}

/**
 * The signed-in account and the data centre its session is homed on.
 *
 * grammers reports the account and the data centre separately — the account through
 * `users.getUsers`, the data centre through the session — so this pairs them. [dataCentreId] is the
 * home data centre of the session, which [Kotlogram.getDcById] names.
 */
data class AccountIdentity(
    val user: User,
    val dataCentreId: Int,
)

internal fun BridgeMe.toCompatibility() = AccountIdentity(user.toCompatibility(), dataCentreId)

/** Where a download wrote the file and how many bytes it holds. */
data class DownloadedMedia(
    val path: String,
    val size: Long,
)

/**
 * One chunk of a streamed download.
 *
 * [offset] is the byte offset in the file the chunk starts at, [size] is its decoded length in
 * bytes and [data] its decoded bytes. Downloading in chunks is how a caller seeks within a file:
 * ask for a chunk size that is a multiple of 4096 bytes and skip the chunks before the part it
 * wants.
 */
data class MediaChunk(
    val offset: Long,
    val size: Long,
    val data: ByteArray,
)

/**
 * The metadata of an uploaded file, which a later send can reuse.
 *
 * grammers exposes no size on an upload, so [size] is the length of the input that was uploaded.
 * [isBig] distinguishes the two TL constructors grammers chooses between at ten megabytes, and only
 * the small-file one carries an [md5Checksum].
 */
data class UploadedFile(
    val id: Long,
    val name: String,
    val size: Long,
    val parts: Int,
    val md5Checksum: String? = null,
    val isBig: Boolean = false,
)

/**
 * A profile photo of a peer.
 *
 * [dcId] is `null` for the empty constructor, which carries no location, and [width]/[height] are
 * the largest thumbnail's pixel dimensions, `null` when it describes bytes without a resolution.
 */
data class ProfilePhoto(
    val id: Long,
    val dcId: Int? = null,
    val size: Long,
    val width: Int? = null,
    val height: Int? = null,
    val spoiler: Boolean = false,
    val ttlSeconds: Int? = null,
)

internal fun BridgeDownloadResult.toCompatibility() = DownloadedMedia(path, size)

internal fun BridgeMediaChunk.toCompatibility() = MediaChunk(offset, size, Base64.getDecoder().decode(data))

internal fun BridgeUploadedFile.toCompatibility() =
    UploadedFile(id, name, size, parts, md5Checksum, isBig)

internal fun BridgeProfilePhoto.toCompatibility() =
    ProfilePhoto(id, dcId, size, width, height, spoiler, ttlSeconds)

/**
 * The media filter a message search can be restricted to.
 *
 * [wire] is the name the bridge and the native `messages_filter` share: the TL constructor less its
 * `inputMessagesFilter` prefix, so [PHOTO_VIDEO] is `photoVideo` and [CHAT_PHOTOS] is `chatPhotos`.
 * Every variant grammers 0.8.1's `MessagesFilter` enum offers is named, including the plain
 * [EMPTY] that leaves the search unrestricted.
 */
enum class MessageSearchFilter(val wire: String) {
    EMPTY("empty"),
    PHOTOS("photos"),
    VIDEO("video"),
    PHOTO_VIDEO("photoVideo"),
    DOCUMENT("document"),
    URL("url"),
    GIF("gif"),
    VOICE("voice"),
    MUSIC("music"),
    CHAT_PHOTOS("chatPhotos"),
    PHONE_CALLS("phoneCalls"),
    ROUND_VOICE("roundVoice"),
    ROUND_VIDEO("roundVideo"),
    MENTIONS("myMentions"),
    GEO("geo"),
    CONTACTS("contacts"),
    PINNED("pinned"),
}

/**
 * A page of a chat's participants: the listing and the chat's total member count.
 *
 * [total] is the count grammers' participant iterator reports for the whole chat, not the number
 * of entries in [participants], which is capped by the requested limit.
 */
data class ParticipantPage(
    val participants: List<Participant>,
    val total: Int,
)

/**
 * The participant listing grammers' `ChannelParticipantsFilter` covers.
 *
 * [wire] is the name the native side maps onto the layer's filter; `Recent` has none because it is
 * the layer's default listing. grammers only applies a filter to a channel or megagroup, so a
 * small group ignores it.
 */
enum class ParticipantFilter(val wire: String?) {
    Recent(null),
    Admins("admins"),
    Bots("bots"),
    Banned("banned"),
    Contacts("contacts"),
    Search("search"),
    Kicked("kicked"),
}

/**
 * The role membership of one participant, as grammers' `ParticipantPermissions` answers it.
 *
 * This is a different question from the rights a role carries: it says what the member *is* in the
 * chat, not what they are allowed to do.
 */
data class ParticipantPermissions(
    /** True for the chat or channel creator. */
    val isCreator: Boolean,
    /** True for a creator too, because a creator has every right an admin has. */
    val isAdmin: Boolean,
    /** True for a banned channel participant. */
    val isBanned: Boolean,
    /** True for a member that left the channel on their own. */
    val hasLeft: Boolean,
    /** True for a normal member with no restrictions and no admin rights. */
    val hasDefaultPermissions: Boolean,
    /** True for a creator and for an admin whose rights let them add admins. */
    val canAddAdmins: Boolean,
)

internal fun BridgeParticipantsResult.toCompatibility() =
    ParticipantPage(participants.map { it.toCompatibility() }, total)

internal fun BridgeParticipantPermissions.toCompatibility() = ParticipantPermissions(
    isCreator, isAdmin, isBanned, hasLeft, hasDefaultPermissions, canAddAdmins,
)

/** The "can do" rights a caller asks for, in the shape grammers' `set_admin_rights` takes. */
internal fun ChatPermissions.toBridge() = BridgeChatPermissions(
    changeInfo, postMessages, editMessages, deleteMessages, banUsers, inviteUsers, pinMessages,
    addAdmins, anonymous, manageCall,
)

/**
 * The bans a caller asks for, in the layer's own "banned" polarity.
 *
 * The compat model spells the flags the way Telegram does — `true` denies the right — which is the
 * same polarity [`BridgeChatRestrictions`] carries, so the flags cross unchanged.
 */
internal fun ChatRestrictions.toBridge() = BridgeChatRestrictions(
    viewMessages, sendMessages, sendMedia, sendStickers, sendGifs, sendGames, sendInline,
    embedLinks, sendPolls, changeInfo, inviteUsers, pinMessages, untilDate,
)
