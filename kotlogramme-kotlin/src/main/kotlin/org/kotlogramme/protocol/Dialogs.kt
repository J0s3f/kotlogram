package org.kotlogramme.protocol

import kotlinx.serialization.Serializable

/** Payloads and results of the dialog operations. */

/** Payload of `getDialogs` and `getDialogsMeta`. */
@Serializable
internal data class LimitPayload(val limit: Int)

/** Result of `getDialogsTotal`. */
@Serializable
internal data class DialogsTotal(val total: Long)
