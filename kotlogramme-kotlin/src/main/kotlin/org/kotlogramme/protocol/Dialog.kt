package org.kotlogramme.protocol

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * A dialog with its most recent message.
 *
 * grammers types a dialog as only its peer and its last message; the remaining state comes from
 * the raw layer payload it publishes, which is the only place these fields exist. A folder row
 * carries no unread counts and no draft, so [unreadCount], [unreadMentionsCount] and [draftText]
 * are `null` for it, and [isFolder] tells the two apart.
 *
 * [meta] is the state this projection deliberately leaves out. It is populated only by
 * `getDialogsMeta`, which flattens this projection and attaches it; a plain listing omits it
 * entirely, which is why it is encoded only when it is set.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Dialog(
    val peer: Peer,
    val lastMessage: Message? = null,
    val pinned: Boolean = false,
    /** The identifier of the newest message counted for this dialog. */
    val topMessage: Int = 0,
    val unreadCount: Int? = null,
    val unreadMentionsCount: Int? = null,
    /** The unsent text of the current draft, if the dialog has one. */
    val draftText: String? = null,
    /** The Telegram folder the dialog sits in, if any. */
    val folderId: Int? = null,
    val isFolder: Boolean = false,
    /**
     * The dialog state the plain listing projection does not carry, from `getDialogsMeta`.
     *
     * It is left out of the encoded document while it holds its default, so the plain listing
     * document is byte-for-byte what it always was.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val meta: DialogMeta? = null,
)

/**
 * The dialog state the plain listing projection does not carry.
 *
 * A regular dialog populates the first block and leaves the folder counters unset; a folder row
 * does the opposite. [Dialog.isFolder] on the listing the metadata is attached to says which one a
 * document came from.
 */
@Serializable
data class DialogMeta(
    /** The dialog is marked as unread by the account. */
    val unreadMark: Boolean? = null,
    /** Show the forum's topics as ordinary messages. */
    val viewForumAsMessages: Boolean? = null,
    val readInboxMaxId: Int? = null,
    val readOutboxMaxId: Int? = null,
    /** Unread reactions, which [Dialog.unreadCount] does not include. */
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
 * The notification settings of a dialog, from the layer's `PeerNotifySettings`.
 *
 * The layer also carries per-platform sounds and story flags, which grammers exposes through no
 * accessor and this projection has no shape for.
 */
@Serializable
data class DialogNotifySettings(
    val showPreviews: Boolean? = null,
    val silent: Boolean? = null,
    /** Epoch milliseconds, converted from the layer's unix-second field. */
    val muteUntil: Long? = null,
)
