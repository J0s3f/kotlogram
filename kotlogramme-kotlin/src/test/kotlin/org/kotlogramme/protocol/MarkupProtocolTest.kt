package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the reply-markup models and payloads.
 *
 * Each document below is exactly what the Rust side emits or expects in `native/src/ops/markup.rs`
 * and `native/src/dto/markup.rs`, so a field renamed on one side, a button kind dropped or a
 * payload key spelled differently breaks a test here rather than a live session. The Rust side pins
 * the same documents in its own test modules.
 */
class MarkupProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** The transport's own codec, which leaves a field at its default out of a request payload. */
    private val requests = Json { ignoreUnknownKeys = true }

    @Test
    fun `an inline markup decodes and re-encodes unchanged`() {
        val markup = roundTrips<ReplyMarkup>(INLINE)

        assertEquals("inline", markup.kind)
        assertEquals(2, markup.rows.size)
        assertEquals("url", markup.rows[0][0].kind)
        assertEquals("Open docs", markup.rows[0][0].text)
        assertEquals("https://example.org/docs", markup.rows[0][0].url)
        assertEquals("callback", markup.rows[1][0].kind)
        assertEquals("vote:yes", markup.rows[1][0].data)
        assertEquals(false, markup.rows[1][0].requiresPassword)
        // An inline markup has no keyboard option, so all four are reported at their defaults.
        assertTrue(!markup.fitSize && !markup.singleUse && !markup.selective && !markup.persistent)
        assertNull(markup.placeholder)
    }

    @Test
    fun `a reply keyboard decodes and re-encodes unchanged`() {
        val markup = roundTrips<ReplyMarkup>(KEYBOARD)

        assertEquals("keyboard", markup.kind)
        assertTrue(markup.fitSize && markup.singleUse && markup.selective && markup.persistent)
        assertEquals("Pick one", markup.placeholder)
        assertEquals("requestPoll", markup.rows[0][0].kind)
        assertEquals("Quiz", markup.rows[0][0].text)
        assertEquals(true, markup.rows[0][0].quiz)
    }

    @Test
    fun `a force reply and a hide carry no rows`() {
        val force = roundTrips<ReplyMarkup>(FORCE_REPLY)
        assertEquals("forceReply", force.kind)
        assertTrue(force.singleUse && force.selective)
        assertEquals(emptyList(), force.rows)
        assertEquals("Type here", force.placeholder)

        val hide = roundTrips<ReplyMarkup>(HIDE)
        assertEquals("hide", hide.kind)
        assertTrue(!hide.selective)
        assertEquals(emptyList(), hide.rows)
    }

    @Test
    fun `a markup declares exactly the fields the native projection emits`() {
        assertEquals(MARKUP_FIELDS.sorted(), fieldNames(INLINE))
        assertEquals(MARKUP_FIELDS.sorted(), fieldNames(HIDE))
    }

    @Test
    fun `a button declares exactly the fields the native projection emits`() {
        assertEquals(BUTTON_FIELDS.sorted(), fieldNames(BUTTON_URL))
        assertEquals(BUTTON_FIELDS.sorted(), fieldNames(BUTTON_UNKNOWN))
    }

    @Test
    fun `a button decodes the fields its own kind populates`() {
        val button = roundTrips<Button>(BUTTON_SWITCH_INLINE)

        assertEquals("switchInline", button.kind)
        assertEquals("Search here", button.text)
        assertEquals("cats ", button.query)
        assertEquals(false, button.samePeer)
        assertEquals(listOf("sameBotPm", "megagroup", "botPm"), button.peerTypes)
        // The other kinds are the ones a switch-inline button cannot answer.
        assertNull(button.url)
        assertNull(button.data)
        assertNull(button.quiz)
        assertNull(button.userId)
    }

    @Test
    fun `a button the native side cannot name still decodes whole`() {
        val button = roundTrips<Button>(BUTTON_UNKNOWN)

        assertEquals("someNewButton", button.kind)
        assertEquals("Future", button.text)
        assertEquals("https://example.org/new", button.url)
        assertEquals("new-payload", button.data)
        assertEquals(7, button.buttonId)
        assertEquals(99L, button.userId)
    }

    @Test
    fun `a poll button of unspecified kind keeps its null flag`() {
        // grammers' `request_poll` leaves the layer's `quiz` flag unset rather than clearing it.
        assertNull(json.decodeFromString<Button>(BUTTON_POLL).quiz)
        assertEquals(true, json.decodeFromString<Button>(BUTTON_QUIZ).quiz)
    }

    @Test
    fun `the minimal documents an older bridge emits still decode`() {
        val markup = json.decodeFromString<ReplyMarkup>("""{"kind": "hide"}""")
        assertEquals("hide", markup.kind)
        assertEquals(emptyList(), markup.rows)
        assertTrue(!markup.selective)
        assertNull(markup.placeholder)

        val button = json.decodeFromString<Button>("""{"kind": "text", "text": "Go"}""")
        assertEquals("text", button.kind)
        assertEquals("Go", button.text)
        assertNull(button.url)
        assertNull(button.data)
    }

    @Test
    fun `the get-reply-markup payload names the message the way the native side reads it`() {
        val payload = MessageMarkupPayload(PeerTarget(peerHandle = 12L), messageId = 31)

        assertEquals(
            """{"peerHandle":12,"messageId":31}""",
            requests.encodeToString(payload),
        )
        assertEquals(
            """{"username":"channel","messageId":31}""",
            requests.encodeToString(MessageMarkupPayload(PeerTarget(username = "channel"), 31)),
        )
    }

    @Test
    fun `an inline request payload encodes every button grammers can build there`() {
        val payload = InlineMarkupPayload(
            listOf(
                listOf(
                    InlineButtonSpec.Url("Docs", "https://example.org"),
                    InlineButtonSpec.WebView("Play", "https://example.org/game"),
                ),
                listOf(
                    InlineButtonSpec.Callback("Vote", "vote:yes"),
                    InlineButtonSpec.SwitchInline("Search", "cats ", samePeer = false),
                ),
            ),
        )

        assertEquals(
            """{"rows":[[{"type":"url","text":"Docs","url":"https://example.org"},""" +
                """{"type":"webView","text":"Play","url":"https://example.org/game"}],""" +
                """[{"type":"callback","text":"Vote","data":"vote:yes"},""" +
                """{"type":"switchInline","text":"Search","query":"cats ","samePeer":false}]]}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `an absent request field is left out for the native default to fill`() {
        // The transport leaves a field at its default out, and every such field is `#[serde(default)]`
        // on the native side, so `switchInline` without `samePeer` is grammers' `switch_inline` and
        // `requestPoll` without `quiz` is grammers' `request_poll`.
        assertEquals(
            """{"rows":[[{"type":"switchInline","text":"S","query":""}]]}""",
            requests.encodeToString(
                InlineMarkupPayload(listOf(listOf(InlineButtonSpec.SwitchInline("S", "")))),
            ),
        )
        assertEquals(
            """{"rows":[[{"type":"requestPoll","text":"Poll"}]]}""",
            requests.encodeToString(
                KeyboardMarkupPayload(listOf(listOf(KeyboardButtonSpec.RequestPoll("Poll")))),
            ),
        )
        assertEquals("{}", requests.encodeToString(ForceReplyPayload()))
        assertEquals("{}", requests.encodeToString(HideKeyboardPayload()))
    }

    @Test
    fun `a keyboard request payload carries the options the native builders take`() {
        val payload = KeyboardMarkupPayload(
            rows = listOf(
                listOf(
                    KeyboardButtonSpec.Text("Accept"),
                    KeyboardButtonSpec.RequestPhone("Number"),
                    KeyboardButtonSpec.RequestGeo("Location"),
                    KeyboardButtonSpec.RequestPoll("Quiz", quiz = true),
                ),
            ),
            fitSize = true,
            singleUse = true,
            selective = true,
        )

        assertEquals(
            """{"rows":[[{"type":"text","text":"Accept"},""" +
                """{"type":"requestPhone","text":"Number"},""" +
                """{"type":"requestGeo","text":"Location"},""" +
                """{"type":"requestPoll","text":"Quiz","quiz":true}]],""" +
                """"fitSize":true,"singleUse":true,"selective":true}""",
            requests.encodeToString(payload),
        )
    }

    @Test
    fun `a force reply and a hide request payload carry only the options they accept`() {
        assertEquals(
            """{"singleUse":true,"selective":true}""",
            requests.encodeToString(ForceReplyPayload(singleUse = true, selective = true)),
        )
        assertEquals("""{"selective":true}""", requests.encodeToString(HideKeyboardPayload(selective = true)))
    }

    @Test
    fun `a markup spec encodes every kind the native side builds`() {
        val inline: MarkupSpec = MarkupSpec.Inline(
            listOf(
                listOf(
                    InlineButtonSpec.Url("Docs", "https://example.org"),
                    InlineButtonSpec.Callback("Vote", "vote:yes"),
                ),
            ),
        )
        assertEquals(
            """{"kind":"inline","rows":[[{"type":"url","text":"Docs","url":"https://example.org"},""" +
                """{"type":"callback","text":"Vote","data":"vote:yes"}]]}""",
            requests.encodeToString(inline),
        )

        val keyboard: MarkupSpec = MarkupSpec.Keyboard(
            rows = listOf(listOf(KeyboardButtonSpec.Text("Go"))),
            fitSize = true,
            singleUse = true,
            selective = true,
        )
        assertEquals(
            """{"kind":"keyboard","rows":[[{"type":"text","text":"Go"}]],""" +
                """"fitSize":true,"singleUse":true,"selective":true}""",
            requests.encodeToString(keyboard),
        )

        val forceReply: MarkupSpec = MarkupSpec.ForceReply(singleUse = true, selective = true)
        assertEquals(
            """{"kind":"forceReply","singleUse":true,"selective":true}""",
            requests.encodeToString(forceReply),
        )
        val hide: MarkupSpec = MarkupSpec.Hide(selective = true)
        assertEquals(
            """{"kind":"hide","selective":true}""",
            requests.encodeToString(hide),
        )
    }

    @Test
    fun `a markup spec leaves a default option out for the native default to fill`() {
        // The transport leaves a field at its default out, and every such field is `#[serde(default)]`
        // on the native side, so a hide with no options and a force reply with none are bare kinds.
        val hide: MarkupSpec = MarkupSpec.Hide()
        val forceReply: MarkupSpec = MarkupSpec.ForceReply()
        assertEquals("""{"kind":"hide"}""", requests.encodeToString(hide))
        assertEquals("""{"kind":"forceReply"}""", requests.encodeToString(forceReply))
    }

    @Test
    fun `a projected markup converts to the spec a send carries`() {
        val keyboard = assertIs<MarkupSpec.Keyboard>(json.decodeFromString<ReplyMarkup>(KEYBOARD).asSpec())
        assertTrue(keyboard.fitSize && keyboard.singleUse && keyboard.selective)
        assertEquals(KeyboardButtonSpec.RequestPoll("Quiz", quiz = true), keyboard.rows[0][0])

        val inline = assertIs<MarkupSpec.Inline>(json.decodeFromString<ReplyMarkup>(INLINE).asSpec())
        assertEquals(InlineButtonSpec.Url("Open docs", "https://example.org/docs"), inline.rows[0][0])
        assertEquals(InlineButtonSpec.Callback("Vote yes", "vote:yes"), inline.rows[1][0])

        val force = assertIs<MarkupSpec.ForceReply>(
            json.decodeFromString<ReplyMarkup>(FORCE_REPLY).asSpec(),
        )
        assertTrue(force.singleUse && force.selective)

        val hide = assertIs<MarkupSpec.Hide>(json.decodeFromString<ReplyMarkup>(HIDE).asSpec())
        assertTrue(!hide.selective)
    }

    @Test
    fun `a projected markup of an unknown kind cannot become a spec`() {
        val markup = json.decodeFromString<ReplyMarkup>(MARKUP_UNKNOWN)
        assertFailsWith<IllegalArgumentException> { markup.asSpec() }
    }

    /** Decodes a native document and asserts that re-encoding it reproduces the same document. */
    private inline fun <reified T> roundTrips(document: String): T {
        val decoded = json.decodeFromString<T>(document)
        assertEquals(
            json.parseToJsonElement(document),
            json.parseToJsonElement(json.encodeToString(decoded)),
            "the projection changed the document on the way back",
        )
        return decoded
    }

    private fun fieldNames(document: String): List<String> =
        (json.parseToJsonElement(document) as JsonObject).keys.sorted()

    private companion object {
        val BUTTON_URL = """
            {"kind": "url", "text": "Open docs", "url": "https://example.org/docs", "data": null,
             "requiresPassword": null, "fwdText": null, "buttonId": null, "query": null,
             "samePeer": null, "peerTypes": null, "quiz": null, "userId": null, "copyText": null,
             "maxQuantity": null, "requestWriteAccess": null}
        """.trimIndent()

        val BUTTON_SWITCH_INLINE = """
            {"kind": "switchInline", "text": "Search here", "url": null, "data": null,
             "requiresPassword": null, "fwdText": null, "buttonId": null, "query": "cats ",
             "samePeer": false, "peerTypes": ["sameBotPm", "megagroup", "botPm"], "quiz": null,
             "userId": null, "copyText": null, "maxQuantity": null, "requestWriteAccess": null}
        """.trimIndent()

        val BUTTON_POLL = """
            {"kind": "requestPoll", "text": "Poll", "url": null, "data": null,
             "requiresPassword": null, "fwdText": null, "buttonId": null, "query": null,
             "samePeer": null, "peerTypes": null, "quiz": null, "userId": null, "copyText": null,
             "maxQuantity": null, "requestWriteAccess": null}
        """.trimIndent()

        val BUTTON_QUIZ = BUTTON_POLL.replace(""""quiz": null""", """"quiz": true""")

        val BUTTON_UNKNOWN = """
            {"kind": "someNewButton", "text": "Future", "url": "https://example.org/new",
             "data": "new-payload", "requiresPassword": true, "fwdText": "Allow?",
             "buttonId": 7, "query": "cats ", "samePeer": true, "peerTypes": ["pm"], "quiz": true,
             "userId": 99, "copyText": "copied", "maxQuantity": 3, "requestWriteAccess": true}
        """.trimIndent()

        val INLINE = """
            {"kind": "inline",
             "rows": [[${BUTTON_URL.trimIndent()}],
                      [{"kind": "callback", "text": "Vote yes", "url": null, "data": "vote:yes",
                        "requiresPassword": false, "fwdText": null, "buttonId": null, "query": null,
                        "samePeer": null, "peerTypes": null, "quiz": null, "userId": null,
                        "copyText": null, "maxQuantity": null, "requestWriteAccess": null}]],
             "fitSize": false, "singleUse": false, "selective": false, "persistent": false,
             "placeholder": null}
        """.trimIndent()

        val KEYBOARD = """
            {"kind": "keyboard",
             "rows": [[{"kind": "requestPoll", "text": "Quiz", "url": null, "data": null,
                        "requiresPassword": null, "fwdText": null, "buttonId": null, "query": null,
                        "samePeer": null, "peerTypes": null, "quiz": true, "userId": null,
                        "copyText": null, "maxQuantity": null, "requestWriteAccess": null}]],
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
            {"kind": "someNewMarkup", "rows": [], "fitSize": false, "singleUse": false,
             "selective": false, "persistent": false, "placeholder": null}
        """.trimIndent()

        /** The names a markup document carries, kept in step with the native projection. */
        val MARKUP_FIELDS = listOf(
            "fitSize", "kind", "persistent", "placeholder", "rows", "selective", "singleUse",
        )

        /** The names a button document carries, kept in step with the native projection. */
        val BUTTON_FIELDS = listOf(
            "buttonId", "copyText", "data", "fwdText", "kind", "maxQuantity", "peerTypes", "query",
            "quiz", "requestWriteAccess", "requiresPassword", "samePeer", "text", "url", "userId",
        )
    }
}
