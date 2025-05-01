package io.github.stream29.proxy.server

import io.github.stream29.proxy.encodeJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow

const val CRLF = "\r\n"

suspend inline fun <reified T> ApplicationCall.respondChatSSE(
    streamPrefix: String,
    streamEndToken: String,
    flow: Flow<T>
) = respondBytesWriter(ContentType.Text.EventStream) {
    try {
        flow.collect {
            writeString("$streamPrefix ${it.encodeJson()}$CRLF")
            flush()
        }
        writeString("$streamEndToken$CRLF$CRLF")
    } catch (e: Throwable) {
        writeString(e.stackTraceToString())
    }
}