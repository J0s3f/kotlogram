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
        assertNotNull(RawTelegramApi.method("messages.sendMessage"))
        assertNotNull(RawTelegramApi.method("auth.sendCode"))
    }

    @Test
    fun `raw requests retain their binary payload and optional data center`() {
        val request = RawRequest(byteArrayOf(1, 2, 3, 4), dataCenterId = 2)

        assertEquals(2, request.dataCenterId)
        assertTrue(request.body.contentEquals(byteArrayOf(1, 2, 3, 4)))
    }
}
