package com.github.badoualy.telegram.api

/**
 * A callback into the update stream, mirroring Kotlogram's `UpdateCallback`.
 *
 * Unlike the reserved one-argument hook the facade started with, this one carries the projected
 * update, so a handler can act on the payload instead of only knowing that something arrived.
 * Dispatching it is a blocking wait, so a caller that wants a loop drives
 * [UpdatesApi.dispatchNextUpdate] itself.
 */
fun interface UpdateCallback {
    fun onUpdate(client: TelegramClient, update: TypedUpdate)
}
