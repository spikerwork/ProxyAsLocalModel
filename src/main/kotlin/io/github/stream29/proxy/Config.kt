package io.github.stream29.proxy

import io.github.stream29.jsonschemagenerator.RefWithSerialName
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
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

@SerialName("ApiProvider")
@RefWithSerialName
@Serializable
sealed interface ApiProvider: AutoCloseable {
    suspend fun getModelNameList(): List<String>
    suspend fun generateLStream(request: LChatCompletionRequest): Flow<LChatCompletionResponseChunk>
    suspend fun generateOStream(request: OChatRequest): Flow<OChatResponseChunk>
}

suspend fun Map<String, ApiProvider>.listModelNames(): List<String> =
    flatMap { (name, apiProvider) -> apiProvider.getModelNameList().map { "$name/$it" } }