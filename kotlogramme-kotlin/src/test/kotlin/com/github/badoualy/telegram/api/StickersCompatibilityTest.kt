package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.AllStickers as BridgeAllStickers
import org.kotlogramme.protocol.FavedStickers as BridgeFavedStickers
import org.kotlogramme.protocol.RecentStickers as BridgeRecentStickers
import org.kotlogramme.protocol.StickerSetInstallResult as BridgeStickerSetInstallResult
import org.kotlogramme.protocol.StickerSetResult as BridgeStickerSetResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade carries the sticker projections the bridge gained.
 *
 * The documents are the same ones `StickersProtocolTest` pins on the wire side, so a field added to
 * the native projection without reaching this layer fails here.
 */
class StickersCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a sticker set projects its flags, thumbnail and packs`() {
        val result = json.decodeFromString<BridgeStickerSetResult>(STICKER_SET).toCompatibility()

        assertTrue(!result.notModified)
        val set = result.set ?: error("the result carries the set")
        assertEquals(1_234_567_890L, set.id)
        assertEquals(-9_876_543_210L, set.accessHash)
        assertEquals("Some Pack", set.title)
        assertEquals("somePack", set.shortName)
        assertEquals(12, set.count)
        assertEquals(99, set.hash)
        assertTrue(set.official && set.emojis && set.textColor && set.creator)
        assertEquals(1_700_000_000_000L, set.installedDate)
        assertEquals(42L, set.thumbDocumentId)
        assertEquals(2, set.thumbDcId)
        assertEquals(7, set.thumbVersion)
        assertEquals(listOf(StickerPack("🦄", listOf(42L, 43L))), set.packs)
        assertEquals(listOf(42L), set.documents)
    }

    @Test
    fun `a not-modified sticker set result projects no set`() {
        val result =
            json.decodeFromString<BridgeStickerSetResult>(
                """{"notModified": true, "set": null}""",
            ).toCompatibility()

        assertTrue(result.notModified)
        assertNull(result.set)
    }

    @Test
    fun `an all-stickers result projects its hash and sets`() {
        val result = json.decodeFromString<BridgeAllStickers>(ALL_STICKERS).toCompatibility()

        assertEquals(7L, result.hash)
        assertEquals(listOf("somePack"), result.sets.map { it.shortName })
    }

    @Test
    fun `a recent stickers result projects its packs, ids and dates`() {
        val result = json.decodeFromString<BridgeRecentStickers>(RECENT).toCompatibility()

        assertEquals(3L, result.hash)
        assertEquals(listOf(42L), result.stickers)
        assertEquals(listOf(1_700_000_000_000L), result.dates)
        assertEquals(listOf(StickerPack("🦄", listOf(42L, 43L))), result.packs)
    }

    @Test
    fun `a faved stickers result projects its packs and ids`() {
        val result = json.decodeFromString<BridgeFavedStickers>(FAVED).toCompatibility()

        assertEquals(5L, result.hash)
        assertEquals(listOf(7L), result.stickers)
        assertTrue(result.packs.isEmpty())
    }

    @Test
    fun `a plain install result projects no set at all`() {
        val install = json.decodeFromString<BridgeStickerSetInstallResult>(
            """{"installed": true, "archivedSets": []}""",
        ).toCompatibility()

        assertTrue(install.installed)
        assertTrue(install.archivedSets.isEmpty())
    }

    @Test
    fun `an archive result projects the sets it archived and their covers`() {
        val install = json.decodeFromString<BridgeStickerSetInstallResult>(ARCHIVED).toCompatibility()

        assertTrue(!install.installed)
        assertEquals(listOf("somePack"), install.archivedSets.map { it.set.shortName })
        assertTrue(install.archivedSets[0].set.archived)
        assertEquals(5150L, install.archivedSets[0].coverDocumentId)
    }

    private companion object {
        val ARCHIVED = """
            {"installed": false, "archivedSets": [
             {"set": {"archived": true, "official": false, "masks": false, "emojis": false,
              "textColor": false, "channelEmojiStatus": false, "creator": false,
              "installedDate": null, "id": 1, "accessHash": 2, "title": "Some Pack",
              "shortName": "somePack", "thumbDocumentId": null, "thumbDcId": null,
              "thumbVersion": null, "count": 0, "hash": 0, "packs": [], "documents": []},
              "coverDocumentId": 5150}]}
        """.trimIndent()
        val STICKER_SET = """
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

        val RECENT = """
            {"notModified": false, "hash": 3,
             "packs": [{"emoticon": "\uD83E\uDD84", "documents": [42, 43]}],
             "stickers": [42], "dates": [1700000000000]}
        """.trimIndent()

        val FAVED = """{"notModified": false, "hash": 5, "packs": [], "stickers": [7]}"""
    }
}
