package org.kotlogramme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Tests for the shared transport's result decoding.
 *
 * [Transport.decode] is the one place every bridge operation turns a native answer into a value, so
 * the distinction it draws between "the operation answered nothing" and "the operation failed" is
 * what makes a nullable result usable. The native side answers an error as a plain
 * `kotlogramme error: ...` string, never as a document, which is what the checks below pin.
 */
class TransportTest {
    /** A transport with no native handle, which is enough for [Transport.decode]. */
    private val transport = Transport(handle = 0)

    @Test
    fun `a bare null decodes to null rather than an error`() {
        assertNull(transport.decode<List<String>?>("null"))
        assertNull(transport.decode<List<String>?>("  null  "))
    }

    @Test
    fun `an object and an array still decode`() {
        assertEquals(emptyList(), transport.decode<List<String>>("[]"))
        assertEquals(mapOf("ok" to true), transport.decode<Map<String, Boolean>>("""{"ok": true}"""))
    }

    @Test
    fun `a native error string is raised as a TelegramException`() {
        val failure = assertFailsWith<TelegramException> {
            transport.decode<List<String>?>("kotlogramme error: chat not found")
        }
        assertEquals("kotlogramme error: chat not found", failure.message)
    }
}
