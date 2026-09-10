package com.github.badoualy.telegram.api

import java.nio.file.Path

/**
 * Location of the persistent grammers SQLite session.
 *
 * Kotlogram exposed the individual MTProto session parts through this abstraction. grammers
 * persists those parts atomically in its own SQLite format, so implementations only supply the
 * session file location.
 */
interface TelegramApiStorage {
    val sessionPath: Path
}

data class FileTelegramApiStorage(override val sessionPath: Path) : TelegramApiStorage
