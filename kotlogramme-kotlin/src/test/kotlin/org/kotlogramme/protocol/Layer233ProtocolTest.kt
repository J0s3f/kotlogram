package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the layer-233 facade described in `docs/layer-233-facade.md`: the forum
 * sidebar toggle, audio transcription, widgets, and the four updates the stream now types.
 *
 * The documents are exactly what the Rust handlers in `native/src/ops/{chats,files,widgets}.rs` read
 * and what `native/src/dto/update.rs` emits.
 */
class Layer233ProtocolTest {
    /** The wire codec, which drops a field whose value is its default. */
    private val wire = Json { ignoreUnknownKeys = true }

    // ---- forum sidebar ----------------------------------------------------------------------

    @Test
    fun `toggleForum encodes the sidebar flag next to enabled and tabs`() {
        assertEquals(
            """{"channel":{"peerHandle":5},"enabled":true,"tabs":true,"sidebar":true}""",
            wire.encodeToString(
                ToggleForumPayload(PeerTarget(5), enabled = true, tabs = true, sidebar = true),
            ),
        )
    }

    @Test
    fun `toggleForum defaults tabs and sidebar to off`() {
        val payload = ToggleForumPayload(PeerTarget(5), enabled = true)
        assertFalse(payload.tabs)
        assertFalse(payload.sidebar)
    }

    // ---- transcription ----------------------------------------------------------------------

    @Test
    fun `transcribeAudioFile carries only the upload handle`() {
        assertEquals("""{"uploadHandle":9}""", wire.encodeToString(TranscribeAudioFilePayload(9)))
    }

    @Test
    fun `a transcription decodes its pending state and the trial allowance`() {
        val done = wire.decodeFromString<AudioTranscription>(
            """{"transcriptionId":77,"text":"hello","pending":false}""",
        )
        assertEquals(77, done.transcriptionId)
        assertEquals("hello", done.text)
        assertFalse(done.pending)
        assertNull(done.trialRemaining)

        val trial = wire.decodeFromString<AudioTranscription>(
            """{"transcriptionId":78,"text":"he","pending":true,"trialRemaining":2,"trialRemainingUntilDate":1700000000000}""",
        )
        assertTrue(trial.pending)
        assertEquals(2, trial.trialRemaining)
        assertEquals(1_700_000_000_000, trial.trialRemainingUntilDate)
    }

    @Test
    fun `getTranscriptionLangs takes an optional peer`() {
        assertEquals("{}", wire.encodeToString(GetTranscriptionLangsPayload()))
        assertEquals(
            """{"peer":{"peerHandle":3}}""",
            wire.encodeToString(GetTranscriptionLangsPayload(PeerTarget(3))),
        )
    }

    // ---- widgets ----------------------------------------------------------------------------

    @Test
    fun `a sent widget carries its html, size and flags`() {
        val payload = SendWidgetPayload(
            peer = PeerTarget(4),
            widget = WidgetSpec(html = "<b>hi</b>", width = 320, height = 200, allowQueries = true, publicData = "{}"),
            caption = "look",
            silent = true,
        )
        assertEquals(
            """{"peer":{"peerHandle":4},"widget":{"html":"<b>hi</b>","width":320,"height":200,""" +
                """"allowQueries":true,"publicData":"{}"},"caption":"look","silent":true}""",
            wire.encodeToString(payload),
        )
    }

    @Test
    fun `an existing widget is sent by its id with per-chat data`() {
        val payload = SendWidgetPayload(peer = PeerTarget(4), widgetId = 99, chatData = "c")
        assertEquals(
            """{"peer":{"peerHandle":4},"widgetId":99,"chatData":"c"}""",
            wire.encodeToString(payload),
        )
    }

    @Test
    fun `a sent widget answers the message when the layer named it`() {
        val sent = wire.decodeFromString<SentWidget>("""{"ok":true}""")
        assertTrue(sent.ok)
        assertNull(sent.message)
    }

    @Test
    fun `public data reads as modified or not modified`() {
        val fresh = wire.decodeFromString<WidgetPublicData>(
            """{"notModified":false,"hash":5,"data":"{\"a\":1}","periodSeconds":60}""",
        )
        assertFalse(fresh.notModified)
        assertEquals(5, fresh.hash)
        assertEquals("""{"a":1}""", fresh.data)
        assertEquals(60, fresh.periodSeconds)

        val same = wire.decodeFromString<WidgetPublicData>("""{"notModified":true,"periodSeconds":30}""")
        assertTrue(same.notModified)
        assertNull(same.data)
        assertNull(same.hash)
    }

    @Test
    fun `public data is requested with the cached hash`() {
        assertEquals(
            """{"peer":{"peerHandle":4},"messageId":12,"hash":5}""",
            wire.encodeToString(GetWidgetPublicDataPayload(PeerTarget(4), 12, 5)),
        )
        // A first request sends no hash, which the layer reads as zero.
        assertEquals(0, GetWidgetPublicDataPayload(PeerTarget(4), 12).hash)
    }

    @Test
    fun `widget data edits leave an untouched field out`() {
        assertEquals(
            """{"peer":{"peerHandle":4},"messageId":12,"publicData":"p"}""",
            wire.encodeToString(EditMessageWidgetDataPayload(PeerTarget(4), 12, publicData = "p")),
        )
        assertEquals(
            """{"messageId":{"dcId":2,"accessHash":3,"id":4},"chatData":"c"}""",
            wire.encodeToString(
                EditInlineMessageWidgetDataPayload(InlineMessageIdPayload(2, 3, 4), chatData = "c"),
            ),
        )
        assertEquals(
            """{"widgetId":8,"publicData":"p","cacheTimeSeconds":60}""",
            wire.encodeToString(EditWidgetDataPayload(8, "p", 60)),
        )
    }

    @Test
    fun `a widget query answer is read and set by its query id`() {
        assertEquals(
            """{"peer":{"peerHandle":4},"messageId":12,"dataBase64":"AQID"}""",
            wire.encodeToString(GetBotWidgetQueryAnswerPayload(PeerTarget(4), 12, "AQID")),
        )
        assertEquals("r", wire.decodeFromString<BotWidgetQueryAnswer>("""{"result":"r"}""").result)
        assertNull(wire.decodeFromString<BotWidgetQueryAnswer>("{}").result)
        assertEquals(
            """{"queryId":6,"result":"done"}""",
            wire.encodeToString(SetBotWidgetQueryAnswerPayload(6, "done")),
        )
    }

    @Test
    fun `an inline answer can carry a widget result`() {
        val spec = InlineResultSpec(
            kind = "widget",
            id = "w1",
            title = "Widget",
            messageText = "caption",
            widget = WidgetSpec(html = "<i/>", width = 10, height = 20),
        )
        assertEquals(
            """{"kind":"widget","title":"Widget","messageText":"caption","id":"w1",""" +
                """"widget":{"html":"<i/>","width":10,"height":20}}""",
            wire.encodeToString(spec),
        )
    }

    // ---- updates ----------------------------------------------------------------------------

    @Test
    fun `a forum topic edit update decodes the changed fields only`() {
        val update = wire.decodeFromString<Update>(
            """{"kind":"forumTopicEdited","forumTopicEdit":{"peerId":5,"peerKind":"channel",""" +
                """"topicId":7,"title":"New","closed":true}}""",
        )
        assertEquals("forumTopicEdited", update.kind)
        val edit = assertNotNull(update.forumTopicEdit)
        assertEquals(7, edit.topicId)
        assertEquals("New", edit.title)
        assertEquals(true, edit.closed)
        assertNull(edit.hidden)
        assertNull(edit.iconEmojiId)
    }

    @Test
    fun `a bot widget query update names its message or its inline message`() {
        val inMessage = wire.decodeFromString<Update>(
            """{"kind":"botWidgetQuery","widgetQuery":{"queryId":1,"userId":2,"queryType":"data",""" +
                """"peerId":5,"peerKind":"user","messageId":9,"chatInstance":3,""" +
                """"chatType":"sender","dataBase64":"AQID","widgetId":4}}""",
        )
        val query = assertNotNull(inMessage.widgetQuery)
        assertEquals(5, query.peerId)
        assertEquals("user", query.peerKind)
        assertEquals("data", query.queryType)
        assertEquals(9, query.messageId)
        assertNull(query.inlineMessageId)
        assertEquals("AQID", query.dataBase64)

        val inline = wire.decodeFromString<Update>(
            """{"kind":"inlineBotWidgetQuery","widgetQuery":{"queryId":1,"userId":2,"queryType":"signData",""" +
                """"inlineMessageId":{"dcId":2,"accessHash":3,"id":4},"chatInstance":3,"chatType":"sender",""" +
                """"dataBase64":""}}""",
        )
        assertEquals(4, assertNotNull(inline.widgetQuery?.inlineMessageId).id)
    }

    @Test
    fun `a message widget data update and a transcription update decode`() {
        val data = wire.decodeFromString<Update>(
            """{"kind":"messageWidgetData","widgetData":{"peerId":5,"peerKind":"user",""" +
                """"messageId":9,"publicData":"p","publicDataHash":11}}""",
        )
        assertEquals(11, assertNotNull(data.widgetData).publicDataHash)
        assertNull(data.widgetData?.chatData)

        val transcription = wire.decodeFromString<Update>(
            """{"kind":"audioTranscribed","transcription":{"transcriptionId":5,"text":"t","pending":true}}""",
        )
        assertEquals(true, assertNotNull(transcription.transcription).pending)
    }
}
