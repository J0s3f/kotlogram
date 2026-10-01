package com.github.badoualy.telegram.api

import org.kotlogramme.TelegramException
import org.kotlogramme.protocol.Authorization as BridgeAuthorization
import org.kotlogramme.protocol.AuthorizationsResult as BridgeAuthorizationsResult
import org.kotlogramme.protocol.Button as BridgeButton
import org.kotlogramme.protocol.CallbackQueryUpdate as BridgeCallbackQueryUpdate
import org.kotlogramme.protocol.ChatPermissions as BridgeChatPermissions
import org.kotlogramme.protocol.ChatRestrictions as BridgeChatRestrictions
import org.kotlogramme.protocol.BlockedContacts as BridgeBlockedContacts
import org.kotlogramme.protocol.BlockedPeer as BridgeBlockedPeer
import org.kotlogramme.protocol.ContactEntry as BridgeContactEntry
import org.kotlogramme.protocol.ContactImport as BridgeContactImport
import org.kotlogramme.protocol.ContactPeer as BridgeContactPeer
import org.kotlogramme.protocol.ContactUser as BridgeContactUser
import org.kotlogramme.protocol.ContactsPage as BridgeContactsPage
import org.kotlogramme.protocol.Dialog as BridgeDialog
import org.kotlogramme.protocol.DialogFilterSpec as BridgeDialogFilterSpec
import org.kotlogramme.protocol.DialogFolder as BridgeDialogFolder
import org.kotlogramme.protocol.DialogFoldersResult as BridgeDialogFoldersResult
import org.kotlogramme.protocol.PeerTarget as BridgePeerTarget
import org.kotlogramme.protocol.FoundContacts as BridgeFoundContacts
import org.kotlogramme.protocol.ImportedContact as BridgeImportedContact
import org.kotlogramme.protocol.ImportedContacts as BridgeImportedContacts
import org.kotlogramme.protocol.PopularInvite as BridgePopularInvite
import org.kotlogramme.protocol.AllStickers as BridgeAllStickers
import org.kotlogramme.protocol.ArchivedStickerSet as BridgeArchivedStickerSet
import org.kotlogramme.protocol.FavedStickers as BridgeFavedStickers
import org.kotlogramme.protocol.NotificationSound as BridgeNotificationSound
import org.kotlogramme.protocol.NotifyScope as BridgeNotifyScope
import org.kotlogramme.protocol.NotifySettingsResult as BridgeNotifySettingsResult
import org.kotlogramme.protocol.NotifySettingsSpec as BridgeNotifySettingsSpec
import org.kotlogramme.protocol.NotifySoundSpec as BridgeNotifySoundSpec
import org.kotlogramme.protocol.PeerNotifySettings as BridgePeerNotifySettings
import org.kotlogramme.protocol.RecentStickers as BridgeRecentStickers
import org.kotlogramme.protocol.StickerPack as BridgeStickerPack
import org.kotlogramme.protocol.StickerSet as BridgeStickerSet
import org.kotlogramme.protocol.StickerSetInstallResult as BridgeStickerSetInstallResult
import org.kotlogramme.protocol.StickerSetResult as BridgeStickerSetResult
import org.kotlogramme.protocol.DialogNotifySettings as BridgeDialogNotifySettings
import org.kotlogramme.protocol.DownloadResult as BridgeDownloadResult
import org.kotlogramme.protocol.ForwardHeader as BridgeForwardHeader
import org.kotlogramme.protocol.GuestChatAnswerResult as BridgeGuestChatAnswerResult
import org.kotlogramme.protocol.GuestChatQueryUpdate as BridgeGuestChatQueryUpdate
import org.kotlogramme.protocol.InlineMessageId as BridgeInlineMessageId
import org.kotlogramme.protocol.InlineQueryResults as BridgeInlineQueryResults
import org.kotlogramme.protocol.InlineQueryUpdate as BridgeInlineQueryUpdate
import org.kotlogramme.protocol.InlineResult as BridgeInlineResult
import org.kotlogramme.protocol.InlineSendUpdate as BridgeInlineSendUpdate
import org.kotlogramme.protocol.InlineSwitchPm as BridgeInlineSwitchPm
import org.kotlogramme.protocol.InlineSwitchWebview as BridgeInlineSwitchWebview
import org.kotlogramme.protocol.InlineWebDocument as BridgeInlineWebDocument
import org.kotlogramme.protocol.Me as BridgeMe
import org.kotlogramme.protocol.Media as BridgeMedia
import org.kotlogramme.protocol.MediaChunk as BridgeMediaChunk
import org.kotlogramme.protocol.Message as BridgeMessage
import org.kotlogramme.protocol.MessageAction
import org.kotlogramme.protocol.MessageEntity as BridgeMessageEntity
import org.kotlogramme.protocol.Participant as BridgeParticipant
import org.kotlogramme.protocol.ParticipantPermissions as BridgeParticipantPermissions
import org.kotlogramme.protocol.ParticipantsResult as BridgeParticipantsResult
import org.kotlogramme.protocol.PasswordSettings as BridgePasswordSettings
import org.kotlogramme.protocol.Peer as BridgePeer
import org.kotlogramme.protocol.PrivacyRuleResult as BridgePrivacyRuleResult
import org.kotlogramme.protocol.PrivacyRulesResult as BridgePrivacyRulesResult
import org.kotlogramme.protocol.PrivacyRuleSpec as BridgePrivacyRuleSpec
import org.kotlogramme.protocol.ProfilePhoto as BridgeProfilePhoto
import org.kotlogramme.protocol.RawUpdate as BridgeRawUpdate
import org.kotlogramme.protocol.RawUpdateEntry as BridgeRawUpdateEntry
import org.kotlogramme.protocol.ReplyHeader as BridgeReplyHeader
import org.kotlogramme.protocol.ReplyMarkup as BridgeReplyMarkup
import org.kotlogramme.protocol.RestrictionReason as BridgeRestrictionReason
import org.kotlogramme.protocol.Update as BridgeUpdate
import org.kotlogramme.protocol.UpdateMessageBox as BridgeUpdateMessageBox
import org.kotlogramme.protocol.UpdateState as BridgeUpdateState
import org.kotlogramme.protocol.UploadedFile as BridgeUploadedFile
import org.kotlogramme.protocol.UploadProgress as BridgeUploadProgress
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
 * The layer reports the `messageMediaDocument` variant for a video, a voice note and an animation
 * alike, so a document is instead named by what it carries: `video`, `audio` or `animation`, and
 * `document` for a plain file.
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
 * The header of a forwarded message, mirroring grammers' `MessageFwdHeader`.
 *
 * The peer references are Bot API dialog ids rather than full objects. [date] and [savedDate]
 * are epoch milliseconds.
 */
data class ForwardHeader(
    val imported: Boolean = false,
    val savedOut: Boolean = false,
    val fromId: Long? = null,
    val fromName: String? = null,
    val date: Long = 0,
    val channelPost: Int? = null,
    val postAuthor: String? = null,
    val savedFromPeer: Long? = null,
    val savedFromMsgId: Int? = null,
    val savedFromId: Long? = null,
    val savedFromName: String? = null,
    val savedDate: Long? = null,
    val psaType: String? = null,
)

/**
 * The header of a reply, mirroring grammers' `MessageReplyHeader`.
 *
 * The layer has two reply-header constructors: a normal reply and a reply to a story. They share
 * no fields, so this is the same flat shape [Media] uses: [kind] names the constructor and says
 * which of the other fields are populated, the rest being the defaults.
 */
data class ReplyHeader(
    /** `header` for a normal reply, `storyHeader` for a reply to a story. */
    val kind: String,
    // Normal reply fields.
    val replyToScheduled: Boolean = false,
    val forumTopic: Boolean = false,
    val quote: Boolean = false,
    val replyToEphemeral: Boolean = false,
    val replyToMsgId: Int? = null,
    val replyToPeerId: Long? = null,
    val replyFrom: ForwardHeader? = null,
    val replyMedia: Media? = null,
    val replyToTopId: Int? = null,
    val quoteText: String? = null,
    val quoteOffset: Int? = null,
    val todoItemId: Int? = null,
    val pollOption: String? = null,
    // Story reply fields.
    val storyPeer: Long? = null,
    val storyId: Int? = null,
)

/**
 * A Telegram message, as far as grammers projects one.
 *
 * [entities] are the formatting entities on [text], empty when it is unformatted, and [htmlText]
 * and [markdownText] are the same text rendered from those entities by grammers. grammers has no
 * caption accessor, so the text of a captioned attachment is [text]. [date] and [editDate] are
 * epoch milliseconds, and [peerId] and [senderId] are `null` for a message grammers could not
 * place.
 *
 * [peer] and [sender] are the full objects for [peerId] and [senderId]. [sender] is `null` when
 * the sender is not a user (a group or channel posting anonymously).
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
    /** The forward header, if this message was forwarded from another. */
    val forwardHeader: ForwardHeader? = null,
    /** The reply header, if this message is a reply to another. */
    val replyHeader: ReplyHeader? = null,
    /** The reasons this message is restricted, empty when it is not. */
    val restrictionReasons: List<RestrictionReason> = emptyList(),
    /** The service action, if this is a service message. */
    val action: MessageAction? = null,
    /** The reply markup, if this message carries one (bot messages). */
    val replyMarkup: ReplyMarkup? = null,
    /** The full peer object for [peerId], registered so the facade can send to it. */
    val peer: TelegramPeer? = null,
    /** The full sender object for [senderId], when the sender is a user. */
    val sender: User? = null,
    /** The formatting entities on [text], empty when the text is unformatted. */
    val entities: List<MessageEntity> = emptyList(),
    /** [text] rendered as HTML from [entities], as grammers computes it. */
    val htmlText: String = "",
    /** [text] rendered as CommonMark from [entities], as grammers computes it. */
    val markdownText: String = "",
)

/** A current-layer Telegram update delivered by grammers' ordered update stream. */
data class TelegramUpdate(
    val kind: String,
    val message: Message? = null,
)

/**
 * One item of an album being sent.
 *
 * Exactly one of [path] and [fileHandle] is set: a path uploads the local file now, a handle
 * reuses an upload a `FilesApi.uploadBytes` or `FilesApi.uploadStream` already produced.
 */
data class OutgoingMedia(
    val path: Path? = null,
    val caption: String = "",
    val asPhoto: Boolean = false,
    val fileHandle: Long? = null,
)

/**
 * One formatting entity a message carries, in either direction.
 *
 * [type] is the layer's entity constructor without its `messageEntity` prefix, for example `bold`,
 * `pre` or `textUrl`. A received entity carries the same name and fields, so it can be handed back
 * to a send unchanged. The variant-specific fields default to null and are only meaningful on the
 * types that accept them; a type whose required field is missing makes the native side refuse the
 * request rather than guess.
 */
data class MessageEntity(
    val type: String,
    val offset: Int,
    val length: Int,
    val url: String? = null,
    val userId: Long? = null,
    val language: String? = null,
    val customEmojiId: Long? = null,
)

/**
 * The media an edit replaces a message's media with.
 *
 * Exactly one source is named: [File] uploads a local file, [Url] hands a URL to Telegram to
 * download and [CopyOf] reuses the media of an existing message without a re-upload.
 */
sealed interface EditMedia {
    /**
     * A local file, uploaded and sent with the given [kind]. [durationSeconds], [width] and
     * [height] describe a [MediaKind.VIDEO] and are ignored for the other kinds.
     */
    data class File(
        val path: Path,
        val kind: MediaKind = MediaKind.DOCUMENT,
        val durationSeconds: Double? = null,
        val width: Int? = null,
        val height: Int? = null,
    ) : EditMedia

    /** A URL Telegram downloads; only a photo or a document, never a verbatim file. */
    data class Url(val url: String, val kind: MediaKind = MediaKind.DOCUMENT) : EditMedia

    /** The media of [messageId] in [peer], reused without a re-upload. */
    data class CopyOf(val peer: TelegramPeer, val messageId: Int) : EditMedia
}

/**
 * The URL media an inline edit attaches.
 *
 * An inline message has no local file to upload, so [EditMedia.File] has no inline counterpart.
 */
data class InlineEditMedia(val url: String, val kind: MediaKind = MediaKind.DOCUMENT)

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
    forwardHeader?.toCompatibility(), replyHeader?.toCompatibility(),
    restrictionReasons.map { it.toCompatibility() }, action,
    replyMarkup?.toCompatibility(), peer?.toCompatibility(), sender?.toCompatibility(),
    entities.map { it.toCompatibility() }, htmlText, markdownText,
)

internal fun BridgeMessageEntity.toCompatibility() = MessageEntity(
    type, offset, length, url, userId, language, customEmojiId,
)

internal fun BridgeForwardHeader.toCompatibility() = ForwardHeader(
    imported, savedOut, fromId, fromName, date, channelPost, postAuthor, savedFromPeer,
    savedFromMsgId, savedFromId, savedFromName, savedDate, psaType,
)

internal fun BridgeReplyHeader.toCompatibility() = ReplyHeader(
    kind, replyToScheduled, forumTopic, quote, replyToEphemeral, replyToMsgId, replyToPeerId,
    replyFrom?.toCompatibility(), replyMedia?.toCompatibility(), replyToTopId, quoteText,
    quoteOffset, todoItemId, pollOption, storyPeer, storyId,
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
    val guestChatQuery: GuestChatQueryUpdate? = null,
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
 * A guest-chat query, mirroring grammers' `GuestChatQuery`.
 *
 * [queryId] is what an answer is sent to, [message] is the message that mentioned the bot, and
 * [referenceMessages] are the messages the update carried.
 */
data class GuestChatQueryUpdate(
    val queryId: Long,
    val message: Message,
    val referenceMessages: List<Message> = emptyList(),
)

/**
 * One page of results an inline bot answered a query with.
 *
 * [queryId] identifies the query, which an inline answer or a chosen result is sent with, and
 * [nextOffset] is the offset that asks for the page after this one; it is null on the last page.
 * [gallery] asks the client to show the results as a grid rather than a list. [switchPm] and
 * [switchWebview] are the prompts the bot answered with, when it sent any.
 */
data class InlineQueryResults(
    val queryId: Long,
    val nextOffset: String? = null,
    val gallery: Boolean = false,
    val switchPm: InlineSwitchPm? = null,
    val switchWebview: InlineSwitchWebview? = null,
    val results: List<InlineResult> = emptyList(),
)

/**
 * One result of an inline query.
 *
 * The layer's two result constructors share an id, a type name and an optional title and
 * description, and differ in how they point at their content: [kind] names which one this is,
 * `result` carries [url], [thumb] and [content] and `mediaResult` carries [photoId] or
 * [documentId]. A field the other variant cannot answer is null. [sendMessageText] is the text the
 * result's message would post when it is a text message, and null for a media-carrying one.
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
    val sendMessageText: String? = null,
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
) : InlineAnswerResult

/**
 * A media result an inline answer carries: the content URL Telegram renders as [kind], an optional
 * JPEG [thumbUrl] and the [caption] its message posts.
 *
 * grammers' builder only covers articles, so this is built on the native side from the raw layer
 * constructors. [kind] is `photo`, `gif`, `video`, `voice` or `document`.
 */
data class InlineMediaResult(
    val kind: String,
    val contentUrl: String,
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val thumbUrl: String? = null,
    val caption: String? = null,
) : InlineAnswerResult

/** One result an inline answer carries: an [InlineArticle] or an [InlineMediaResult]. */
sealed interface InlineAnswerResult

/** The prompt that offers to switch an inline query to the bot's private chat. */
data class InlineSwitchPm(val text: String, val startParam: String)

/** The prompt that offers to open an inline query's result in a webview. */
data class InlineSwitchWebview(val text: String, val url: String)

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
    guestChatQuery?.toCompatibility(), rawUpdate?.toCompatibility(),
)

internal fun BridgeUpdateState.toCompatibility() =
    UpdateState(date, seq, messageBox?.toCompatibility())

internal fun BridgeUpdateMessageBox.toCompatibility() = UpdateMessageBox(kind, pts, channelId)

internal fun BridgeCallbackQueryUpdate.toCompatibility() = CallbackQueryUpdate(
    Base64.getDecoder().decode(data), isFromInline, queryId, messageId,
    inlineMessageId?.toCompatibility(), peer.toCompatibility(), sender.toCompatibility(),
)

internal fun BridgeInlineMessageId.toCompatibility() = InlineMessageId(dcId, accessHash, id)

internal fun BridgeGuestChatQueryUpdate.toCompatibility() = GuestChatQueryUpdate(
    queryId, message.toCompatibility(), referenceMessages.map { it.toCompatibility() },
)

/**
 * The identifier of the inline message a guest-chat answer produced.
 *
 * The native projection also carries the owner id of the layer's 64-bit constructor; the compat
 * [InlineMessageId] does not surface it yet, so an answer addressed that way is not editable
 * through the facade until it does.
 */
internal fun BridgeGuestChatAnswerResult.toCompatibility() = InlineMessageId(dcId, accessHash, id)

internal fun BridgeInlineQueryUpdate.toCompatibility() =
    InlineQueryUpdate(sender.toCompatibility(), text, offset, queryId, peerType)

internal fun BridgeInlineSendUpdate.toCompatibility() =
    InlineSendUpdate(sender.toCompatibility(), text, resultId, messageId?.toCompatibility())

internal fun BridgeInlineQueryResults.toCompatibility() =
    InlineQueryResults(
        queryId,
        nextOffset,
        gallery,
        switchPm?.toCompatibility(),
        switchWebview?.toCompatibility(),
        results.map { it.toCompatibility() },
    )

internal fun BridgeInlineSwitchPm.toCompatibility() = InlineSwitchPm(text, startParam)

internal fun BridgeInlineSwitchWebview.toCompatibility() = InlineSwitchWebview(text, url)

internal fun BridgeInlineResult.toCompatibility() = InlineResult(
    kind, id, type, title, description, url, thumb?.toCompatibility(), content?.toCompatibility(),
    photoId, documentId, sendMessageText,
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

/**
 * One active Telegram session on the account.
 *
 * [hash] identifies the session for `accountResetAuthorization`, [current] marks the session this
 * call was made from, and [dateCreated]/[dateActive] are epoch milliseconds.
 */
data class AccountAuthorization(
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
    val current: Boolean = false,
    val officialApp: Boolean = false,
    val passwordPending: Boolean = false,
    val encryptedRequestsDisabled: Boolean = false,
    val callRequestsDisabled: Boolean = false,
    val unconfirmed: Boolean = false,
)

/** The account's active sessions and Telegram's inactivity window for them. */
data class Authorizations(
    val authorizationTtlDays: Int,
    val authorizations: List<AccountAuthorization>,
)

/**
 * Whether the account has a two-factor password and what Telegram shows about it.
 *
 * The layer's key material is deliberately absent, because a caller that can read it can compute
 * the password hash.
 */
data class PasswordSettings(
    val hasPassword: Boolean = false,
    val hasRecovery: Boolean = false,
    val hasSecureValues: Boolean = false,
    val hint: String? = null,
    val emailUnconfirmedPattern: String? = null,
    val loginEmailPattern: String? = null,
    /** Epoch milliseconds at which a pending password reset becomes effective. */
    val pendingResetDate: Long? = null,
)

/** A privacy rule a caller asks to set, named by the layer constructor less its prefix. */
data class PrivacyRule(
    val kind: String,
    val chats: List<Long> = emptyList(),
)

/** One privacy rule that is in place, with the bare ids it references. */
data class PrivacyRuleValue(
    val kind: String,
    val users: List<Long> = emptyList(),
    val chats: List<Long> = emptyList(),
)

/** The rules a privacy setting holds, together with the entities they reference. */
data class PrivacyRules(
    val key: String,
    val rules: List<PrivacyRuleValue> = emptyList(),
    val chats: List<Long> = emptyList(),
    val users: List<Long> = emptyList(),
)

/** The privacy keys this bridge curates; the layer knows more and they stay behind `invokeRaw`. */
enum class AccountPrivacyKey(val wire: String) {
    StatusTimestamp("statusTimestamp"),
    ChatInvite("chatInvite"),
    PhoneNumber("phoneNumber"),
}

internal fun BridgeAuthorization.toCompatibility() = AccountAuthorization(
    hash, deviceModel, platform, systemVersion, apiId, appName, appVersion, dateCreated,
    dateActive, ip, country, region, current, officialApp, passwordPending,
    encryptedRequestsDisabled, callRequestsDisabled, unconfirmed,
)

internal fun BridgeAuthorizationsResult.toCompatibility() =
    Authorizations(authorizationTtlDays, authorizations.map { it.toCompatibility() })

internal fun BridgePasswordSettings.toCompatibility() = PasswordSettings(
    hasPassword, hasRecovery, hasSecureValues, hint, emailUnconfirmedPattern, loginEmailPattern,
    pendingResetDate,
)

internal fun PrivacyRule.toBridge() = BridgePrivacyRuleSpec(kind, chats)

internal fun BridgePrivacyRuleResult.toCompatibility() = PrivacyRuleValue(kind, users, chats)

internal fun BridgePrivacyRulesResult.toCompatibility() =
    PrivacyRules(key, rules.map { it.toCompatibility() }, chats, users)

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
 * The live progress of an upload in flight, as `FilesApi.uploadProgress` reports it.
 *
 * [sent] is how many bytes have been handed to Telegram so far and [total] the declared size; a
 * path upload fills [total] in when it opens the file, so it is only zero before that. [bytesPerSecond]
 * is the average rate since the upload started and [elapsedMillis] how long that is, which is
 * everything a render loop needs for a bar and a transfer rate.
 */
data class UploadProgress(
    val sent: Long,
    val total: Long,
    val bytesPerSecond: Double,
    val elapsedMillis: Long,
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
    /**
     * The per-client registry handle a later send references this upload by, as an alternative to
     * a local path. It is distinct from [id], the TL file id, and is only valid for the client
     * that produced it.
     */
    val handle: Long? = null,
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

internal fun BridgeUploadProgress.toCompatibility() =
    UploadProgress(sent, total, bytesPerSecond, elapsedMillis)

internal fun BridgeUploadedFile.toCompatibility() =
    UploadedFile(id, name, size, parts, md5Checksum, isBig, handle)

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

/** A user together with the registered peer it projects to. */
data class ContactUser(val user: User, val peer: TelegramPeer)

/** One saved contact, enriched with the user it names. */
data class ContactEntry(
    val userId: Long,
    val mutual: Boolean,
    /** The account the id names, when the answer carried its user object. */
    val user: User?,
    val peer: TelegramPeer?,
)

/**
 * A page of saved contacts.
 *
 * [notModified] is the layer's `contactsNotModified`, which carries no list at all.
 */
data class ContactsPage(
    val contacts: List<ContactEntry>,
    val savedCount: Int,
    val notModified: Boolean,
    /** The user objects the entries referenced, each with its registered peer. */
    val users: List<ContactUser>,
)

/** One phone contact to import. */
data class ContactImport(
    val clientId: Long,
    val phone: String,
    val firstName: String,
    val lastName: String = "",
)

/** One entry of [ImportedContacts.imported]. */
data class ImportedContact(
    val userId: Long,
    val clientId: Long,
    val user: User?,
    val peer: TelegramPeer?,
)

/** One entry of [ImportedContacts.popularInvites]. */
data class PopularInvite(val clientId: Long, val importers: Int)

/** The imported, retried and popular contacts of one import. */
data class ImportedContacts(
    val imported: List<ImportedContact>,
    /** The client ids Telegram could not import; the caller may retry them. */
    val retryContacts: List<Long>,
    val popularInvites: List<PopularInvite>,
    val users: List<ContactUser>,
)

/** A peer an answer referenced, resolved against the users the answer carried. */
data class ContactPeer(
    /** The account description when the peer is a user; null for a chat or channel. */
    val user: User?,
    val peer: TelegramPeer,
)

/** One blocked peer with the layer's block date, in epoch milliseconds. */
data class BlockedPeer(val date: Long, val user: User?, val peer: TelegramPeer)

/** The account's blocked peers; [count] is only present on the sliced answer. */
data class BlockedContacts(
    val count: Int?,
    val blocked: List<BlockedPeer>,
    val users: List<ContactUser>,
)

/** The matches of a contacts search: the viewer's contacts and the global results. */
data class FoundContacts(
    val myResults: List<ContactPeer>,
    val results: List<ContactPeer>,
    val users: List<ContactUser>,
)

internal fun BridgeContactUser.toCompatibility() =
    ContactUser(user.toCompatibility(), peer.toCompatibility())

internal fun BridgeContactEntry.toCompatibility() =
    ContactEntry(userId, mutual, user?.toCompatibility(), peer?.toCompatibility())

internal fun BridgeContactsPage.toCompatibility() = ContactsPage(
    contacts.map { it.toCompatibility() },
    savedCount,
    notModified,
    users.map { it.toCompatibility() },
)

internal fun BridgeImportedContact.toCompatibility() =
    ImportedContact(userId, clientId, user?.toCompatibility(), peer?.toCompatibility())

internal fun BridgePopularInvite.toCompatibility() = PopularInvite(clientId, importers)

internal fun BridgeImportedContacts.toCompatibility() = ImportedContacts(
    imported.map { it.toCompatibility() },
    retryContacts,
    popularInvites.map { it.toCompatibility() },
    users.map { it.toCompatibility() },
)

internal fun BridgeContactPeer.toCompatibility() =
    ContactPeer(user?.toCompatibility(), peer.toCompatibility())

internal fun BridgeBlockedPeer.toCompatibility() =
    BlockedPeer(date, user?.toCompatibility(), peer.toCompatibility())

internal fun BridgeBlockedContacts.toCompatibility() = BlockedContacts(
    count,
    blocked.map { it.toCompatibility() },
    users.map { it.toCompatibility() },
)

internal fun BridgeFoundContacts.toCompatibility() = FoundContacts(
    myResults.map { it.toCompatibility() },
    results.map { it.toCompatibility() },
    users.map { it.toCompatibility() },
)

/** The request shape crosses to the bridge unchanged. */
internal fun ContactImport.toBridge() = BridgeContactImport(clientId, phone, firstName, lastName)

/** One sticker pack: an emoticon and the sticker ids it groups. */
data class StickerPack(val emoticon: String, val documents: List<Long>)

/**
 * A sticker set summary.
 *
 * [installedDate] is epoch milliseconds, and the thumbnail travels as the identifiers the layer
 * reports rather than as bytes.
 */
data class StickerSet(
    val id: Long,
    val accessHash: Long,
    val title: String,
    val shortName: String,
    val count: Int,
    val hash: Int,
    val archived: Boolean = false,
    val official: Boolean = false,
    val masks: Boolean = false,
    val emojis: Boolean = false,
    val textColor: Boolean = false,
    val channelEmojiStatus: Boolean = false,
    val creator: Boolean = false,
    val installedDate: Long? = null,
    val thumbDocumentId: Long? = null,
    val thumbDcId: Int? = null,
    val thumbVersion: Int? = null,
    /** The packs the answer carried; empty for the list operations. */
    val packs: List<StickerPack> = emptyList(),
    /** The ids of the documents (stickers) the answer carried. */
    val documents: List<Long> = emptyList(),
)

/** The set `messagesGetStickerSet` answered with, or the not-modified marker. */
data class StickerSetResult(val notModified: Boolean, val set: StickerSet?)

/** The account's sticker sets. */
data class AllStickers(val notModified: Boolean, val hash: Long, val sets: List<StickerSet>)

/** The stickers the account recently used; [dates] matches [stickers] by index. */
data class RecentStickers(
    val notModified: Boolean,
    val hash: Long,
    val packs: List<StickerPack>,
    val stickers: List<Long>,
    val dates: List<Long>,
)

/** The stickers the account has favourited. */
data class FavedStickers(
    val notModified: Boolean,
    val hash: Long,
    val packs: List<StickerPack>,
    val stickers: List<Long>,
)

/** One sticker set `messagesInstallStickerSet` archived instead of installing one. */
data class ArchivedStickerSet(
    val set: StickerSet,
    /** Absent when Telegram's answer named no cover for the archived set. */
    val coverDocumentId: Long? = null,
)

/**
 * What an install did.
 *
 * Telegram's success answer is empty on this layer, so [installed] is true and [archivedSets] is
 * empty after a plain install; only an archive names the sets it archived.
 */
data class StickerSetInstall(
    val installed: Boolean,
    val archivedSets: List<ArchivedStickerSet> = emptyList(),
)

/** One notification sound, mirroring Telegram's own constructors. */
data class NotificationSound(
    /** `default`, `none`, `local` or `ringtone`. */
    val kind: String,
    val id: Long? = null,
    val title: String? = null,
    val data: String? = null,
)

/** The notification settings of one scope. */
data class AccountNotifySettings(
    val scope: String,
    val settings: PeerNotifySettings,
)

/**
 * The notification settings themselves, as Telegram reports them.
 *
 * Every field is nullable because Telegram's settings are flags: an absent one means "not said",
 * which is not the same answer as an explicit `false`.
 */
data class PeerNotifySettings(
    val showPreviews: Boolean? = null,
    val silent: Boolean? = null,
    /** Epoch milliseconds at which a mute lifts. */
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

/**
 * The notification settings to write.
 *
 * Only what should change is set: an absent field leaves that one setting as Telegram has it.
 */
data class NotifySettings(
    val showPreviews: Boolean? = null,
    val silent: Boolean? = null,
    /** Epoch milliseconds at which the mute lifts; `0` unmutes now. */
    val muteUntil: Long? = null,
    val sound: NotificationSound? = null,
    val storiesMuted: Boolean? = null,
    val storiesHideSender: Boolean? = null,
    val storiesSound: NotificationSound? = null,
)

/**
 * Which notifications an operation addresses.
 *
 * [Account] is the account-wide scope, which Telegram spells as the logged-in user; the peer-scoped
 * values need a peer to name.
 */
enum class AccountNotifyScope(internal val bridge: BridgeNotifyScope) {
    Account(BridgeNotifyScope.Account),
    Peer(BridgeNotifyScope.Peer),
    Users(BridgeNotifyScope.Users),
    Chats(BridgeNotifyScope.Chats),
    Broadcasts(BridgeNotifyScope.Broadcasts),
    ForumTopic(BridgeNotifyScope.ForumTopic),
    Community(BridgeNotifyScope.Community),
}

internal fun BridgeStickerPack.toCompatibility() = StickerPack(emoticon, documents)

internal fun BridgeStickerSet.toCompatibility() = StickerSet(
    id, accessHash, title, shortName, count, hash, archived, official, masks, emojis, textColor,
    channelEmojiStatus, creator, installedDate, thumbDocumentId, thumbDcId, thumbVersion,
    packs.map { it.toCompatibility() }, documents,
)

internal fun BridgeStickerSetResult.toCompatibility() =
    StickerSetResult(notModified, set?.toCompatibility())

internal fun BridgeAllStickers.toCompatibility() =
    AllStickers(notModified, hash, sets.map { it.toCompatibility() })

internal fun BridgeRecentStickers.toCompatibility() =
    RecentStickers(notModified, hash, packs.map { it.toCompatibility() }, stickers, dates)

internal fun BridgeFavedStickers.toCompatibility() =
    FavedStickers(notModified, hash, packs.map { it.toCompatibility() }, stickers)

internal fun BridgeArchivedStickerSet.toCompatibility() =
    ArchivedStickerSet(set.toCompatibility(), coverDocumentId)

internal fun BridgeStickerSetInstallResult.toCompatibility() =
    StickerSetInstall(installed, archivedSets.map { it.toCompatibility() })

internal fun BridgeNotificationSound.toCompatibility() =
    NotificationSound(kind, id, title, data)

internal fun BridgePeerNotifySettings.toCompatibility() = PeerNotifySettings(
    showPreviews, silent, muteUntil, iosSound?.toCompatibility(), androidSound?.toCompatibility(),
    otherSound?.toCompatibility(), storiesMuted, storiesHideSender,
    storiesIosSound?.toCompatibility(), storiesAndroidSound?.toCompatibility(),
    storiesOtherSound?.toCompatibility(),
)

internal fun BridgeNotifySettingsResult.toCompatibility() =
    AccountNotifySettings(scope, settings.toCompatibility())

internal fun NotificationSound.toBridge() = BridgeNotifySoundSpec(kind, id, title, data)

internal fun NotifySettings.toBridge() = BridgeNotifySettingsSpec(
    showPreviews, silent, muteUntil, sound?.toBridge(), storiesMuted, storiesHideSender,
    storiesSound?.toBridge(),
)

/**
 * One dialog filter (folder).
 *
 * [kind] is `filter`, `chatlist` or `default`. Only a `filter` carries the contacts/bots/broadcasts
 * flags, only a `chatlist` reports [hasMyInvites], and the peers are the registered handles the
 * bridge resolved for it.
 */
data class DialogFolder(
    val id: Int,
    val kind: String,
    val title: String,
    val contacts: Boolean = false,
    val nonContacts: Boolean = false,
    val groups: Boolean = false,
    val broadcasts: Boolean = false,
    val bots: Boolean = false,
    val excludeMuted: Boolean = false,
    val excludeRead: Boolean = false,
    val excludeArchived: Boolean = false,
    val hasMyInvites: Boolean = false,
    val pinnedPeers: List<TelegramPeer> = emptyList(),
    val includePeers: List<TelegramPeer> = emptyList(),
    val excludePeers: List<TelegramPeer> = emptyList(),
)

/** The account's dialog filters and the layer's tags flag. */
data class DialogFolders(
    val tagsEnabled: Boolean,
    val filters: List<DialogFolder>,
)

/**
 * A dialog filter a caller asks to create or replace.
 *
 * The peer lists take the registered peers a previous read reported, so a filter can be read and
 * written back unchanged.
 */
data class DialogFolderSpec(
    val title: String,
    val contacts: Boolean = false,
    val nonContacts: Boolean = false,
    val groups: Boolean = false,
    val broadcasts: Boolean = false,
    val bots: Boolean = false,
    val excludeMuted: Boolean = false,
    val excludeRead: Boolean = false,
    val excludeArchived: Boolean = false,
    val pinnedPeers: List<TelegramPeer> = emptyList(),
    val includePeers: List<TelegramPeer> = emptyList(),
    val excludePeers: List<TelegramPeer> = emptyList(),
)

internal fun BridgeDialogFolder.toCompatibility() = DialogFolder(
    id, kind, title, contacts, nonContacts, groups, broadcasts, bots, excludeMuted,
    excludeRead, excludeArchived, hasMyInvites,
    pinnedPeers.map { it.toCompatibility() },
    includePeers.map { it.toCompatibility() },
    excludePeers.map { it.toCompatibility() },
)

internal fun BridgeDialogFoldersResult.toCompatibility() =
    DialogFolders(tagsEnabled, filters.map { it.toCompatibility() })

internal fun DialogFolderSpec.toBridge() = BridgeDialogFilterSpec(
    title, contacts, nonContacts, groups, broadcasts, bots, excludeMuted, excludeRead,
    excludeArchived,
    pinnedPeers.map { BridgePeerTarget(it.native.nativeHandle) },
    includePeers.map { BridgePeerTarget(it.native.nativeHandle) },
    excludePeers.map { BridgePeerTarget(it.native.nativeHandle) },
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
    embedLinks, sendPolls, changeInfo, inviteUsers, pinMessages, untilDate = untilDate,
)
