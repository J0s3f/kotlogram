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
            .string("raw")
            .bytes(ByteArray(254) { it.toByte() })
            .toByteArray()
        val reader = TlReader(encoded)

        assertEquals(0x78563412, reader.int())
        assertEquals(0x0807060504030201, reader.long())
        assertEquals("raw", reader.string())
        assertTrue(reader.bytes().contentEquals(ByteArray(254) { it.toByte() }))
        reader.requireFullyRead()
    }
}
