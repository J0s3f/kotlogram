package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the authentication operations. */

/** Payload of `requestLoginCode`. */
@Serializable
internal data class PhonePayload(val phone: String)

/** Payload of `signIn`. */
@Serializable
internal data class CodePayload(val code: String)

/** Payload of `checkPassword`. */
@Serializable
internal data class PasswordPayload(val password: String)

/** Result of `signIn`, which may or may not carry the authorized account. */
@Serializable
internal data class SignInResponse(
    val status: String,
    val user: User? = null,
    val hint: String? = null,
)

/** Payload of `getMe`, which takes no fields. */
@Serializable
internal object EmptyPayload

/**
 * Result of `getMe`: the signed-in account together with the data centre its session is homed on.
 *
 * grammers reports the two separately — the account through `users.getUsers`, the data centre
 * through the session — so the bridge projects them together.
 */
@Serializable
internal data class Me(
    val user: User,
    val dataCentreId: Int,
)

/** Result of `getDataCentreId`: the home data centre of the session. */
@Serializable
internal data class DataCentreResult(val dataCentreId: Int)
