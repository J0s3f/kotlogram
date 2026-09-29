package org.kotlogramme.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
