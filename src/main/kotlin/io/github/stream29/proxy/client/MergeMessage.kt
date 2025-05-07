package io.github.stream29.proxy.client

import io.github.stream29.proxy.server.LChatCompletionRequest
import io.github.stream29.proxy.server.LChatCompletionResponseChunk
import io.github.stream29.proxy.server.OChatRequest
import io.github.stream29.proxy.server.OChatResponseChunk
import kotlinx.coroutines.flow.Flow

fun <T> List<T>.mergeBy(
    selector: (T) -> String,
    merger: (T, T) -> T,
): List<T> {
    val result = mutableListOf<T>()
    var lastItem: T? = null
    for (item in this) {
        result.add(
            if (lastItem != null && selector(lastItem) == selector(item)) {
                merger(lastItem, item).also { result.removeAt(result.lastIndex) }
            } else {
                item
            }.also { lastItem = it }
        )
    }
    return result
}

fun ApiProvider.messageMergedByRole() = MessageMergedApiProvider(this)
class MessageMergedApiProvider(
    val delegate: ApiProvider,
) : ApiProvider by delegate {
    override suspend fun generateLStream(request: LChatCompletionRequest): Flow<LChatCompletionResponseChunk> {
        return delegate.generateLStream(
            request.copy(
                messages = request.messages.mergeBy(
                    selector = { it.role },
                    merger = { a, b -> a.copy(content = a.content + b.content) }
                )
            )
        )
    }

    override suspend fun generateOStream(request: OChatRequest): Flow<OChatResponseChunk> {
        return delegate.generateOStream(
            request.copy(
                messages = request.messages.mergeBy(
                    selector = { it.role },
                    merger = { a, b -> a.copy(content = a.content + b.content) }
                )
            )
        )
    }
}