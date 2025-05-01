package io.github.stream29.proxy.client

import io.github.stream29.proxy.globalClient
import io.github.stream29.proxy.globalJson
import io.github.stream29.proxy.relocate.com.aallam.openai.api.chat.ChatCompletionChunk
import io.github.stream29.proxy.relocate.com.aallam.openai.api.chat.ChatCompletionRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive

suspend fun createStreamingChatCompletion(
    baseUrl: String,
    apiKey: String,
    request: ChatCompletionRequest
): Flow<ChatCompletionChunk> {
    val statement = globalClient.preparePost(baseUrl) {
        url { appendPathSegments("chat", "completions") }
        setBody(request)
        contentType(ContentType.Application.Json)
        accept(ContentType.Text.EventStream)
        headers {
            append(HttpHeaders.CacheControl, "no-cache")
            append(HttpHeaders.Connection, "keep-alive")
            append(HttpHeaders.Authorization, "Bearer $apiKey")
        }
    }
    val channel: ByteReadChannel = statement.body()
    return flow {
        while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
            val line = channel.readUTF8Line()
            val value: ChatCompletionChunk = when {
                line == null -> break
                line.startsWith(STREAM_END_TOKEN) -> break
                line.startsWith(STREAM_PREFIX) -> line.decodeChunkNoReflection()
                else -> continue
            }
            emit(value)
        }
    }.onCompletion { channel.cancel() }
}

private fun String.decodeChunkNoReflection(): ChatCompletionChunk {
    return globalJson.decodeFromString(ChatCompletionChunk.serializer(), removePrefix(STREAM_PREFIX))
}

const val STREAM_PREFIX = "data:"
const val STREAM_END_TOKEN = "$STREAM_PREFIX [DONE]"