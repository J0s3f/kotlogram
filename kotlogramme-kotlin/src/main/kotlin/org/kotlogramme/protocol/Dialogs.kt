package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the dialog operations. */

/**
 * Payload of `getDialogs` and `getDialogsMeta`.
 *
 * [offsetPeer], [offsetId] and [offsetDate] are the paging cursor: they resume the listing after
 * the dialog they name, and must all be present to have any effect. A partial cursor is ignored
 * and the listing starts from the top, because the native side would otherwise re-serve the first
 * page. [offsetDate] is epoch milliseconds, matching how the rest of the payloads spell dates.
 */
@Serializable
internal data class LimitPayload(
    val limit: Int,
    val offsetPeer: Long? = null,
    val offsetId: Int? = null,
    val offsetDate: Long? = null,
)

/** Result of `getDialogsTotal`. */
@Serializable
internal data class DialogsTotal(val total: Long)
