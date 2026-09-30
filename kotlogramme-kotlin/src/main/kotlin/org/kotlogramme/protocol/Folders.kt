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
