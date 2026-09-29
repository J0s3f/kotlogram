package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.Me as BridgeMe
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that the compatibility facade projects the account identity the bridge returns, and that
 * the data-centre identifier it carries is the one Kotlogram already names.
 */
class AuthCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `the account identity carries the account and its data centre`() {
        val identity = json.decodeFromString<BridgeMe>(ME).toCompatibility()

        assertEquals(7L, identity.user.id)
        assertEquals("Some One", identity.user.fullName)
        assertEquals(2, identity.dataCentreId)
    }

    @Test
    fun `the data centre identifier names the same data centre Kotlogram maps`() {
        val identity = json.decodeFromString<BridgeMe>(ME).toCompatibility()

        assertEquals(Kotlogram.PROD_DC2, Kotlogram.getDcById(identity.dataCentreId))
        assertEquals(2, Kotlogram.getDcId(Kotlogram.PROD_DC2))
    }

    private companion object {
        val ME = """{"user": {"id": 7, "fullName": "Some One"}, "dataCentreId": 2}"""
    }
}
