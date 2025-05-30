package io.github.stream29.proxy.relocate.com.aallam.openai.api.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generated chat message.
 */
@Serializable
data class ChatDelta(
    /**
     * The role of the author of this message.
     */
    @SerialName("role") val role: ChatRole? = null,

    /**
     * The contents of the message.
     */
    @SerialName("content") val content: String? = null,

    @SerialName("reasoning_content")
    val reasoningContent: String? = null,

    /**
     * The name and arguments of a function that should be called, as generated by the model.
     */
    @Deprecated(message = "Deprecated in favor of toolCalls")
    @SerialName("function_call") val functionCall: FunctionCall? = null,

    /**
     * The tool calls generated by the model, such as function calls.
     */
    @SerialName("tool_calls") val toolCalls: List<ToolCallChunk>? = null,

    /**
     * Tool call ID.
     */
    @SerialName("tool_call_id") val toolCallId: ToolId? = null,
)
