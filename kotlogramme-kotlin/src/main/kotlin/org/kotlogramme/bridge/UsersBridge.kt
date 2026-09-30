package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.Transport
import org.kotlogramme.protocol.ContactUser
import org.kotlogramme.protocol.GetUsersPayload

/**
 * User lookup by numeric id, built on grammers' TL layer because it exposes no typed users API.
 *
 * The answer reuses the contacts projection, so a resolved account arrives with the same fields
 * and the same registered peer handle the contacts family returns.
 */
internal interface UsersBridge {
    val transport: Transport

    /**
     * Resolves each id to the account and registered peer it names.
     *
     * A repeated id is asked for once and an id Telegram does not resolve is absent from the
     * answer, so the result can be shorter than [ids].
     */
    @Operation("getUsers")
    fun getUsers(ids: List<Long>): List<ContactUser> =
        transport.request("getUsers", GetUsersPayload(ids))
}

/** The [UsersBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class UsersOperations(override val transport: Transport) : UsersBridge
