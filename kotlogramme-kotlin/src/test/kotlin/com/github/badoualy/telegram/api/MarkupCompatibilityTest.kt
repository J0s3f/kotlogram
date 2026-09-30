package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.InlineButtonSpec
import org.kotlogramme.protocol.KeyboardButtonSpec
import org.kotlogramme.protocol.MarkupSpec
import org.kotlogramme.protocol.ReplyMarkup as BridgeReplyMarkup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade models every reply-markup shape and button kind the bridge
 * projects, and that only the buttons grammers can build are offered to the builders.
 *
 * The documents are the same ones `MarkupProtocolTest` pins on the wire side, so a button kind or a
 * markup shape added to `native/src/dto/markup.rs` without reaching this layer fails here.
 */
class MarkupCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an inline markup keeps its rows in the order Telegram renders them`() {
        val markup = json.decodeFromString<BridgeReplyMarkup>(INLINE).toCompatibility()

        assertEquals("inline", markup.kind)
        val inline = assertIs<ReplyMarkup.Inline>(markup)
        assertEquals(2, inline.rows.size)
        assertEquals(Button.Url("Open docs", "https://example.org/docs"), inline.rows[0][0])
        assertEquals(Button.Callback("Vote yes", "vote:yes", false), inline.rows[1][0])
        assertEquals(Button.Text("Vote no"), inline.rows[1][1])
    }

    @Test
    fun `a keyboard keeps the flags grammers cannot set on a built one`() {
        val markup = json.decodeFromString<BridgeReplyMarkup>(KEYBOARD).toCompatibility()

        val keyboard = assertIs<ReplyMarkup.Keyboard>(markup)
        assertTrue(keyboard.fitSize && keyboard.singleUse && keyboard.selective && keyboard.persistent)
        assertEquals("Pick one", keyboard.placeholder)
        assertEquals(Button.RequestPoll("Quiz", true), keyboard.rows[0][0])
    }

    @Test
    fun `a force reply and a hide carry no rows`() {
        val force = assertIs<ReplyMarkup.ForceReply>(
            json.decodeFromString<BridgeReplyMarkup>(FORCE_REPLY).toCompatibility(),
        )
        assertTrue(force.singleUse && force.selective)
        assertEquals("Type here", force.placeholder)

        val hide = assertIs<ReplyMarkup.Hide>(
            json.decodeFromString<BridgeReplyMarkup>(HIDE).toCompatibility(),
        )
        assertTrue(!hide.selective)
    }

    @Test
    fun `every button kind the native side projects reaches its own variant`() {
        assertEquals(KINDS.sorted(), BUTTONS.map { it.first }.sorted())
        for ((kind, expected) in BUTTONS) {
            assertEquals(expected, buttonOf(kind).toCompatibility(), "kind '$kind' lost its shape")
        }
    }

    @Test
    fun `a button kind this build cannot name still arrives whole`() {
        val button = buttonOf("someNewButton").toCompatibility()

        val unknown = assertIs<Button.Unknown>(button)
        assertEquals("someNewButton", unknown.kind)
        assertEquals("T", unknown.text)
        assertEquals("https://example.org", unknown.url)
        assertEquals(99L, unknown.userId)
    }

    @Test
    fun `a markup shape this build cannot name still arrives whole`() {
        val markup = json.decodeFromString<BridgeReplyMarkup>(MARKUP_UNKNOWN).toCompatibility()

        val unknown = assertIs<ReplyMarkup.Unknown>(markup)
        assertEquals("someNewMarkup", unknown.kind)
        assertTrue(unknown.fitSize && unknown.singleUse)
        assertEquals(listOf(listOf(Button.Text("Go"))), unknown.rows)
    }

    @Test
    fun `the builders take exactly the buttons grammers can put in an inline markup`() {
        assertEquals(
            InlineButtonSpec.Url("Docs", "https://example.org"),
            Button.Url("Docs", "https://example.org").asInlineSpec(),
        )
        assertEquals(
            InlineButtonSpec.WebView("Play", "https://example.org/game"),
            Button.WebView("Play", "https://example.org/game").asInlineSpec(),
        )
        assertEquals(
            InlineButtonSpec.Callback("Vote", "vote:yes"),
            // `requiresPassword` has no grammers constructor parameter, so building drops it.
            Button.Callback("Vote", "vote:yes", true).asInlineSpec(),
        )
        assertEquals(
            InlineButtonSpec.SwitchInline("Search", "cats ", false),
            Button.SwitchInline("Search", "cats ", samePeer = false, peerTypes = listOf("pm"))
                .asInlineSpec(),
        )

        // `text` is grammers' `button::text`, which is a keyboard button, and the request buttons
        // are too; a game button has no builder at all.
        for (button in listOf(Button.Text("Go"), Button.RequestPhone("Number"), Button.Game("Play"))) {
            assertFailsWith<IllegalArgumentException>("${button.kind} is not an inline button") {
                button.asInlineSpec()
            }
        }
    }

    @Test
    fun `the builders take exactly the buttons grammers can put in a custom keyboard`() {
        assertEquals(KeyboardButtonSpec.Text("Accept"), Button.Text("Accept").asKeyboardSpec())
        assertEquals(
            KeyboardButtonSpec.RequestPhone("Number"),
            Button.RequestPhone("Number").asKeyboardSpec(),
        )
        assertEquals(
            KeyboardButtonSpec.RequestGeo("Location"),
            Button.RequestGeo("Location").asKeyboardSpec(),
        )
        assertEquals(
            KeyboardButtonSpec.RequestPoll("Quiz", quiz = true),
            Button.RequestPoll("Quiz", quiz = true).asKeyboardSpec(),
        )
        // A poll button of unspecified kind is the same request grammers' `request_poll` sends.
        assertEquals(
            KeyboardButtonSpec.RequestPoll("Poll", quiz = false),
            Button.RequestPoll("Poll").asKeyboardSpec(),
        )

        // The inline kinds are `button::Inline`, which the keyboard does not take, and the auth,
        // copy and profile buttons have no builder at all.
        for (button in listOf(Button.Url("Docs", "https://example.org"), Button.Pay("Pay"), Button.Copy("Copy", "x"))) {
            assertFailsWith<IllegalArgumentException>("${button.kind} is not a keyboard button") {
                button.asKeyboardSpec()
            }
        }
    }

    @Test
    fun `a button missing the field a build needs is refused rather than sent blank`() {
        // A callback payload that came back as binary is null, and a switch-inline query is always
        // present; neither can be rebuilt.
        assertFailsWith<IllegalArgumentException> { Button.Callback("Vote").asInlineSpec() }
        assertFailsWith<IllegalArgumentException> { Button.SwitchInline("Search").asInlineSpec() }
        assertFailsWith<IllegalArgumentException> { Button.Url("Docs").asInlineSpec() }
    }

    @Test
    fun `a button always reports the wire kind it was projected from`() {
        assertEquals("webView", Button.WebView("Play", "u").kind)
        assertEquals("inputRequestPeer", Button.InputRequestPeer("Suggest").kind)
        assertEquals("forceReply", ReplyMarkup.ForceReply().kind)
        assertEquals("hide", ReplyMarkup.Hide().kind)
    }

    @Test
    fun `a markup converts to the spec a send carries`() {
        val inline = assertIs<MarkupSpec.Inline>(
            ReplyMarkup.Inline(
                listOf(
                    listOf(Button.Url("Docs", "https://example.org")),
                    listOf(Button.Callback("Vote", "vote:yes", false)),
                ),
            ).asSpec(),
        )
        assertEquals(InlineButtonSpec.Url("Docs", "https://example.org"), inline.rows[0][0])
        assertEquals(InlineButtonSpec.Callback("Vote", "vote:yes"), inline.rows[1][0])

        val keyboard = assertIs<MarkupSpec.Keyboard>(
            ReplyMarkup.Keyboard(
                rows = listOf(listOf(Button.Text("Go"), Button.RequestPoll("Quiz", true))),
                fitSize = true,
                singleUse = true,
                selective = true,
                // `persistent` and `placeholder` have no grammers builder method, so a spec drops them.
                persistent = true,
                placeholder = "Pick one",
            ).asSpec(),
        )
        assertTrue(keyboard.fitSize && keyboard.singleUse && keyboard.selective)
        assertEquals(KeyboardButtonSpec.Text("Go"), keyboard.rows[0][0])
        assertEquals(KeyboardButtonSpec.RequestPoll("Quiz", quiz = true), keyboard.rows[0][1])

        val force = assertIs<MarkupSpec.ForceReply>(
            ReplyMarkup.ForceReply(singleUse = true, selective = true).asSpec(),
        )
        assertTrue(force.singleUse && force.selective)

        val hide = assertIs<MarkupSpec.Hide>(ReplyMarkup.Hide(selective = true).asSpec())
        assertTrue(hide.selective)
    }

    @Test
    fun `a markup of an unknown kind cannot become a spec`() {
        val unknown = ReplyMarkup.Unknown(
            kind = "someNewMarkup",
            rows = listOf(listOf(Button.Text("Go"))),
        )
        assertFailsWith<IllegalArgumentException> { unknown.asSpec() }
    }

    /** A button document of [kind] with every field populated, so no variant can pass by accident. */
    private fun buttonOf(kind: String) = json.decodeFromString<org.kotlogramme.protocol.Button>(
        """
        {"kind": "$kind", "text": "T", "url": "https://example.org", "data": "payload",
         "requiresPassword": true, "fwdText": "Allow?", "buttonId": 42, "query": "cats ",
         "samePeer": false, "peerTypes": ["pm"], "quiz": true, "userId": 99, "copyText": "copied",
         "maxQuantity": 3, "requestWriteAccess": true}
        """.trimIndent(),
    )

    private companion object {
        val INLINE = """
            {"kind": "inline",
             "rows": [[{"kind": "url", "text": "Open docs", "url": "https://example.org/docs"}],
                      [{"kind": "callback", "text": "Vote yes", "data": "vote:yes",
                        "requiresPassword": false},
                       {"kind": "text", "text": "Vote no"}]],
             "fitSize": false, "singleUse": false, "selective": false, "persistent": false,
             "placeholder": null}
        """.trimIndent()

        val KEYBOARD = """
            {"kind": "keyboard",
             "rows": [[{"kind": "requestPoll", "text": "Quiz", "quiz": true}]],
             "fitSize": true, "singleUse": true, "selective": true, "persistent": true,
             "placeholder": "Pick one"}
        """.trimIndent()

        val FORCE_REPLY = """
            {"kind": "forceReply", "rows": [], "fitSize": false, "singleUse": true, "selective": true,
             "persistent": false, "placeholder": "Type here"}
        """.trimIndent()

        val HIDE = """
            {"kind": "hide", "rows": [], "fitSize": false, "singleUse": false, "selective": false,
             "persistent": false, "placeholder": null}
        """.trimIndent()

        val MARKUP_UNKNOWN = """
            {"kind": "someNewMarkup", "rows": [[{"kind": "text", "text": "Go"}]], "fitSize": true,
             "singleUse": true, "selective": false, "persistent": false, "placeholder": null}
        """.trimIndent()

        /** Every button kind the native projection names, and the variant it reaches. */
        val BUTTONS = listOf(
            "text" to Button.Text("T"),
            "url" to Button.Url("T", "https://example.org"),
            "webView" to Button.WebView("T", "https://example.org"),
            "callback" to Button.Callback("T", "payload", true),
            "switchInline" to Button.SwitchInline("T", "cats ", false, listOf("pm")),
            "requestPhone" to Button.RequestPhone("T"),
            "requestGeo" to Button.RequestGeo("T"),
            "requestPoll" to Button.RequestPoll("T", true),
            "game" to Button.Game("T"),
            "pay" to Button.Pay("T"),
            "urlAuth" to Button.UrlAuth("T", "https://example.org", "Allow?", 42),
            "inputUrlAuth" to Button.InputUrlAuth("T", "https://example.org", "Allow?", true),
            "inputUserProfile" to Button.InputUserProfile("T"),
            "userProfile" to Button.UserProfile("T", 99L),
            "simpleWebView" to Button.SimpleWebView("T", "https://example.org"),
            "requestPeer" to Button.RequestPeer("T", 42, 3),
            "inputRequestPeer" to Button.InputRequestPeer("T", 42, 3),
            "copy" to Button.Copy("T", "copied"),
        )

        /** The kinds the native projection names, so a variant cannot be added without its wire name. */
        val KINDS = listOf(
            "text", "url", "webView", "callback", "switchInline", "requestPhone", "requestGeo",
            "requestPoll", "game", "pay", "urlAuth", "inputUrlAuth", "inputUserProfile",
            "userProfile", "simpleWebView", "requestPeer", "inputRequestPeer", "copy",
        )
    }
}
