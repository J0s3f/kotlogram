package com.github.badoualy.telegram.tl.builder

import com.github.badoualy.telegram.tl.builder.parser.TLDefinitionBuilder
import java.io.File

private val TL_SCHEMA_LEVEL = 71
private val TL_SCHEMA_PATH = "./tl-builder/src/main/resources/tl-schema-$TL_SCHEMA_LEVEL.tl"

val OUTPUT = "./tl/src/main/java"
val OUTPUT_TEST = "./tl/src/test/java"


fun main(args: Array<String>) {
    println("TL Compiler developed by Yannick Badoual, Kotlogram (c) 2016 v2.0")

    val tlSchemaFile = File(TL_SCHEMA_PATH)

    val tlDef = TLDefinitionBuilder.build(tlSchemaFile)

}