package io.github.stream29.proxy

import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatDelta
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.logging.Logger
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@SerialName("OpenAi")
@Serializable
data class OpenAiConfig(
    val baseUrl: String,
    val apiKey: String,
    val modelList: List<String>,
) : ApiProvider {
    private val client = OpenAI(
        token = apiKey,
        host = OpenAIHost(baseUrl),
        logging = LoggingConfig(logger = Logger.Empty)
    )

    override suspend fun getModelNameList(): List<String> = modelList
    override suspend fun generateLStream(request: LChatCompletionRequest): Flow<LChatCompletionResponseChunk> {
        return client.chatCompletions(request.asOpenAiRequest()).map { it.asLChatCompletionResponseChunk() }
    }

    override suspend fun generateOStream(request: OChatRequest): Flow<OChatResponseChunk> {
        return client.chatCompletions(request.asOpenAiRequest()).map { it.asOChatResponseChunk() }
    }

    override fun close() {
        client.close()
    }
}

private fun OChatRequest.asOpenAiRequest() =
    ChatCompletionRequest(
        model = ModelId(model),
        messages = messages.map { it.asOpenAiMessage() },
        temperature = options.temperature,
    )

private fun OChatMessage.asOpenAiMessage() =
    ChatMessage(
        role = role.asOpenAiRole(),
        content = content
    )

private fun ChatCompletionChunk.asOChatResponseChunk() =
    OChatResponseChunk(
        model = model.id,
        message = OChatMessage(
            role = "assistant",
            content = choices.firstOrNull()?.delta?.content ?: "",
        ),
        done = choices.firstOrNull()?.finishReason != null,
    )

private fun String.asOpenAiRole(): Role = when (this) {
    "user" -> Role.User
    "assistant" -> Role.Assistant
    "system" -> Role.System
    "tool" -> Role.Tool
    "function" -> Role.Function
    else -> throw IllegalArgumentException("Unsupported role: $this")
}

private fun LChatCompletionRequest.asOpenAiRequest() =
    ChatCompletionRequest(
        model = ModelId(model),
        messages = messages.map { it.asOpenAiMessage() },
        temperature = temperature,
    )

private fun LChatMessage.asOpenAiMessage() =
    ChatMessage(
        role = role.asOpenAiRole(),
        content = content
    )

private fun ChatCompletionChunk.asLChatCompletionResponseChunk() =
    LChatCompletionResponseChunk(
        id = id,
        model = model.id,
        choices = choices.map {
            LChatCompletionChoice(
                index = it.index,
                delta = it.delta!!.asLChatMessage(),
                finishReason = it.finishReason?.value,
            )
        }
    )

private fun ChatDelta.asLChatMessage() =
    LChatMessage(
        role = role?.role ?: "assistant",
        content = content ?: "",
    )