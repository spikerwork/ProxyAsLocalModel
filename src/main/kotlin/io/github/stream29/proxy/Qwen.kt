package io.github.stream29.proxy

import com.alibaba.dashscope.aigc.generation.Generation
import com.alibaba.dashscope.aigc.generation.GenerationParam
import com.alibaba.dashscope.aigc.generation.GenerationResult
import com.alibaba.dashscope.common.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
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
    override suspend fun getModelNameList(): List<String> = modelList
    override suspend fun generateLStream(
        request: LChatCompletionRequest
    ): Flow<LChatCompletionResponseChunk> {
        return generateQStream(request.asQRequest()).map { it.asLChunk(request.model) }
    }

    override suspend fun generateOStream(oRequest: OChatRequest): Flow<OChatResponseChunk> {
        return generateQStream(oRequest.asQRequest()).map { it.asOChunk(oRequest.model) }
    }

    override fun close() {}

    private fun LChatCompletionRequest.asQRequest(): GenerationParam = buildQRequest {
        model(model)
        apiKey(apiKey)
        enableSearch(enableSearch)
        enableThinking(enableThinking)
        incrementalOutput(stream)
        temperature(temperature.toFloat())
        messages(
            messages.map {
                buildQMessage {
                    role(it.role)
                    content(it.content)
                }
            }
        )
    }

    private fun GenerationResult.asOChunk(modelName: String) = OChatResponseChunk(
        model = modelName,
        message = OChatMessage(
            role = "assistant",
            content = textOrNull() ?: "",
        ),
        done = finishReasonOrNull() != null,
        doneReason = finishReasonOrNull()
    )

    private fun GenerationResult.asLChunk(modelName: String) = LChatCompletionResponseChunk(
        id = requestId,
        model = modelName,
        choices = listOf(
            LChatCompletionChoice(
                delta = LChatMessage(
                    role = "assistant",
                    content = textOrNull() ?: "",
                ),
                finishReason = finishReasonOrNull(),
            )
        )
    )

    private fun OChatRequest.asQRequest(): GenerationParam = buildQRequest {
        model(model)
        apiKey(apiKey)
        enableSearch(enableSearch)
        enableThinking(enableThinking)
        incrementalOutput(stream)
        temperature(options.temperature.toFloat())
        messages(
            messages.map {
                buildQMessage {
                    role(it.role)
                    content(it.content)
                }
            }
        )
    }
}

private suspend fun generateQStream(qRequest: GenerationParam): Flow<GenerationResult> {
    val generation = Generation()
    val recorder = GenerationRecorder(qwenLogger)
    recorder.onRequest(qRequest.prettyPrint())
    return generation.streamCall(qRequest).asFlow()
        .onEach { generationResult ->
            generationResult.textOrNull()?.let { recorder.onPartialOutput(it) }
            generationResult.reasoningOrNull()?.let { recorder.onPartialReasoning(it) }
        }.onCompletion {
            it?.let { recorder.onError("Error during generation", it) }
            recorder.dump()
        }
}

private fun GenerationParam.prettyPrint() = buildString {
    appendLine("Model: $model")
    appendLine("Enable Search: $enableSearch")
    appendLine("Enable Thinking: $enableThinking")
    appendLine("Temperature: $temperature")
    appendLine("Messages:")
    messages.forEach { message ->
        appendLine("${message.role}: ${message.content}")
    }
}

private fun GenerationResult.finishReasonOrNull(): String? {
    output.finishReason?.takeIf { it.isNotEmpty() && it != "null" }?.let { return it }
    output.choices?.firstOrNull()?.finishReason?.takeIf { it.isNotEmpty() && it != "null" }?.let { return it }
    return null
}

private fun GenerationResult.textOrNull(): String? =
    output.text?.takeIf { it.isNotEmpty() }
        ?: output.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotEmpty() }

private fun GenerationResult.reasoningOrNull(): String? =
    output.choices?.firstOrNull()?.message?.reasoningContent?.takeIf { it.isNotEmpty() }

private inline fun buildQRequest(
    builderAction: GenerationParam.GenerationParamBuilder<*, *>.() -> Unit
): GenerationParam = GenerationParam.builder().apply(builderAction).build()

private inline fun buildQMessage(
    builderAction: Message.MessageBuilder<*, *>.() -> Unit
): Message = Message.builder().apply(builderAction).build()