package io.github.stream29.proxy

import com.alibaba.dashscope.aigc.generation.Generation
import com.alibaba.dashscope.aigc.generation.GenerationParam
import com.alibaba.dashscope.common.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
@SerialName("Qwen")
data class QwenApiProvider(
    val modelList: List<String>,
    val apiKey: String,
    val enableSearch: Boolean = true,
    val enableThinking: Boolean = true,
) : ApiProvider {
    override suspend fun getModelList(): List<String> = modelList
    override suspend fun generate(
        request: LChatCompletionRequest
    ): Flow<LChatCompletionResponseChunk> {
        val generation = Generation()
        val request = buildRequest {
            model(request.model)
            apiKey(apiKey)
            enableSearch(enableSearch)
            enableThinking(enableThinking)
            incrementalOutput(request.stream)
            temperature(request.temperature.toFloat())
            messages(
                request.messages.map {
                    buildMessage {
                        role(it.role)
                        content(it.content)
                    }
                }
            )
        }
        return generation.streamCall(request).asFlow().map {
            LChatCompletionResponseChunk(
                id = it.requestId,
                model = request.model,
                choices = listOf(
                    LChatCompletionChoice(
                        delta = LChatMessage(
                            role = "assistant",
                            content = it.output.text
                        ),
                        finishReason = it.output.finishReason,
                    )
                )
            )
        }
    }
}

internal inline fun buildRequest(
    builderAction: GenerationParam.GenerationParamBuilder<*, *>.() -> Unit
) = GenerationParam.builder().apply(builderAction).build()

internal inline fun buildMessage(
    builderAction: Message.MessageBuilder<*, *>.() -> Unit
) = Message.builder().apply(builderAction).build()