package io.github.stream29.proxy.relocate.com.aallam.openai.api.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Usage(
    /**
     * Count of prompts tokens.
     */
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    /**
     * Count of completion tokens.
     */
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    /**
     * Count of total tokens.
     */
    @SerialName("total_tokens") val totalTokens: Int? = null,
)
