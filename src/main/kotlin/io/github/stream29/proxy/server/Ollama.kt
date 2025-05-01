package io.github.stream29.proxy.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class OModelResponse(
    val models: List<OModel>
)

@Serializable
@OptIn(ExperimentalTime::class)
data class OModel(
    val name: String,
    val model: String = name,
    @SerialName("modified_at")
    val modifiedAt: String = Clock.System.now().toString(),
    val size: Long = 0,
    val digest: String = "0",
    val details: ODetails = ODetails(),
)

@Serializable
data class ODetails(
    val format: String = "cloud",
    val family: String = "unknown",
    val families: String? = null,
    val parameterSize: String = "unknown",
    val quantizationLevel: String = "unknown",
)

@Serializable
data class OChatRequest(
    val model: String,
    val messages: List<OChatMessage>,
    val stream: Boolean = true,
    val options: OChatRequestOptions = OChatRequestOptions(),
)

@Serializable
data class OChatRequestOptions(
    val temperature: Double = 1.0,
)

@OptIn(ExperimentalTime::class)
@Serializable
data class OChatResponseChunk(
    val model: String,
    @SerialName("created_at")
    val createdTime: String = Clock.System.now().toString(),
    val message: OChatMessage,
    val done: Boolean,
    val doneReason: String? = null,
)

@Serializable
data class OChatMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null
)