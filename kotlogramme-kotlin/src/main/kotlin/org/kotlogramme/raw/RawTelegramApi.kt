package org.kotlogramme.raw

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.kotlogramme.TelegramClient

/**
 * Versioned description of the raw Telegram TL schema bundled with this library.
 *
 * It deliberately exposes schema metadata only. Invoking an arbitrary TL method requires
 * generated Kotlin codecs and a matching Rust dispatcher; accepting unvalidated JSON here would
 * not be a safe or wire-compatible raw API.
 */
object RawTelegramApi {
    const val FORMAT: String = "kotlogram-raw-schema/v1"
    const val LAYER: Int = 216

    fun schema(): RawSchema = schema

    fun method(name: String): RawMethod? = schema.functions.firstOrNull { it.name == name }

    /**
     * Encodes a raw request for a schema method that has no parameters.
     *
     * This is deliberately narrow: callers with parameters should use [TlWriter] and the
     * schema declaration until the Layer-216 request-code generator emits typed request classes.
     */
    fun encodeNoArgumentRequest(methodName: String): ByteArray {
        val method = requireNotNull(method(methodName)) { "Unknown Layer-$LAYER method: $methodName" }
        require(method.parameters.isEmpty()) {
            "$methodName has parameters and must be encoded with a Layer-$LAYER codec"
        }
        return TlWriter().constructor(method.constructorId).toByteArray()
    }

    /**
     * Invokes an already TL-encoded Layer-216 request through grammers' sender pool.
     *
     * The schema manifest is the version contract for generated codecs. This method does not
     * accept JSON because that would not preserve Telegram TL's binary constructors or flags.
     */
    fun invoke(client: TelegramClient, request: RawRequest): ByteArray =
        client.invokeRaw(request.body, request.dataCenterId)

    private val schema: RawSchema by lazy {
        val resource = requireNotNull(RawTelegramApi::class.java.getResourceAsStream("/raw/telegram-layer-216.json")) {
            "Bundled Layer-216 raw schema manifest is missing"
        }
        resource.bufferedReader().use { json.decodeFromString(it.readText()) }
    }

    private val json = Json { ignoreUnknownKeys = true }
}

@Serializable
data class RawSchema(
    val format: String,
    val layer: Int,
    val schemaSha256: String,
    val functions: List<RawMethod>,
)

@Serializable
data class RawMethod(
    val name: String,
    val constructorId: Int,
    val parameters: String,
    val result: String,
    val declaration: String,
)

data class RawRequest(
    val body: ByteArray,
    val dataCenterId: Int? = null,
)
