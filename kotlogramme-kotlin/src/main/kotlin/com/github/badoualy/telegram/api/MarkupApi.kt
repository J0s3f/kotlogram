package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.InlineButtonSpec
import org.kotlogramme.protocol.KeyboardButtonSpec
import org.kotlogramme.protocol.MarkupSpec

/**
 * Reply markups: reading the one a message carries and building the four grammers can send.
 *
 * grammers attaches a markup to an outgoing message through `InputMessage::reply_markup`, which the
 * message operations own, so a built markup is handed back rather than sent; [markupGetReplyMarkup]
 * reads one off a message so a bot can copy the layout of a markup Telegram already accepts.
 */
interface MarkupApi : BridgeApi {
    /**
     * Reads the markup a message carries, which is grammers' `Message::reply_markup`.
     *
     * Answers null for a message that carries no markup, which is every message not sent by a bot.
     */
    fun markupGetReplyMarkup(peer: TelegramPeer, messageId: Int): ReplyMarkup? =
        bridge.getReplyMarkup(peer.native, messageId)?.toCompatibility()

    /**
     * Builds the markup grammers' `reply_markup::inline` builds: buttons attached to the message.
     *
     * [buttons] is the row matrix Telegram renders, so each inner list is one row, left to right.
     * Only [Button.Url], [Button.WebView], [Button.Callback] and [Button.SwitchInline] belong in an
     * inline markup; the others are keyboard buttons and are refused.
     */
    fun markupBuildInline(buttons: List<List<Button>>): ReplyMarkup =
        bridge.buildInlineMarkup(buttons.map { row -> row.map { it.asInlineSpec() } }).toCompatibility()

    /**
     * Builds the markup grammers' `reply_markup::keyboard` builds: a custom keyboard.
     *
     * Only [Button.Text], [Button.RequestPhone], [Button.RequestGeo] and [Button.RequestPoll] belong
     * in a custom keyboard; the inline kinds are refused. The layer's `persistent` flag and
     * input-field placeholder have no grammers builder method, so a built keyboard reports them
     * unset.
     */
    fun markupBuildKeyboard(
        buttons: List<List<Button>>,
        fitSize: Boolean = false,
        singleUse: Boolean = false,
        selective: Boolean = false,
    ): ReplyMarkup = bridge.buildReplyKeyboard(
        buttons.map { row -> row.map { it.asKeyboardSpec() } },
        fitSize = fitSize,
        singleUse = singleUse,
        selective = selective,
    ).toCompatibility()

    /**
     * Builds the markup grammers' `reply_markup::force_reply` builds: a one-off prompt to type a
     * reply.
     *
     * As with the keyboard, grammers' builder cannot set an input-field placeholder.
     */
    fun markupBuildForceReply(singleUse: Boolean = false, selective: Boolean = false): ReplyMarkup =
        bridge.buildForceReply(singleUse = singleUse, selective = selective).toCompatibility()

    /**
     * Builds the markup grammers' `reply_markup::hide` builds, which removes the custom keyboard
     * this bot showed earlier.
     */
    fun markupBuildHide(selective: Boolean = false): ReplyMarkup =
        bridge.buildHideKeyboard(selective = selective).toCompatibility()
}

/**
 * The button a grammers inline markup can carry, which is one of the four the button constructors
 * it takes cover.
 *
 * A read-only field the constructors have no parameter for — [Button.Callback.requiresPassword] and
 * [Button.SwitchInline.peerTypes] — is dropped here, exactly as the keyboard `persistent` flag is
 * dropped from a built keyboard.
 */
internal fun Button.asInlineSpec(): InlineButtonSpec = when (this) {
    is Button.Url -> InlineButtonSpec.Url(text, requireField(url, "url"))
    is Button.WebView -> InlineButtonSpec.WebView(text, requireField(url, "url"))
    // `data` is null only for a payload that came back as binary, which cannot be rebuilt.
    is Button.Callback -> InlineButtonSpec.Callback(text, requireField(data, "data"))
    // An absent `samePeer` is grammers' `switch_inline`, which keeps the current peer.
    is Button.SwitchInline -> InlineButtonSpec.SwitchInline(
        text,
        requireField(query, "query"),
        samePeer,
    )
    else -> throw IllegalArgumentException("grammers builds no inline button of kind '$kind'")
}

/**
 * The button a grammers custom keyboard can carry, which is one of the four the keyboard button
 * constructors it takes cover.
 *
 * A received poll button reports `null` when the layer left the flag unset, which is the same
 * request grammers' `request_poll` sends.
 */
internal fun Button.asKeyboardSpec(): KeyboardButtonSpec = when (this) {
    is Button.Text -> KeyboardButtonSpec.Text(text)
    is Button.RequestPhone -> KeyboardButtonSpec.RequestPhone(text)
    is Button.RequestGeo -> KeyboardButtonSpec.RequestGeo(text)
    is Button.RequestPoll -> KeyboardButtonSpec.RequestPoll(text, quiz ?: false)
    else -> throw IllegalArgumentException("grammers builds no keyboard button of kind '$kind'")
}

/** The [name] field this button needs to build, which a received button of that kind always has. */
private fun Button.requireField(value: String?, name: String): String =
    requireNotNull(value) { "a $kind button carries no $name" }

/**
 * Converts this markup to the wire spec a send payload carries.
 *
 * The fields a spec cannot carry — [ReplyMarkup.Keyboard.persistent] and
 * [ReplyMarkup.Keyboard.placeholder], which grammers' builder cannot set — are dropped, exactly as
 * the build operations drop them. An [ReplyMarkup.Unknown] has no grammers constructor, so it
 * cannot be sent.
 */
internal fun ReplyMarkup.asSpec(): MarkupSpec = when (this) {
    is ReplyMarkup.Inline -> MarkupSpec.Inline(rows.map { row -> row.map { it.asInlineSpec() } })
    is ReplyMarkup.Keyboard -> MarkupSpec.Keyboard(
        rows.map { row -> row.map { it.asKeyboardSpec() } },
        fitSize = fitSize,
        singleUse = singleUse,
        selective = selective,
    )
    is ReplyMarkup.ForceReply -> MarkupSpec.ForceReply(singleUse = singleUse, selective = selective)
    is ReplyMarkup.Hide -> MarkupSpec.Hide(selective = selective)
    is ReplyMarkup.Unknown -> throw IllegalArgumentException("grammers builds no markup of kind '$kind'")
}
