package com.github.badoualy.telegram.api

/**
 * Reserved compatibility hook for the original Kotlogram callback type.
 *
 * Update streaming is intentionally not dispatched through this callback yet. The bridge first
 * maps request/response operations and will expose grammers' ordered update stream in a dedicated
 * follow-up API instead of forwarding stale Layer-66 update objects.
 */
fun interface UpdateCallback {
    fun onUpdate(client: TelegramClient)
}
