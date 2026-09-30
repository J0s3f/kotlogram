package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.BlockContactPayload
import org.kotlogramme.protocol.BlockedContacts
import org.kotlogramme.protocol.ContactImport
import org.kotlogramme.protocol.ContactsPage
import org.kotlogramme.protocol.DeleteContactsPayload
import org.kotlogramme.protocol.FoundContacts
import org.kotlogramme.protocol.GetBlockedPayload
import org.kotlogramme.protocol.GetContactsPayload
import org.kotlogramme.protocol.ImportContactsPayload
import org.kotlogramme.protocol.ImportedContacts
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.SearchContactsPayload

/**
 * The typed contacts family, built on grammers' TL layer because it exposes no high-level contacts
 * API.
 */
internal interface ContactsBridge {
    val transport: Transport

    /**
     * Lists the account's saved contacts.
     *
     * A non-zero [hash] asks Telegram for the incremental answer; with no change it reports
     * `notModified` instead of a list.
     */
    @Operation("contactsGetContacts")
    fun contactsGetContacts(hash: Long = 0): ContactsPage =
        transport.request("contactsGetContacts", GetContactsPayload(hash))

    /** Imports phone contacts, answering the imported, retried and popular entries. */
    @Operation("contactsImportContacts")
    fun contactsImportContacts(contacts: List<ContactImport>): ImportedContacts =
        transport.request("contactsImportContacts", ImportContactsPayload(contacts))

    /** Deletes saved contacts. */
    @Operation("contactsDeleteContacts")
    fun contactsDeleteContacts(users: List<Peer>) {
        transport.request<DeleteContactsPayload, OperationResult>(
            "contactsDeleteContacts",
            DeleteContactsPayload(users.map { PeerTarget(it.nativeHandle) }),
        )
    }

    /** Blocks a peer. */
    @Operation("contactsBlock")
    fun contactsBlock(peer: Peer, myStoriesFrom: Boolean = false) {
        transport.request<BlockContactPayload, OperationResult>(
            "contactsBlock",
            BlockContactPayload(PeerTarget(peer.nativeHandle), myStoriesFrom),
        )
    }

    /** Unblocks a peer. */
    @Operation("contactsUnblock")
    fun contactsUnblock(peer: Peer, myStoriesFrom: Boolean = false) {
        transport.request<BlockContactPayload, OperationResult>(
            "contactsUnblock",
            BlockContactPayload(PeerTarget(peer.nativeHandle), myStoriesFrom),
        )
    }

    /** Lists the account's blocked peers. */
    @Operation("contactsGetBlocked")
    fun contactsGetBlocked(
        myStoriesFrom: Boolean = false,
        offset: Int = 0,
        limit: Int = 100,
    ): BlockedContacts =
        transport.request("contactsGetBlocked", GetBlockedPayload(myStoriesFrom, offset, limit))

    /** Searches the account's contacts and the public directory. */
    @Operation("contactsSearch")
    fun contactsSearch(
        q: String,
        broadcasts: Boolean = false,
        bots: Boolean = false,
        limit: Int = 50,
    ): FoundContacts =
        transport.request("contactsSearch", SearchContactsPayload(q, broadcasts, bots, limit))
}

/** The [ContactsBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class ContactsOperations(override val transport: Transport) : ContactsBridge
