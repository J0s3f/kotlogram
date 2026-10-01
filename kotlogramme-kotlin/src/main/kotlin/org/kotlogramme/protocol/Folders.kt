package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the typed dialog-filter (folder) operations. */

/**
 * One dialog filter the native side projected.
 *
 * [kind] is `filter`, `chatlist` or `default`, which is how the layer splits `DialogFilter`. Only
 * a `filter` carries the contacts/bots/broadcasts flags, only a `chatlist` reports
 * [hasMyInvites], and a `default` marker carries neither. [pinnedPeers], [includePeers] and
 * [excludePeers] hold the registered peers the filter references.
 */
@Serializable
internal data class DialogFolder(
    val id: Int = 0,
    val kind: String = "filter",
    val title: String = "",
    val contacts: Boolean = false,
    val nonContacts: Boolean = false,
    val groups: Boolean = false,
    val broadcasts: Boolean = false,
    val bots: Boolean = false,
    val excludeMuted: Boolean = false,
    val excludeRead: Boolean = false,
    val excludeArchived: Boolean = false,
    val hasMyInvites: Boolean = false,
    val pinnedPeers: List<Peer> = emptyList(),
    val includePeers: List<Peer> = emptyList(),
    val excludePeers: List<Peer> = emptyList(),
)

/** Result of `messagesGetDialogFilters`: the account's filters and the tags flag. */
@Serializable
internal data class DialogFoldersResult(
    val tagsEnabled: Boolean = false,
    val filters: List<DialogFolder> = emptyList(),
)

/** Payload of `messagesUpdateDialogFilter`; a null [filter] deletes the one named by [id]. */
@Serializable
internal data class UpdateDialogFilterPayload(
    val id: Int,
    val filter: DialogFilterSpec? = null,
)

/**
 * The plain `dialogFilter` a caller asks to create or replace.
 *
 * [title] is the folder's label and the flags are the layer's own include/exclude selectors. The
 * peer lists take registered [PeerTarget]s, so a filter read from `messagesGetDialogFilters` can be
 * edited and written back unchanged. The `dialogFilterChatlist` constructor is not built here.
 */
@Serializable
internal data class DialogFilterSpec(
    val title: String,
    val contacts: Boolean = false,
    val nonContacts: Boolean = false,
    val groups: Boolean = false,
    val broadcasts: Boolean = false,
    val bots: Boolean = false,
    val excludeMuted: Boolean = false,
    val excludeRead: Boolean = false,
    val excludeArchived: Boolean = false,
    val pinnedPeers: List<PeerTarget> = emptyList(),
    val includePeers: List<PeerTarget> = emptyList(),
    val excludePeers: List<PeerTarget> = emptyList(),
)

/** Payload of `messagesUpdateDialogFiltersOrder`. */
@Serializable
internal data class UpdateDialogFiltersOrderPayload(val order: List<Int>)

/**
 * A bare peer a chatlist answer reported without objects to resolve it against.
 *
 * Only `chatlistsGetLeaveChatlistSuggestions` reaches this: every other answer carries the chats
 * and users that describe its peers, which arrive as full [Peer] values.
 */
@Serializable
internal data class ChatlistPeer(
    val id: Long = 0,
    val kind: String = "user",
)

/** One exported invite of a folder. */
@Serializable
internal data class ExportedInvite(
    val title: String = "",
    val slug: String = "",
    val url: String = "",
    val peers: List<ChatlistPeer> = emptyList(),
)

/** Result of `chatlistsGetExportedInvites`: the folder's invites and the objects they name. */
@Serializable
internal data class ExportedInvitesResult(
    val invites: List<ExportedInvite> = emptyList(),
    val chats: List<Peer> = emptyList(),
    val users: List<User> = emptyList(),
)

/**
 * Result of `chatlistsCheckChatlistInvite`, in both of the layer's cases.
 *
 * [kind] is `new` for a still-unjoined invite, which carries [title], [titleEntities], [emoticon]
 * and the [peers] joining would add, or `already` for a slug the account has already joined, which
 * carries the folder's [filterId] and sorts the peers into [missingPeers] and [alreadyPeers]. The
 * fields of the other case stay at their defaults, so a caller reads [kind] before either group.
 */
@Serializable
internal data class ChatlistInviteResult(
    val kind: String = "new",
    val titleNoanimate: Boolean = false,
    val title: String = "",
    val titleEntities: List<MessageEntity> = emptyList(),
    val emoticon: String? = null,
    val peers: List<Peer> = emptyList(),
    val filterId: Int = 0,
    val missingPeers: List<Peer> = emptyList(),
    val alreadyPeers: List<Peer> = emptyList(),
    val chats: List<Peer> = emptyList(),
    val users: List<User> = emptyList(),
)

/** Result of `chatlistsGetChatlistUpdates`: the peers a joined folder still misses. */
@Serializable
internal data class ChatlistUpdatesResult(
    val missingPeers: List<Peer> = emptyList(),
    val chats: List<Peer> = emptyList(),
    val users: List<User> = emptyList(),
)

/**
 * Result of the `Updates`-returning chatlist calls: an acknowledgement and the chats they touched.
 *
 * The point updates themselves arrive through the update stream, so only the chats the bundle
 * reported travel here.
 */
@Serializable
internal data class ChatlistUpdatesAck(
    val ok: Boolean = false,
    val peers: List<Peer> = emptyList(),
)

/** Result of `chatlistsGetLeaveChatlistSuggestions`: the peers a caller could leave. */
@Serializable
internal data class LeaveChatlistSuggestionsResult(
    val peers: List<ChatlistPeer> = emptyList(),
)

/** Payload of an operation that names a folder by its dialog-filter id. */
@Serializable
internal data class ChatlistPayload(val filterId: Int)

/** Payload of `chatlistsExportChatlistInvite`: the folder, the invite's label and its peers. */
@Serializable
internal data class ExportChatlistInvitePayload(
    val filterId: Int,
    val title: String,
    val peers: List<PeerTarget> = emptyList(),
)

/** Payload of `chatlistsEditExportedInvite`; an absent field leaves that part of the invite alone. */
@Serializable
internal data class EditExportedInvitePayload(
    val filterId: Int,
    val slug: String,
    val title: String? = null,
    val peers: List<PeerTarget>? = null,
)

/** Payload of `chatlistsDeleteExportedInvite`: the folder and the slug to drop. */
@Serializable
internal data class DeleteExportedInvitePayload(val filterId: Int, val slug: String)

/** Payload of `chatlistsCheckChatlistInvite`: the slug to look up. */
@Serializable
internal data class CheckChatlistInvitePayload(val slug: String)

/** Payload of `chatlistsJoinChatlistInvite`: the slug and the peers to join. */
@Serializable
internal data class JoinChatlistInvitePayload(
    val slug: String,
    val peers: List<PeerTarget> = emptyList(),
)

/** Payload of `chatlistsJoinChatlistUpdates` and `chatlistsLeaveChatlist`. */
@Serializable
internal data class ChatlistPeersPayload(
    val filterId: Int,
    val peers: List<PeerTarget> = emptyList(),
)
