package com.github.badoualy.telegram.api

/**
 * The typed contacts family, mapped onto grammers' TL layer.
 *
 * These operations have no high-level grammers equivalent, so the bridge builds the raw
 * `contacts.*` requests; the facade keeps the same Kotlogram-shaped surface as the other domains.
 */
interface ContactsApi : BridgeApi {
    /**
     * Lists the account's saved contacts.
     *
     * A non-zero [hash] asks Telegram for the incremental answer, which reports
     * [ContactsPage.notModified] when nothing changed.
     */
    fun contactsGetContacts(hash: Long = 0): ContactsPage =
        bridge.contactsGetContacts(hash).toCompatibility()

    /** Imports phone contacts, answering the imported, retried and popular entries. */
    fun contactsImportContacts(contacts: List<ContactImport>): ImportedContacts =
        bridge.contactsImportContacts(contacts.map { it.toBridge() }).toCompatibility()

    /** Deletes saved contacts. */
    fun contactsDeleteContacts(users: List<TelegramPeer>) {
        bridge.contactsDeleteContacts(users.map { it.native })
    }

    /** Blocks a peer. With [myStoriesFrom] only the story-visibility block is changed. */
    fun contactsBlock(peer: TelegramPeer, myStoriesFrom: Boolean = false) {
        bridge.contactsBlock(peer.native, myStoriesFrom)
    }

    /** Unblocks a peer. With [myStoriesFrom] only the story-visibility block is changed. */
    fun contactsUnblock(peer: TelegramPeer, myStoriesFrom: Boolean = false) {
        bridge.contactsUnblock(peer.native, myStoriesFrom)
    }

    /** Lists the account's blocked peers. */
    fun contactsGetBlocked(
        myStoriesFrom: Boolean = false,
        offset: Int = 0,
        limit: Int = 100,
    ): BlockedContacts =
        bridge.contactsGetBlocked(myStoriesFrom, offset, limit).toCompatibility()

    /** Searches the account's contacts and the public directory. */
    fun contactsSearch(
        q: String,
        broadcasts: Boolean = false,
        bots: Boolean = false,
        limit: Int = 50,
    ): FoundContacts =
        bridge.contactsSearch(q, broadcasts, bots, limit).toCompatibility()
}
