package org.kotlogramme.raw

/**
 * A value accepted by the experimental dynamic Layer-216 raw API.
 *
 * The sealed model makes every TL wire value explicit. [Object] is named after a constructor in
 * the bundled schema and may itself contain vectors and nested objects.
 */
sealed interface RawValue {
    data class IntValue(val value: Int) : RawValue
    data class LongValue(val value: Long) : RawValue
    data class DoubleValue(val value: Double) : RawValue
    data class StringValue(val value: String) : RawValue
    data class BytesValue(val value: ByteArray) : RawValue
    data class BooleanValue(val value: Boolean) : RawValue
    data class VectorValue(val values: List<RawValue>) : RawValue
    data class Object(val constructorName: String, val fields: Map<String, RawValue> = emptyMap()) : RawValue
}

/** Runtime codec fed solely by the checked-in Layer-specific raw schema manifest. */
internal class RawTlDynamicCodec(private val schema: RawSchema) {
    private val methods = schema.functions.associateBy { it.name }
    private val constructorsByName = schema.constructors.associateBy { it.name }
    private val constructorsById = schema.constructors.associateBy { it.constructorId }

    fun encodeMethod(name: String, fields: Map<String, RawValue>): ByteArray {
        val method = requireNotNull(methods[name]) { "Unknown Layer-${schema.layer} method: $name" }
        return TlWriter().constructor(method.constructorId).also {
            writeFields(it, parseFields(method.parameters), fields)
        }.toByteArray()
    }

    fun decodeMethodResult(name: String, body: ByteArray): RawValue {
        val method = requireNotNull(methods[name]) { "Unknown Layer-${schema.layer} method: $name" }
        val reader = TlReader(body)
        val result = readValue(reader, method.result)
        reader.requireFullyRead()
        return result
    }

    private fun writeObject(writer: TlWriter, value: RawValue.Object, expectedType: String) {
        val constructor = requireNotNull(constructorsByName[value.constructorName]) {
            "Unknown Layer-${schema.layer} constructor: ${value.constructorName}"
        }
        require(matchesType(constructor.result, expectedType)) {
            "${value.constructorName} produces ${constructor.result}, not $expectedType"
        }
        writer.constructor(constructor.constructorId)
        writeFields(writer, parseFields(constructor.parameters), value.fields)
    }

    private fun writeFields(writer: TlWriter, fields: List<Field>, values: Map<String, RawValue>) {
        val allowed = fields.map { it.name }.toSet()
        require(values.keys.all { it in allowed }) { "Unknown TL field(s): ${values.keys - allowed}" }
        val flags = fields.filter { it.type == "#" }.associate { field ->
            field.name to fields.filter { it.flagName == field.name && values.containsKey(it.name) }
                .fold(0) { bits, field -> bits or (1 shl requireNotNull(field.flagBit)) }
        }
        fields.forEach { field ->
            when {
                field.type == "#" -> writer.int(requireNotNull(flags[field.name]))
                field.flagName != null && !values.containsKey(field.name) -> Unit
                field.type == "true" -> requireBoolean(values, field.name, true)
                else -> writeValue(writer, field.type, requireNotNull(values[field.name]) { "Missing TL field: ${field.name}" })
            }
        }
    }

    private fun writeValue(writer: TlWriter, type: String, value: RawValue) {
        when (type) {
            "int" -> writer.int((value as? RawValue.IntValue)?.value ?: typeError(type, value))
            "long" -> writer.long((value as? RawValue.LongValue)?.value ?: typeError(type, value))
            "double" -> writer.double((value as? RawValue.DoubleValue)?.value ?: typeError(type, value))
            "string" -> writer.string((value as? RawValue.StringValue)?.value ?: typeError(type, value))
            "bytes" -> writer.bytes((value as? RawValue.BytesValue)?.value ?: typeError(type, value))
            "int128" -> writer.fixedBytes(fixedBytes(value, 16, type))
            "int256" -> writer.fixedBytes(fixedBytes(value, 32, type))
            "Bool" -> writer.boolean((value as? RawValue.BooleanValue)?.value ?: typeError(type, value))
            else -> when {
                type.startsWith("Vector<") && type.endsWith('>') -> {
                    val vector = value as? RawValue.VectorValue ?: typeError(type, value)
                    val elementType = type.substringAfter('<').dropLast(1)
                    writer.vector(vector.values) { writeValue(this, elementType, it) }
                }
                type.startsWith('!') -> writeUntypedObject(writer, value)
                else -> writeObject(writer, value as? RawValue.Object ?: typeError(type, value), type)
            }
        }
    }

    private fun writeUntypedObject(writer: TlWriter, value: RawValue) {
        val objectValue = value as? RawValue.Object ?: typeError("generic object", value)
        val constructor = requireNotNull(constructorsByName[objectValue.constructorName]) {
            "Unknown Layer-${schema.layer} constructor: ${objectValue.constructorName}"
        }
        writer.constructor(constructor.constructorId)
        writeFields(writer, parseFields(constructor.parameters), objectValue.fields)
    }

    private fun readValue(reader: TlReader, type: String): RawValue = when (type) {
        "int" -> RawValue.IntValue(reader.int())
        "long" -> RawValue.LongValue(reader.long())
        "double" -> RawValue.DoubleValue(reader.double())
        "string" -> RawValue.StringValue(reader.string())
        "bytes" -> RawValue.BytesValue(reader.bytes())
        "int128" -> RawValue.BytesValue(reader.fixedBytes(16))
        "int256" -> RawValue.BytesValue(reader.fixedBytes(32))
        "Bool" -> RawValue.BooleanValue(reader.boolean())
        else -> when {
            type.startsWith("Vector<") && type.endsWith('>') -> {
                val elementType = type.substringAfter('<').dropLast(1)
                RawValue.VectorValue(reader.vector { readValue(this, elementType) })
            }
            type.startsWith('!') || type == "X" -> readObject(reader, null)
            else -> readObject(reader, type)
        }
    }

    private fun readObject(reader: TlReader, expectedType: String?): RawValue.Object {
        val constructorId = reader.int()
        val constructor = requireNotNull(constructorsById[constructorId]) {
            "Unknown Layer-${schema.layer} constructor: 0x${constructorId.toUInt().toString(16)}"
        }
        require(expectedType == null || matchesType(constructor.result, expectedType)) {
            "${constructor.name} produces ${constructor.result}, not $expectedType"
        }
        val values = linkedMapOf<String, RawValue>()
        val flags = mutableMapOf<String, Int>()
        parseFields(constructor.parameters).forEach { field ->
            when {
                field.type == "#" -> flags[field.name] = reader.int()
                field.flagName != null && (requireNotNull(flags[field.flagName]) and (1 shl requireNotNull(field.flagBit))) == 0 -> Unit
                field.type == "true" -> values[field.name] = RawValue.BooleanValue(true)
                else -> values[field.name] = readValue(reader, field.type)
            }
        }
        return RawValue.Object(constructor.name, values)
    }

    private fun fixedBytes(value: RawValue, size: Int, type: String): ByteArray =
        (value as? RawValue.BytesValue)?.value?.also { require(it.size == size) { "$type requires exactly $size bytes" } }
            ?: typeError(type, value)

    private fun requireBoolean(values: Map<String, RawValue>, name: String, expected: Boolean) {
        require((values[name] as? RawValue.BooleanValue)?.value == expected) { "$name must be $expected" }
    }

    private fun matchesType(actual: String, expected: String): Boolean = actual.removePrefix("!") == expected.removePrefix("!")

    private fun typeError(expected: String, value: RawValue): Nothing =
        throw IllegalArgumentException("Expected $expected, received ${value::class.simpleName}")

    private fun parseFields(parameters: String): List<Field> = parameters.split(' ').mapNotNull { token ->
        if (token.isBlank() || token.startsWith('{')) return@mapNotNull null
        val separator = token.indexOf(':')
        require(separator > 0) { "Unsupported TL parameter: $token" }
        val name = token.substring(0, separator)
        val declaredType = token.substring(separator + 1)
        val optional = OPTIONAL_FIELD.matchEntire(declaredType)
        Field(name, optional?.groupValues?.get(3) ?: declaredType, optional?.groupValues?.get(1), optional?.groupValues?.get(2)?.toInt())
    }

    private data class Field(val name: String, val type: String, val flagName: String? = null, val flagBit: Int? = null)

    private companion object {
        val OPTIONAL_FIELD = Regex("([A-Za-z0-9_]+)\\.(\\d+)\\?(.+)")
    }
}
