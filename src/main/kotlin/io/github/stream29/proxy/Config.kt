package io.github.stream29.proxy

import io.github.stream29.jsonschemagenerator.RefWithSerialName
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val lmStudio: LmStudioConfig = LmStudioConfig(),
    val ollama: OllamaConfig = OllamaConfig(),
    val apiProviders: Map<String, ApiProvider>
)

@Serializable
data class LmStudioConfig(
    val port: Int = 1235,
    val enabled: Boolean = true,
)

@Serializable
data class OllamaConfig(
    val port: Int = 11435,
    val enabled: Boolean = true,
)

@SerialName("ApiProvider")
@RefWithSerialName
@Serializable
sealed interface ApiProvider {
    suspend fun getModelList(): List<String>
    suspend fun generate(request: LChatCompletionRequest): Flow<LChatCompletionResponseChunk>
}