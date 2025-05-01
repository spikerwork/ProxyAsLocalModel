package io.github.stream29.proxy

import io.github.stream29.jsonschemagenerator.SchemaGenerator
import io.github.stream29.jsonschemagenerator.schemaOf
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test

class GenerateSchema {
    @Test
    fun generateSchema() {
        val schemaGenerator = SchemaGenerator()
        val json = Json { prettyPrint = true }
        val schemaFile = File("config.schema.json")
        schemaFile.writeText(json.encodeToString(schemaGenerator.schemaOf<Config>()))
    }
}