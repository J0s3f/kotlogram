package com.github.badoualy.telegram.api

import org.kotlogramme.TelegramClient as GrammersClient

/**
 * The default [TelegramClient], delegating to a grammers bridge.
 *
 * It only supplies what the domain interfaces cannot derive from the bridge on their own: the
 * application's API hash for bot sign-in, and the closed flag.
 */
internal class InternalTelegramClient(
    override val bridge: GrammersClient,
    private val application: TelegramApp,
    @Suppress("unused") private val updateCallback: UpdateCallback?,
) : TelegramClient {
    override fun isClosed(): Boolean = closed

    override fun authImportBotAuthorization(botAuthToken: String): Authorization =
        Authorization(bridge.signInBot(botAuthToken, application.apiHash).toCompatibility())

    override fun close() {
        if (!closed) {
            closed = true
            bridge.close()
        }
    }

    private var closed = false
}
