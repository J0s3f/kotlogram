package org.kotlogramme.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/** Wire models and payloads of the reply-markup operations. */

/**
 * A reply markup, mirroring the four shapes grammers' `reply_markup` module builds.
 *
 * grammers builds a markup through `reply_markup::{inline, keyboard, force_reply, hide}` and hands
 * back an opaque value; it reads one off a message through `Message::reply_markup`, which returns
 * the raw layer value. Both sides are the same four shapes, so this is one flat object whose [kind]
 * says which shape it is and whose other fields are populated by it alone.
 *
 * [kind] is the grammers `reply_markup` function for the four shapes. The fields a shape cannot
 * answer are the defaults rather than absent, so a decoder never has to tell "not set" from "not
 * modelled".
 */
@Serializable
data class ReplyMarkup(
    val kind: String,
    val rows: List<List<Button>> = emptyList(),
    /** grammers' `Keyboard::fit_size`, the layer's `resize` flag. Keyboard only. */
    val fitSize: Boolean = false,
    /** grammers' `Keyboard::single_use` and `ForceReply::single_use`. */
    val singleUse: Boolean = false,
    /** grammers' `Keyboard::selective`, `Hide::selective` and `ForceReply::selective`. */
    val selective: Boolean = false,
    /**
     * The layer's `persistent` flag, which keeps a custom keyboard after the bot is gone.
     *
     * grammers' builder offers no method for it, so a markup this library builds always reports
     * `false`; only one read off a received message can report `true`.
     */
    val persistent: Boolean = false,
    /**
     * The input-field placeholder of a keyboard or a force-reply.
     *
     * As with [persistent], grammers' builder cannot set it, so a built markup always reports null.
     */
    val placeholder: String? = null,
)

/**
 * One button of a reply markup.
 *
 * Every field is always present on the wire; the ones this button's [kind] cannot answer are null,
 * so a decoder sees one shape rather than one per button kind.
 */
@Serializable
data class Button(
    val kind: String,
    val text: String,
    val url: String? = null,
    /**
     * A `callback` button's payload.
     *
     * grammers' `button::inline` accepts arbitrary bytes, but the wire is JSON, so a payload that is
     * not valid UTF-8 arrives as null: a bot that put binary data in a callback button cannot read
     * it back. A text payload always survives unchanged.
     */
    val data: String? = null,
    /** A `callback` button's `requires_password` flag, which grammers' builder always clears. */
    val requiresPassword: Boolean? = null,
    /** The confirmation label of a `urlAuth` or `inputUrlAuth` button. */
    val fwdText: String? = null,
    /** The identifier of a `urlAuth`, `inputUrlAuth`, `requestPeer` or `inputRequestPeer` button. */
    val buttonId: Int? = null,
    /** The pre-filled query of a `switchInline` button. */
    val query: String? = null,
    /**
     * grammers' `switch_inline` sets this, `switch_inline_elsewhere` clears it, asking the user to
     * pick a peer first.
     */
    val samePeer: Boolean? = null,
    /** The peer types a `switchInline` button accepts, in lowerCamelCase. */
    val peerTypes: List<String>? = null,
    /**
     * A `requestPoll` button: `true` is grammers' `request_quiz`, `false` its `request_poll`, and
     * null a poll of unspecified kind, which only a received markup carries.
     */
    val quiz: Boolean? = null,
    /** A `userProfile` button. */
    val userId: Long? = null,
    /** A `copy` button's text to place on the clipboard. */
    val copyText: String? = null,
    /** A `requestPeer` or `inputRequestPeer` button's cap on how many peers may be picked. */
    val maxQuantity: Int? = null,
    /**
     * An `inputUrlAuth` button's `request_write_access` flag, asking for permission to message the
     * bot that placed the button.
     */
    val requestWriteAccess: Boolean? = null,
)

/**
 * A reply markup a send or edit payload may carry, tagged by the grammers `reply_markup` function
 * that builds it.
 *
 * This is the request-side shape of the four markups the build operations project: the same
 * fields the four build payloads accept, so a markup read off one message can be sent on another.
 * The boolean options default to `false`, exactly as the build payloads do.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
internal sealed class MarkupSpec {
    @Serializable
    @SerialName("inline")
    data class Inline(val rows: List<List<InlineButtonSpec>>) : MarkupSpec()

    @Serializable
    @SerialName("keyboard")
    data class Keyboard(
        val rows: List<List<KeyboardButtonSpec>>,
        val fitSize: Boolean = false,
        val singleUse: Boolean = false,
        val selective: Boolean = false,
    ) : MarkupSpec()

    @Serializable
    @SerialName("forceReply")
    data class ForceReply(
        val singleUse: Boolean = false,
        val selective: Boolean = false,
    ) : MarkupSpec()

    @Serializable
    @SerialName("hide")
    data class Hide(val selective: Boolean = false) : MarkupSpec()
}

/**
 * Converts this projection to the wire spec a send payload carries.
 *
 * The projection's button `kind` becomes the spec's button `type`, and the fields a spec cannot
 * carry — [ReplyMarkup.persistent] and [ReplyMarkup.placeholder], which grammers' builder cannot
 * set — are dropped, exactly as the build operations drop them.
 */
internal fun ReplyMarkup.asSpec(): MarkupSpec = when (kind) {
    "inline" -> MarkupSpec.Inline(rows.map { row -> row.map { it.asInlineSpec() } })
    "keyboard" -> MarkupSpec.Keyboard(
        rows.map { row -> row.map { it.asKeyboardSpec() } },
        fitSize = fitSize,
        singleUse = singleUse,
        selective = selective,
    )
    "forceReply" -> MarkupSpec.ForceReply(singleUse = singleUse, selective = selective)
    "hide" -> MarkupSpec.Hide(selective = selective)
    else -> throw IllegalArgumentException("grammers builds no markup of kind '$kind'")
}

/**
 * The button a grammers inline markup can carry, which is one of the four the button constructors
 * it takes cover.
 *
 * A read-only field the constructors have no parameter for — [Button.requiresPassword] and
 * [Button.peerTypes] — is dropped here, exactly as the keyboard `persistent` flag is dropped from a
 * built keyboard.
 */
internal fun Button.asInlineSpec(): InlineButtonSpec = when (kind) {
    "url" -> InlineButtonSpec.Url(text, requireField(url, "url"))
    "webView" -> InlineButtonSpec.WebView(text, requireField(url, "url"))
    // `data` is null only for a payload that came back as binary, which cannot be rebuilt.
    "callback" -> InlineButtonSpec.Callback(text, requireField(data, "data"))
    // An absent `samePeer` is grammers' `switch_inline`, which keeps the current peer.
    "switchInline" -> InlineButtonSpec.SwitchInline(text, requireField(query, "query"), samePeer)
    else -> throw IllegalArgumentException("grammers builds no inline button of kind '$kind'")
}

/**
 * The button a grammers custom keyboard can carry, which is one of the four the keyboard button
 * constructors it takes cover.
 *
 * A received poll button reports `null` when the layer left the flag unset, which is the same
 * request grammers' `request_poll` sends.
 */
internal fun Button.asKeyboardSpec(): KeyboardButtonSpec = when (kind) {
    "text" -> KeyboardButtonSpec.Text(text)
    "requestPhone" -> KeyboardButtonSpec.RequestPhone(text)
    "requestGeo" -> KeyboardButtonSpec.RequestGeo(text)
    "requestPoll" -> KeyboardButtonSpec.RequestPoll(text, quiz ?: false)
    else -> throw IllegalArgumentException("grammers builds no keyboard button of kind '$kind'")
}

/** The [name] field this button needs to build, which a received button of that kind always has. */
private fun Button.requireField(value: String?, name: String): String =
    requireNotNull(value) { "a $kind button carries no $name" }

/** Payload of `getReplyMarkup`. */
@Serializable
internal data class MessageMarkupPayload(
    val peerHandle: Long? = null,
    val username: String? = null,
    val messageId: Int,
) {
    constructor(peer: PeerTarget, messageId: Int) : this(peer.peerHandle, peer.username, messageId)
}

/** Payload of `buildInlineMarkup`. */
@Serializable
internal data class InlineMarkupPayload(val rows: List<List<InlineButtonSpec>>)

/**
 * A button an inline markup may carry, one variant per grammers `button` function usable there.
 *
 * grammers' inline buttons cover the callback button, `switch_inline`, `switch_inline_elsewhere`,
 * `url` and `webview`, and nothing else: `button::text` returns a keyboard button, so a plain label
 * cannot go in an inline markup at all.
 */
@Serializable
internal sealed class InlineButtonSpec {
    @Serializable
    @SerialName("url")
    data class Url(val text: String, val url: String) : InlineButtonSpec()

    @Serializable
    @SerialName("webView")
    data class WebView(val text: String, val url: String) : InlineButtonSpec()

    @Serializable
    @SerialName("callback")
    data class Callback(val text: String, val data: String) : InlineButtonSpec()

    @Serializable
    @SerialName("switchInline")
    data class SwitchInline(
        val text: String,
        val query: String,
        /**
         * Null is grammers' `switch_inline`, which keeps the current peer; false is
         * `switch_inline_elsewhere`, which asks the user to pick one.
         */
        val samePeer: Boolean? = null,
    ) : InlineButtonSpec()
}

/** Payload of `buildReplyKeyboard`. */
@Serializable
internal data class KeyboardMarkupPayload(
    val rows: List<List<KeyboardButtonSpec>>,
    val fitSize: Boolean = false,
    val singleUse: Boolean = false,
    val selective: Boolean = false,
)

/**
 * A button a custom reply keyboard may carry, one variant per grammers `button` function usable
 * there. The inline kinds are absent because grammers' `reply_markup::keyboard` takes a matrix of
 * keyboard buttons, which they are not.
 */
@Serializable
internal sealed class KeyboardButtonSpec {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : KeyboardButtonSpec()

    @Serializable
    @SerialName("requestPhone")
    data class RequestPhone(val text: String) : KeyboardButtonSpec()

    @Serializable
    @SerialName("requestGeo")
    data class RequestGeo(val text: String) : KeyboardButtonSpec()

    @Serializable
    @SerialName("requestPoll")
    data class RequestPoll(val text: String, val quiz: Boolean = false) : KeyboardButtonSpec()
}

/** Payload of `buildForceReply`. */
@Serializable
internal data class ForceReplyPayload(
    val singleUse: Boolean = false,
    val selective: Boolean = false,
)

/** Payload of `buildHideKeyboard`. */
@Serializable
internal data class HideKeyboardPayload(val selective: Boolean = false)
