package io.github.stream29.proxy

import io.github.stream29.proxy.client.ApiProvider
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val lmStudio: LmStudioConfig = LmStudioConfig(),
    val ollama: OllamaConfig = OllamaConfig(),
    val client: KtorClientConfig = KtorClientConfig(),
    val apiProviders: Map<String, ApiProvider> = emptyMap(),
    val logging: LoggingConfig = LoggingConfig()

)

@Serializable
data class LmStudioConfig(
    val port: Int = 1235,
    val host: String = "0.0.0.0",
    val path: String = "/",
    val enabled: Boolean = true,
)

@Serializable
data class OllamaConfig(
    val port: Int = 11435,
    val host: String = "0.0.0.0",
    val path: String = "/",
    val enabled: Boolean = true,
)

@Serializable
data class KtorClientConfig(
    val socketTimeout: Long = Long.MAX_VALUE,
    val connectTimeout: Long = Long.MAX_VALUE,
    val requestTimeout: Long = Long.MAX_VALUE,
    val retry: Int = 3,
    val delayBeforeRetry: Long = 1000,
    val proxyHost: String = "127.0.0.1",
    val proxyPort: Int = 8180,
    val proxyEnabled: Boolean = false,
    )

@Serializable
data class LoggingConfig(
    val level: String = "OFF" // Set to OFF, ERROR, WARN, INFO, DEBUG, or TRACE
)
