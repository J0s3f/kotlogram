package com.github.badoualy.telegram.api

/**
 * Reserved compatibility hook for the original Kotlogram callback type.
 *
 * Update streaming is available through [TelegramClient.getNextUpdate]. This legacy callback is
 * retained for source compatibility but is not automatically dispatched, because forwarding
 * stale Layer-66 update objects would produce misleading compatibility semantics.
 */
fun interface UpdateCallback {
    fun onUpdate(client: TelegramClient)
}
