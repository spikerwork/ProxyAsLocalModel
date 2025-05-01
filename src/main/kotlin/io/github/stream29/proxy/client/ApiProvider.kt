package io.github.stream29.proxy.client

import io.github.stream29.jsonschemagenerator.RefWithSerialName
import io.github.stream29.proxy.server.LChatCompletionRequest
import io.github.stream29.proxy.server.LChatCompletionResponseChunk
import io.github.stream29.proxy.server.OChatRequest
import io.github.stream29.proxy.server.OChatResponseChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("ApiProvider")
@RefWithSerialName
@Serializable
sealed interface ApiProvider : AutoCloseable {
    suspend fun getModelNameList(): List<String>
    suspend fun generateLStream(request: LChatCompletionRequest): Flow<LChatCompletionResponseChunk>
    suspend fun generateOStream(request: OChatRequest): Flow<OChatResponseChunk>
}

suspend fun Map<String, ApiProvider>.listModelNames(): List<String> =
    flatMap { (name, apiProvider) -> apiProvider.getModelNameList().map { "$name/$it" } }