package com.github.badoualy.telegram.api

/**
 * User lookup by numeric id.
 *
 * A message's inline-bot origin is projected as a bare `viaBotId`, and
 * [contactsResolveUsername] resolves the other direction; this is the missing half that turns
 * such an id into the account behind it.
 */
interface UsersApi : BridgeApi {
    /**
     * Resolves each id to the account it names, in the order the ids were given.
     *
     * A repeated id is resolved once and an id Telegram does not resolve is absent from the
     * answer, so the result can be shorter than [ids].
     */
    fun usersGetUsers(ids: List<Long>): List<User> =
        bridge.getUsers(ids).map { it.user.toCompatibility() }
}
