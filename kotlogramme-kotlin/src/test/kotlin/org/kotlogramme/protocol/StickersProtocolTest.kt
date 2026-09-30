package org.kotlogramme.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-contract tests for the sticker payloads and result models.
 *
 * The documents below are exactly what the projections in `native/src/dto/stickers.rs` emit and
 * what its handlers consume, which the Rust tests pin on their side, so a field renamed on one
 * side fails here instead of against a live session.
 */
class StickersProtocolTest {
    /** Re-encodes with defaults, so a field that is present but holds its default survives. */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Encodes a payload the way `Transport` does, dropping fields left at their default. */
    private val wire = Json { ignoreUnknownKeys = true }

    @Test
    fun `a sticker set payload carries the short name it is given`() {
        assertEquals(
            wire.parseToJsonElement("""{"shortName": "somePack"}"""),
            wire.parseToJsonElement(wire.encodeToString(GetStickerSetPayload(shortName = "somePack"))),
        )
    }

    @Test
    fun `a sticker set payload carries the id pair when no short name is given`() {
        val document = wire.encodeToString(GetStickerSetPayload(id = 1, accessHash = 2, hash = 7))
        assertEquals(
            wire.parseToJsonElement("""{"id": 1, "accessHash": 2, "hash": 7}"""),
            wire.parseToJsonElement(document),
        )
    }

    @Test
    fun `a bare sticker set payload encodes as an empty object`() {
        assertEquals("{}", wire.encodeToString(GetStickerSetPayload()))
    }

    @Test
    fun `a recent stickers payload carries the attached flag`() {
        assertEquals(
            """{"attached":true}""",
            wire.encodeToString(GetRecentStickersPayload(attached = true)),
        )
        assertEquals("{}", wire.encodeToString(GetRecentStickersPayload()))
    }

    @Test
    fun `a get stickers payload defaults its hash away`() {
        assertEquals("{}", wire.encodeToString(GetStickersPayload()))
        assertEquals("""{"hash":42}""", wire.encodeToString(GetStickersPayload(hash = 42)))
    }

    @Test
    fun `a full sticker set carries its flags, thumbnail and packs`() {
        val result = roundTrips<StickerSetResult>(STICKER_SET_RESULT)

        assertTrue(!result.notModified)
        val set = result.set ?: error("the result carries the set")
        assertEquals(1_234_567_890L, set.id)
        assertEquals(-9_876_543_210L, set.accessHash)
        assertEquals("Some Pack", set.title)
        assertEquals("somePack", set.shortName)
        assertEquals(12, set.count)
        assertEquals(99, set.hash)
        assertTrue(set.official && set.emojis && set.textColor && set.creator)
        assertTrue(!set.archived && !set.masks && !set.channelEmojiStatus)
        assertEquals(1_700_000_000_000L, set.installedDate)
        assertEquals(42L, set.thumbDocumentId)
        assertEquals(2, set.thumbDcId)
        assertEquals(7, set.thumbVersion)
        assertEquals(listOf("🦄"), set.packs.map { it.emoticon })
        assertEquals(listOf(42L, 43L), set.packs[0].documents)
        assertEquals(listOf(42L), set.documents)
    }

    @Test
    fun `a not-modified sticker set result carries no set`() {
        val result = roundTrips<StickerSetResult>("""{"notModified": true, "set": null}""")
        assertTrue(result.notModified)
        assertNull(result.set)
    }

    @Test
    fun `an all-stickers result carries the hash and the sets`() {
        val result = roundTrips<AllStickers>(ALL_STICKERS)

        assertEquals(7L, result.hash)
        assertTrue(!result.notModified)
        assertEquals(listOf("somePack"), result.sets.map { it.shortName })
        assertTrue(result.sets[0].packs.isEmpty())
    }

    @Test
    fun `a not-modified all-stickers result is an empty page`() {
        val result =
            roundTrips<AllStickers>("""{"notModified": true, "hash": 0, "sets": []}""")
        assertTrue(result.notModified)
        assertTrue(result.sets.isEmpty())
    }

    @Test
    fun `a recent stickers result carries the packs, ids and dates`() {
        val result = roundTrips<RecentStickers>(RECENT_STICKERS)

        assertEquals(3L, result.hash)
        assertEquals(listOf(42L), result.stickers)
        assertEquals(listOf(1_700_000_000_000L), result.dates)
        assertEquals(listOf("🦄"), result.packs.map { it.emoticon })
    }

    @Test
    fun `a not-modified recent stickers result is an empty page`() {
        val result = roundTrips<RecentStickers>(
            """{"notModified": true, "hash": 0, "packs": [], "stickers": [], "dates": []}""",
        )
        assertTrue(result.notModified && result.stickers.isEmpty() && result.dates.isEmpty())
    }

    @Test
    fun `a faved stickers result carries the packs and the ids`() {
        val result = roundTrips<FavedStickers>(
            """{"notModified": false, "hash": 5, "packs": [], "stickers": [7]}""",
        )
        assertEquals(5L, result.hash)
        assertEquals(listOf(7L), result.stickers)
        assertTrue(result.packs.isEmpty())
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

    private companion object {
        val STICKER_SET_RESULT = """
            {"notModified": false, "set": {"archived": false, "official": true, "masks": false,
             "emojis": true, "textColor": true, "channelEmojiStatus": false, "creator": true,
             "installedDate": 1700000000000, "id": 1234567890, "accessHash": -9876543210,
             "title": "Some Pack", "shortName": "somePack", "thumbDocumentId": 42, "thumbDcId": 2,
             "thumbVersion": 7, "count": 12, "hash": 99,
             "packs": [{"emoticon": "\uD83E\uDD84", "documents": [42, 43]}], "documents": [42]}}
        """.trimIndent()

        val ALL_STICKERS = """
            {"notModified": false, "hash": 7, "sets": [{"archived": false, "official": false,
             "masks": false, "emojis": false, "textColor": false, "channelEmojiStatus": false,
             "creator": false, "installedDate": null, "id": 1, "accessHash": 2, "title": "Some Pack",
             "shortName": "somePack", "thumbDocumentId": null, "thumbDcId": null,
             "thumbVersion": null, "count": 0, "hash": 0, "packs": [], "documents": []}]}
        """.trimIndent()

        val RECENT_STICKERS = """
            {"notModified": false, "hash": 3,
             "packs": [{"emoticon": "\uD83E\uDD84", "documents": [42, 43]}],
             "stickers": [42], "dates": [1700000000000]}
        """.trimIndent()
    }
}
