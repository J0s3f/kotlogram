package com.github.badoualy.telegram.tl.builder.parser

import java.io.File
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object TLDefinitionBuilder {

    private val genericRegex = Regex("([a-zA-Z]+)<([a-zA-Z]+)>") // Vector<SomeKindOfType>
    private val flagRegex = Regex("([a-zA-Z]+).(\\d+)\\?([a-zA-Z<>.]+)") // flags.0?true
    private val rawRegex = Regex("[a-zA-Z].+")

    private val tlTypeRegex = "([^#]+)#([a-f0-9]+) ([^=]*?) ?= ([^;]+);".toRegex()

    private val typeMap = HashMap<String, TLTypeRaw>()

    fun build(file: File): TLDefinition {
        typeMap.clear()
        println("Reading TL-Schema...")

        val defList = splitDefList(file)
        val typesDef = mapToConstructorDef(defList.first)
        val methodsDef = mapToConstructorDef(defList.second)

        // Create all types
        typesDef.map { it.type }.distinct()
                .map { Pair(it, createConstructorType(it)) }
                .toMap(typeMap)

        // Create all types constructors
        val types = typesDef.map {
            TLConstructor(it.name, it.id, it.parameters.map {
                TLParameter(it.first, createType(it.second))
            }, typeMap[it.type]!!)
        }

        // Create all methods constructors
        val methods = methodsDef.map {
            TLMethod(it.name, it.id, it.parameters.map {
                TLParameter(it.first, createType(it.second))
            }, createType(it.type, false))
        }

        // TODO: build abstracted types constructors
        val occurrencesMap = HashMap<String, Int>()
        typeMap.values.forEach { occurrencesMap.put(it.name, occurrencesMap[it.name] ?: 0 + 1) }
        val abstractedTypes = occurrencesMap.filter { it.value > 1 }.keys
        println("To abstract"  + abstractedTypes.size)

        return TLDefinition(HashMap(typeMap), types, methods)
    }

    // Split def list between types and methods
    private fun splitDefList(file: File): Pair<List<String>, List<String>> {
        val nodes = Pair(ArrayList<String>(), ArrayList<String>())

        var parsingMethods = false
        file.forEachLine { line ->
            when {
                line.isBlank() -> Unit
                line.startsWith("//") -> Unit
                line == "---types---" -> Unit
                line == "---functions---" -> parsingMethods = true
                !tlTypeRegex.matches(line.trim()) ->
                    System.err.println("Line not matching tlTypeRegex:\n$line")
                else -> {
                    (if (!parsingMethods)
                        nodes.first
                    else
                        nodes.second).add(line.trim())
                }
            }
        }

        return nodes
    }

    private fun mapToConstructorDef(constructorsDef: List<String>) = constructorsDef.mapNotNull { def ->
        val groups = tlTypeRegex.matchEntire(def)!!.groups

        val name = groups[1]!!.value
        val id = groups[2]!!.value.toLong(16)
        val paramsDef = if (groups.size == 5) groups[3]!!.value.trim().split(' ') else emptyList()
        val type = groups.last()!!.value

        if (IgnoredTypes.contains(type))
            return@mapNotNull null

        val parameters = paramsDef.mapNotNull { paramDef ->
            when {
                paramDef.isBlank() -> null
                paramDef.replace(" ", "") == "{X:Type}" -> null
                else -> {
                    val fields = paramDef.split(':')
                    Pair(fields[0], fields[1])
                }
            }
        }

        ConstructorDef(name, id.toInt(), parameters, type)
    }

    private fun createConstructorType(typeName: String): TLTypeRaw = when {
        typeName.matches(rawRegex) -> TLTypeRaw(typeName)
        else -> throw RuntimeException("Unsupported type $typeName for constructor/method}")
    }

    private fun createType(typeName: String, isParameter: Boolean = true): TLType = when {
        !isParameter && typeName == "X" -> TLTypeAny
        isParameter && typeName == "!X" -> TLTypeFunctional
        isParameter && typeName == "#" -> TLTypeFlag
        isParameter && typeName.matches(flagRegex) -> {
            val groups = flagRegex.matchEntire(typeName)?.groups!!
            val maskName = groups[1]?.value
                    ?: throw RuntimeException("Unknown error with type $typeName")
            val value = groups[2]?.value?.toInt()
                    ?: throw RuntimeException("Unknown error with type $typeName")
            val realType = groups[3]?.value
                    ?: throw RuntimeException("Unknown error with type $typeName")
            if (maskName != "flags")
                throw RuntimeException("Unsupported flag name, expected `flags`")

            TLTypeConditional(value, createType(realType))
        }
        typeName.matches(genericRegex) -> {
            val groups = genericRegex.matchEntire(typeName)?.groups!!
            val tlName: String = groups[1]?.value
                    ?: throw RuntimeException("Unknown error with type $typeName")
            val genericName: String = groups[2]?.value
                    ?: throw RuntimeException("Unknown error with type $typeName")
            if (!SupportedGenericTypes.contains(tlName))
                throw RuntimeException("Unsupported generic type $tlName")

            TLTypeGeneric(tlName, arrayOf(createType(genericName)))
        }
        typeName.matches(rawRegex) -> {
            when {
                BuiltInTypes.contains(typeName) -> TLTypeRaw(typeName)
                typeMap.containsKey(typeName) -> typeMap[typeName]!!
                else -> throw RuntimeException("Unknown type $typeName")
            }
        }
        else -> throw RuntimeException("Unsupported type $typeName for" +
                                               if (isParameter) "parameter"
                                               else "method")
    }

    private data class ConstructorDef(val name: String, val id: Int, val parameters: List<Pair<String, String>>, val type: String)
}
