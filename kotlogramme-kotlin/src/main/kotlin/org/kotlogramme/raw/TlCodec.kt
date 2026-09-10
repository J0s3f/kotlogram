package org.kotlogramme.raw

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Minimal Layer-independent Telegram TL primitive codec.
 *
 * Telegram encodes signed numbers in little-endian order and strings as TL byte arrays. This
 * class is the stable building block for the generated, Layer-specific request codecs; it does
 * not attempt to infer flags or nested constructors from untyped values.
 */
class TlWriter {
    private val output = ByteArrayOutputStream()

    fun boolean(value: Boolean): TlWriter = constructor(if (value) BOOL_TRUE else BOOL_FALSE)

    fun constructor(id: Int): TlWriter = int(id)

    fun int(value: Int): TlWriter = apply {
        output.write(value and 0xff)
        output.write((value ushr 8) and 0xff)
        output.write((value ushr 16) and 0xff)
        output.write((value ushr 24) and 0xff)
    }

    fun long(value: Long): TlWriter = apply {
        repeat(Long.SIZE_BYTES) { byte -> output.write(((value ushr (byte * 8)) and 0xff).toInt()) }
    }

    fun double(value: Double): TlWriter = long(value.toRawBits())

    fun string(value: String): TlWriter = bytes(value.toByteArray(StandardCharsets.UTF_8))

    /** Writes a fixed-width TL primitive such as int128 or int256 without a byte-array prefix. */
    fun fixedBytes(value: ByteArray): TlWriter = apply { output.write(value) }

    fun <T> vector(values: Iterable<T>, writeElement: TlWriter.(T) -> Unit): TlWriter = apply {
        val entries = values.toList()
        constructor(VECTOR_CONSTRUCTOR)
        int(entries.size)
        entries.forEach { writeElement(it) }
    }

    fun bytes(value: ByteArray): TlWriter = apply {
        require(value.size <= 0x00ff_ffff) { "TL byte arrays cannot exceed 16,777,215 bytes" }
        val headerSize = if (value.size < 254) {
            output.write(value.size)
            1
        } else {
            output.write(254)
            output.write(value.size and 0xff)
            output.write((value.size ushr 8) and 0xff)
            output.write((value.size ushr 16) and 0xff)
            4
        }
        output.write(value)
        repeat(padding(headerSize + value.size)) { output.write(0) }
    }

    fun toByteArray(): ByteArray = output.toByteArray()

    private fun padding(size: Int): Int = (Int.SIZE_BYTES - size % Int.SIZE_BYTES) % Int.SIZE_BYTES

    companion object {
        const val BOOL_FALSE: Int = -1132882121
        const val BOOL_TRUE: Int = -1720552011
        const val VECTOR_CONSTRUCTOR: Int = 481674261
    }
}

/** Reader counterpart to [TlWriter] for generated Layer-specific response codecs. */
class TlReader(private val input: ByteArray) {
    private var offset = 0

    fun constructor(expected: Int): TlReader = apply {
        val actual = int()
        require(actual == expected) { "Unexpected TL constructor: 0x${actual.toUInt().toString(16)}" }
    }

    fun int(): Int {
        requireAvailable(Int.SIZE_BYTES)
        val value = (input[offset].toInt() and 0xff) or
            ((input[offset + 1].toInt() and 0xff) shl 8) or
            ((input[offset + 2].toInt() and 0xff) shl 16) or
            ((input[offset + 3].toInt() and 0xff) shl 24)
        offset += Int.SIZE_BYTES
        return value
    }

    fun long(): Long {
        requireAvailable(Long.SIZE_BYTES)
        var value = 0L
        repeat(Long.SIZE_BYTES) { byte -> value = value or ((input[offset + byte].toLong() and 0xff) shl (byte * 8)) }
        offset += Long.SIZE_BYTES
        return value
    }

    fun double(): Double = Double.fromBits(long())

    fun boolean(): Boolean = when (val constructor = int()) {
        TlWriter.BOOL_TRUE -> true
        TlWriter.BOOL_FALSE -> false
        else -> throw IllegalArgumentException("Unexpected TL boolean constructor: 0x${constructor.toUInt().toString(16)}")
    }

    fun bytes(): ByteArray {
        requireAvailable(1)
        val prefix = input[offset++].toInt() and 0xff
        val headerSize: Int
        val size: Int
        if (prefix < 254) {
            headerSize = 1
            size = prefix
        } else {
            require(prefix == 254) { "Invalid TL byte-array prefix: $prefix" }
            requireAvailable(3)
            headerSize = 4
            size = (input[offset].toInt() and 0xff) or
                ((input[offset + 1].toInt() and 0xff) shl 8) or
                ((input[offset + 2].toInt() and 0xff) shl 16)
            offset += 3
        }
        requireAvailable(size)
        val value = input.copyOfRange(offset, offset + size)
        offset += size
        requireAvailable(padding(headerSize + size))
        offset += padding(headerSize + size)
        return value
    }

    /** Reads a fixed-width TL primitive such as int128 or int256 without a byte-array prefix. */
    fun fixedBytes(size: Int): ByteArray {
        requireAvailable(size)
        val value = input.copyOfRange(offset, offset + size)
        offset += size
        return value
    }

    fun string(): String = String(bytes(), StandardCharsets.UTF_8)

    fun <T> vector(readElement: TlReader.() -> T): List<T> {
        constructor(TlWriter.VECTOR_CONSTRUCTOR)
        val size = int()
        require(size in 0..MAX_VECTOR_SIZE) { "Invalid TL vector size: $size" }
        return List(size) { readElement() }
    }

    fun requireFullyRead() {
        require(offset == input.size) { "TL input has ${input.size - offset} trailing byte(s)" }
    }

    private fun requireAvailable(size: Int) {
        require(size >= 0 && offset <= input.size - size) { "Truncated TL input" }
    }

    private fun padding(size: Int): Int = (Int.SIZE_BYTES - size % Int.SIZE_BYTES) % Int.SIZE_BYTES

    private companion object {
        const val MAX_VECTOR_SIZE: Int = 1_000_000
    }
}
