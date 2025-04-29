package io.github.stream29.proxy

import io.github.stream29.jsonschemagenerator.SchemaGenerator
import io.github.stream29.jsonschemagenerator.schemaOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.test.Test

class GenerateSchema {
    @Test
    fun generateSchema() {
        val schemaGenerator = SchemaGenerator(
            encodeMap = {
                buildJsonObject {
                    val (keyDescriptor, valueDescriptor) = descriptor.elementDescriptors.toList()
                    if (keyDescriptor.kind != PrimitiveKind.STRING) throw SerializationException("Map key must be a string")
                    putComment()
                    putTitle()
                    putType()
                    putDescription()
                    putJsonObject("additionalProperties") {
                        put("\$ref", "#/\$defs/ApiProvider")
                        schemaOf(valueDescriptor, emptyList())
                    }
                }
            }
        )

        val schemaFile = File("config.schema.json")

        schemaFile.writeText(schemaGenerator.schemaOf<Config>().encodeJson())
    }
}