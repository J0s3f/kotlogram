package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payload of `getUsers`: the numeric user ids to resolve, in the order the caller gave them. */
@Serializable
internal data class GetUsersPayload(val ids: List<Long>)
