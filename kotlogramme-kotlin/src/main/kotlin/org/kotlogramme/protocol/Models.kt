package org.kotlogramme.protocol

import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * Wire models exchanged with the native bridge.
 *
 * These mirror the JSON the Rust side produces and consumes. They are a transport detail of this
 * library rather than the Kotlogram compatibility surface, which lives in
 * `com.github.badoualy.telegram.api`. The entities each live in their own file next to this one,
 * named after what they describe: `User`, `Peer`, `Message`, `Media`, `Dialog`, `Participant`,
 * `Permissions` and `Update`.
 */

/**
 * A file queued for an album: either a local [path] to upload, or the [fileHandle] of an upload
 * that already ran. Exactly one of the two is set.
 */
data class OutgoingMedia(
    val path: Path? = null,
    val caption: String = "",
    val asPhoto: Boolean = false,
    val fileHandle: Long? = null,
)

/** Result of `requestLoginCode`: the native side keeps the real Telegram token. */
@Serializable
data class LoginCodeSent(val phone: String)

/** Result of the code step of a user login. */
sealed interface SignInResult {
    data class Authorized(val user: User) : SignInResult
    data class PasswordRequired(val hint: String?) : SignInResult
}
