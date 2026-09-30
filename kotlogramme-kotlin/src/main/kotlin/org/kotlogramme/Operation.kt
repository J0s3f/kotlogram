package org.kotlogramme

import org.kotlogramme.bridge.ActionsBridge
import org.kotlogramme.bridge.AccountBridge
import org.kotlogramme.bridge.AuthBridge
import org.kotlogramme.bridge.ChatsBridge
import org.kotlogramme.bridge.ContactsBridge
import org.kotlogramme.bridge.DialogsBridge
import org.kotlogramme.bridge.FilesBridge
import org.kotlogramme.bridge.InlineBridge
import org.kotlogramme.bridge.MarkupBridge
import org.kotlogramme.bridge.MediaBridge
import org.kotlogramme.bridge.MessagesBridge
import org.kotlogramme.bridge.RawBridge
import org.kotlogramme.bridge.StickersBridge
import org.kotlogramme.bridge.UpdatesBridge
import org.kotlogramme.bridge.UsersBridge
import kotlin.reflect.KClass

/**
 * Marks a bridge method as the Kotlin side of native operation [name].
 *
 * The name must appear in `native/operations.txt`, which lists every operation the Rust crate
 * answers: the names dispatched through `request` plus the three that dedicated JNI exports
 * implement. `OperationParityTest` fails the build when the two sides disagree.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Operation(val name: String)

/**
 * Every native operation the Kotlin bridge declares, discovered by reflection.
 *
 * The operations live on the per-domain bridge interfaces that [TelegramClient] delegates to, so
 * each of them is listed here as well as the client itself. A new domain adds one entry.
 */
object OperationCatalog {
    private val BRIDGES: List<KClass<*>> = listOf(
        TelegramClient::class,
        AccountBridge::class,
        AuthBridge::class,
        MessagesBridge::class,
        ChatsBridge::class,
        ContactsBridge::class,
        DialogsBridge::class,
        UpdatesBridge::class,
        MediaBridge::class,
        FilesBridge::class,
        InlineBridge::class,
        ActionsBridge::class,
        MarkupBridge::class,
        StickersBridge::class,
        RawBridge::class,
        UsersBridge::class,
    )

    val names: Set<String> = BRIDGES
        .flatMap { bridge -> bridge.java.declaredMethods.asList() }
        .mapNotNull { method -> method.getAnnotation(Operation::class.java)?.name }
        .toSortedSet()
}
