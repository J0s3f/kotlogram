package com.github.badoualy.telegram.api

import org.kotlogramme.TelegramClient as GrammersClient

/**
 * Kotlogram-shaped API mapped onto grammers' current high-level client operations.
 *
 * The interface is composed of one file per domain, so a new feature area is added as its own
 * `*Api.kt` plus one supertype here. Each domain declares its methods with default implementations
 * that delegate to the direct grammers bridge.
 */
interface TelegramClient : AutoCloseable, BridgeApi, AuthApi, MessagesApi, ChatsApi, ContactsApi, DialogsApi,
    UpdatesApi, MediaApi, FilesApi, InlineApi, ActionsApi, MarkupApi, AccountApi, StickersApi, FoldersApi, RawApi, UsersApi {
    fun isAuthorized(): Boolean = bridge.isAuthorized()
    fun isClosed(): Boolean

    /** Retained for source compatibility; grammers manages request timeouts internally. */
    fun setTimeout(timeout: Long) = Unit

    /** Retained for source compatibility; grammers manages exported DC connections internally. */
    fun setExportedClientTimeout(timeout: Long) = Unit

    /** Retained for source compatibility with Kotlogram's shutdown flag. */
    fun close(shutdown: Boolean) = close()

    /** grammers' file operations use the same client/session, so no secondary client is needed. */
    fun getDownloaderClient(): TelegramClient = this
}

/**
 * The direct grammers bridge the domain interfaces' default implementations call.
 *
 * This is an implementation detail of this library, not part of the Kotlogram compatibility
 * surface; callers should not depend on it.
 */
interface BridgeApi {
    val bridge: GrammersClient
}
