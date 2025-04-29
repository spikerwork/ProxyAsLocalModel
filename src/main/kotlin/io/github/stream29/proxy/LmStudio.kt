package io.github.stream29.proxy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class LModel(
    val id: String,
    val `object`: String = "model",
    val type: String = "llm",
    val publisher: String = "alibaba",
    val arch: String = "qwen",
    @SerialName("compatibility_type")
    val compatibilityType: String = "gguf",
    val quantization: String = "Q4_0",
    val state: String = "not-loaded",
    @SerialName("max_context_length")
    val maxContextLength: Int = 1_000_000,
)

@Serializable
data class LModelResponse(
    val data: List<LModel>,
    val `object`: String = "list"
)

@Serializable
data class LChatCompletionRequest(
    val model: String,
    val messages: List<LChatMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int = -1,
    val temperature: Double = 1.0,
    val stream: Boolean = false,
)

@Serializable
data class LChatCompletionResponseChunk(
    val id: String,
    val model: String,
    val choices: List<LChatCompletionChoice>,
    val `object`: String = "chat.completion.chunk",
    @SerialName("system_fingerprint")
    val systemFingerprint: String = model,
    val created: Long = System.currentTimeMillis()
)

@Serializable
data class LChatCompletionChoice(
    val index: Int = 0,
    val delta: LChatMessage,
    @SerialName("logprobs")
    val logprobs: String? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class LChatMessage(
    val role: String,
    val content: String
)