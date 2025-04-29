package io.github.stream29.proxy

import io.github.stream29.jsonschemagenerator.RefWithSerialName
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val port: Int = 1235,
    val connectTimeout: Long = 30000,
    val requestTimeout: Long = 60000,
    val socketTimeout: Long = 60000,
    val apiProviders: Map<String, ApiProvider>
)

@SerialName("ApiProvider")
@RefWithSerialName
@Serializable
sealed interface ApiProvider {
    suspend fun getModelList(): List<String>
    suspend fun generate(request: LChatCompletionRequest): Flow<LChatCompletionResponseChunk>
}