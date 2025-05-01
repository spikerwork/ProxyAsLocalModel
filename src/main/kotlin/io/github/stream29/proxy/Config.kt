package io.github.stream29.proxy

import io.github.stream29.proxy.client.ApiProvider
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val lmStudio: LmStudioConfig = LmStudioConfig(),
    val ollama: OllamaConfig = OllamaConfig(),
    val apiProviders: Map<String, ApiProvider> = emptyMap(),
)

@Serializable
data class LmStudioConfig(
    val port: Int = 1235,
    val host: String = "0.0.0.0",
    val enabled: Boolean = true,
)

@Serializable
data class OllamaConfig(
    val port: Int = 11435,
    val host: String = "0.0.0.0",
    val enabled: Boolean = true,
)