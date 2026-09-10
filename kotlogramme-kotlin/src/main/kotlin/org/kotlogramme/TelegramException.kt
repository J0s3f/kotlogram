package org.kotlogramme

/** Error returned by Telegram, grammers, or the native bridge. */
open class TelegramException(message: String) : RuntimeException(message)
