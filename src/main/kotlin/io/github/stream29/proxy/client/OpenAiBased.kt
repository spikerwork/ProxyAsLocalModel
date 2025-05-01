package io.github.stream29.proxy.client

import kotlinx.serialization.SerialName

@Suppress("unused")
@SerialName("DashScope")
data class DashScopeConfig(
    val apiKey: String,
    val modelList: List<String> = listOf("qwen-max", "qwen-plus", "qwen-turbo", "qwen-long"),
): ApiProvider by OpenAiConfig(
    baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/",
    apiKey = apiKey,
    modelList = modelList
)

@Suppress("unused")
@SerialName("DeepSeek")
data class DeepSeekConfig(
    val apiKey: String,
    val modelList: List<String> = listOf("deepseek-chat", "deepseek-reasoner"),
): ApiProvider by OpenAiConfig(
    baseUrl = "https://api.deepseek.ai/v1/",
    apiKey = apiKey,
    modelList = modelList
)

@Suppress("unused")
@SerialName("Mistral")
data class MistralConfig(
    val apiKey: String,
    val modelList: List<String> = listOf("codestral", "mistral-large"),
): ApiProvider by OpenAiConfig(
    baseUrl = "https://api.mistral.ai/v1/",
    apiKey = apiKey,
    modelList = modelList
)

@Suppress("unused")
@SerialName("SiliconFlow")
data class SiliconFlowConfig(
    val apiKey: String,
    val modelList: List<String>,
): ApiProvider by OpenAiConfig(
    baseUrl = "https://api.siliconflow.cn/v1/",
    apiKey = apiKey,
    modelList = modelList
)

@Suppress("unused")
@SerialName("Gemini")
data class GeminiConfig(
    val apiKey: String,
    val modelList: List<String>,
): ApiProvider by OpenAiConfig(
    baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
    apiKey = apiKey,
    modelList = modelList
)