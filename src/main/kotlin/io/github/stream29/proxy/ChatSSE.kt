package io.github.stream29.proxy

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow

const val STREAM_PREFIX = "data:"
const val STREAM_END_TOKEN = "$STREAM_PREFIX [DONE]"
const val CRLF = "\r\n"

suspend inline fun <reified T> ApplicationCall.respondChatSSE(flow: Flow<T>) =
    respondBytesWriter(ContentType.Text.EventStream) {
        flow.collect {
            writeString("$STREAM_PREFIX ${it.encodeJson<T>()}$CRLF$CRLF")
            flush()
        }
        writeString("$STREAM_END_TOKEN$CRLF$CRLF")
        flush()
    }