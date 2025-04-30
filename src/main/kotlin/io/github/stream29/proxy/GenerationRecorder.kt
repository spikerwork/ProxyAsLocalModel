package io.github.stream29.proxy

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger

class GenerationRecorder(
    val logger: Logger,
) {
    private val mutex = Mutex()
    private val buffer = StringBuffer()

    @Volatile
    private var state = GenerationState.INIT
    suspend fun dump() {
        mutex.withLock {
            if (buffer.isEmpty()) return
            logger.info(buffer.toString())
            buffer.setLength(0)
        }
    }

    suspend fun onRequest(request: String) {
        mutex.withLock {
            buffer.append("Request: \n$request")
        }
    }

    suspend fun onPartialOutput(output: String) {
        mutex.withLock {
            if (state != GenerationState.PARTIAL_OUTPUT) {
                buffer.append("\nOutput: \n")
                state = GenerationState.PARTIAL_OUTPUT
            }
            buffer.append(output)
        }
    }

    suspend fun onPartialReasoning(reasoning: String) {
        mutex.withLock {
            if (state != GenerationState.PARTIAL_REASONING) {
                buffer.append("\nReasoning: \n")
                state = GenerationState.PARTIAL_REASONING
            }
            buffer.append(reasoning)
        }
    }

    suspend fun onError(message: String, e: Throwable) {
        mutex.withLock {
            dump()
            logger.error(message, e)
        }
    }
}

private enum class GenerationState {
    INIT,
    PARTIAL_OUTPUT,
    PARTIAL_REASONING
}

