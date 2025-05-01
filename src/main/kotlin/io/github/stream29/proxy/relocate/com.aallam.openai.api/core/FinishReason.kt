package io.github.stream29.proxy.relocate.com.aallam.openai.api.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class FinishReason(val value: String) {
    companion object
}
