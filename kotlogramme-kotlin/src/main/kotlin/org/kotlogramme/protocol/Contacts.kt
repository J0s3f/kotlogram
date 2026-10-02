package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the typed contacts operations. */

/** Payload of `contactsGetContacts`; a non-zero [hash] asks for the incremental answer. */
@Serializable
internal data class GetContactsPayload(val hash: Long = 0)

/** Payload of `contactsImportContacts`. */
@Serializable
internal data class ImportContactsPayload(val contacts: List<ContactImport>)

/** One phone contact to import. */
@Serializable
internal data class ContactImport(
    /** The caller-assigned identifier Telegram echoes back in the imported/retry lists. */
    val clientId: Long,
    val phone: String,
    val firstName: String,
    val lastName: String = "",
)

/** Payload of `contactsDeleteContacts`. */
@Serializable
internal data class DeleteContactsPayload(val users: List<PeerTarget>)

/** Payload of `contactsBlock` and `contactsUnblock`. */
@Serializable
internal data class BlockContactPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    /** When set, only the block on story visibility is changed. */
    val myStoriesFrom: Boolean = false,
) {
    constructor(peer: PeerTarget, myStoriesFrom: Boolean = false) :
        this(peer.peerHandle, peer.username, myStoriesFrom)
}

/**
 * Payload of `contactsGetBlocked`.
 *
 * [all] returns every blocked peer by walking the layer's pages in one call. It wins over
 * [offset] and [limit].
 */
@Serializable
internal data class GetBlockedPayload(
    val myStoriesFrom: Boolean = false,
    val offset: Int = 0,
    val limit: Int = 100,
    val all: Boolean = false,
)

/** Payload of `contactsSearch`. */
@Serializable
internal data class SearchContactsPayload(
    val q: String,
    /** Restrict the global half of the answer to public channels. */
    val broadcasts: Boolean = false,
    /** Restrict the global half of the answer to bots. */
    val bots: Boolean = false,
    val limit: Int = 50,
)

/** A user together with the registered peer it projects to. */
@Serializable
internal data class ContactUser(val user: User, val peer: Peer)

/** One saved contact, enriched with the user it names. */
@Serializable
internal data class ContactEntry(
    val userId: Long,
    val mutual: Boolean = false,
    /** The account the id names, when the answer carried its user object. */
    val user: User? = null,
    val peer: Peer? = null,
)

/**
 * Result of `contactsGetContacts`.
 *
 * [notModified] is the layer's `contactsNotModified`, which carries no list at all.
 */
@Serializable
internal data class ContactsPage(
    val notModified: Boolean = false,
    val savedCount: Int = 0,
    val contacts: List<ContactEntry> = emptyList(),
    val users: List<ContactUser> = emptyList(),
)

/** One entry of `contactsImportContacts`'s imported list. */
@Serializable
internal data class ImportedContact(
    val userId: Long,
    val clientId: Long,
    val user: User? = null,
    val peer: Peer? = null,
)

/** One entry of `contactsImportContacts`'s popular-invite list. */
@Serializable
internal data class PopularInvite(val clientId: Long, val importers: Int)

/** Result of `contactsImportContacts`. */
@Serializable
internal data class ImportedContacts(
    val imported: List<ImportedContact> = emptyList(),
    /** The client ids Telegram could not import; the caller may retry them. */
    val retryContacts: List<Long> = emptyList(),
    val popularInvites: List<PopularInvite> = emptyList(),
    val users: List<ContactUser> = emptyList(),
)

/** A peer an answer referenced, resolved against the users the answer carried. */
@Serializable
internal data class ContactPeer(
    /** The account description when the peer is a user; null for a chat or channel. */
    val user: User? = null,
    val peer: Peer,
)

/** One blocked peer with the layer's block date, in epoch milliseconds. */
@Serializable
internal data class BlockedPeer(
    val date: Long,
    val user: User? = null,
    val peer: Peer,
)

/** Result of `contactsGetBlocked`; [count] is only present on the sliced answer. */
@Serializable
internal data class BlockedContacts(
    val count: Int? = null,
    val blocked: List<BlockedPeer> = emptyList(),
    val users: List<ContactUser> = emptyList(),
)

/** Result of `contactsSearch`: the viewer's contacts and the global matches. */
@Serializable
internal data class FoundContacts(
    val myResults: List<ContactPeer> = emptyList(),
    val results: List<ContactPeer> = emptyList(),
    val users: List<ContactUser> = emptyList(),
)
