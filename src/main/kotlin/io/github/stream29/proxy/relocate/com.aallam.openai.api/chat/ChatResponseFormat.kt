package io.github.stream29.proxy.relocate.com.aallam.openai.api.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * An object specifying the format that the model must output.
 */
@Serializable
data class ChatResponseFormat(
    /**
     * Response format type.
     */
    @SerialName("type") val type: String,

    /**
     * Optional JSON schema specification when type is "json_schema"
     */
    @SerialName("json_schema") val jsonSchema: JsonSchema? = null
) {
    companion object {
        /**
         * JSON mode, which guarantees the message the model generates, is valid JSON.
         */
        val JsonObject: ChatResponseFormat = ChatResponseFormat(type = "json_object")

    }
}

/**
 * Specification for JSON schema response format
 */
@Serializable
data class JsonSchema(
    /**
     * Optional name for the schema
     */
    @SerialName("name") val name: String? = null,

    /**
     * The JSON schema specification
     */
    @SerialName("schema") val schema: JsonObject,

    /**
     * Whether to enforce strict schema validation
     */
    @SerialName("strict") val strict: Boolean? = null
)
