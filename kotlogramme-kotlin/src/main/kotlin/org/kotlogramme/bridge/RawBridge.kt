package org.kotlogramme.bridge

import org.kotlogramme.Transport

/**
 * Typed wrappers around raw Telegram methods that grammers does not surface, such as contacts and
 * account management.
 *
 * Placeholder: no operation is routed here yet. A follow-up task adds its methods here and their
 * names to `native/operations.txt`, without touching any other domain.
 */
internal interface RawBridge {
    val transport: Transport
}

/** The [RawBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class RawOperations(override val transport: Transport) : RawBridge
