package org.kotlogramme.raw

import com.github.badoualy.telegram.api.Kotlogram
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RawTelegramApiTest {
    @Test
    fun `bundled schema matches the advertised layer and contains core requests`() {
        val schema = RawTelegramApi.schema()

        assertEquals(RawTelegramApi.FORMAT, schema.format)
        assertEquals(Kotlogram.API_LAYER, schema.layer)
        assertEquals(RawTelegramApi.LAYER, schema.layer)
        assertTrue(schema.functions.size > 500)
        assertTrue(schema.constructors.size > 1_000)
        assertTrue(assertNotNull(RawTelegramApi.constructor("inputPeerUser")).constructorId != 0)
        assertTrue(assertNotNull(RawTelegramApi.method("messages.sendMessage")).constructorId != 0)
        assertTrue(assertNotNull(RawTelegramApi.method("auth.sendCode")).constructorId != 0)
    }

    @Test
    fun `raw requests retain their binary payload and optional data center`() {
        val request = RawRequest(byteArrayOf(1, 2, 3, 4), dataCenterId = 2)

        assertEquals(2, request.dataCenterId)
        assertTrue(request.body.contentEquals(byteArrayOf(1, 2, 3, 4)))
    }

    @Test
    fun `no argument requests use the constructor declared by the bundled schema`() {
        val method = assertNotNull(RawTelegramApi.method("help.getNearestDc"))
        val reader = TlReader(RawTelegramApi.encodeNoArgumentRequest(method.name))

        reader.constructor(method.constructorId)
        reader.requireFullyRead()
    }

    @Test
    fun `TL primitive codec preserves little endian values and byte-array framing`() {
        val encoded = TlWriter()
            .int(0x78563412)
            .long(0x0807060504030201)
            .boolean(true)
            .boolean(false)
            .string("raw")
            .vector(listOf(1, 2, 3)) { int(it) }
            .bytes(ByteArray(254) { it.toByte() })
            .toByteArray()
        val reader = TlReader(encoded)

        assertEquals(0x78563412, reader.int())
        assertEquals(0x0807060504030201, reader.long())
        assertTrue(reader.boolean())
        assertTrue(!reader.boolean())
        assertEquals("raw", reader.string())
        assertEquals(listOf(1, 2, 3), reader.vector { int() })
        assertTrue(reader.bytes().contentEquals(ByteArray(254) { it.toByte() }))
        reader.requireFullyRead()
    }

    @Test
    fun `dynamic raw codec derives flags and decodes schema declared responses`() {
        val request = RawTelegramApi.encodeRequest(
            "auth.sendCode",
            mapOf(
                "phone_number" to RawValue.StringValue("+12025550123"),
                "api_id" to RawValue.IntValue(1),
                "api_hash" to RawValue.StringValue("test-hash"),
                "settings" to RawValue.Object(
                    "codeSettings",
                    mapOf("allow_flashcall" to RawValue.BooleanValue(true)),
                ),
            ),
        )
        val requestReader = TlReader(request)
        requestReader.constructor(assertNotNull(RawTelegramApi.method("auth.sendCode")).constructorId)
        assertEquals("+12025550123", requestReader.string())
        assertEquals(1, requestReader.int())
        assertEquals("test-hash", requestReader.string())
        requestReader.constructor(assertNotNull(RawTelegramApi.constructor("codeSettings")).constructorId)
        assertEquals(1, requestReader.int())
        requestReader.requireFullyRead()

        val response = TlWriter()
            .constructor(assertNotNull(RawTelegramApi.constructor("nearestDc")).constructorId)
            .string("AT")
            .int(2)
            .int(1)
            .toByteArray()
        assertEquals(
            RawValue.Object(
                "nearestDc",
                mapOf(
                    "country" to RawValue.StringValue("AT"),
                    "this_dc" to RawValue.IntValue(2),
                    "nearest_dc" to RawValue.IntValue(1),
                ),
            ),
            RawTelegramApi.decodeResponse("help.getNearestDc", response),
        )
    }
}
