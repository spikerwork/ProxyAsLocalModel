package io.github.stream29.proxy.server

import io.github.stream29.proxy.apiProviders
import io.github.stream29.proxy.client.STREAM_END_TOKEN
import io.github.stream29.proxy.client.STREAM_PREFIX
import io.github.stream29.proxy.client.listModelNames
import io.github.stream29.proxy.globalJson
import io.github.stream29.proxy.lmStudioLogger
import io.github.stream29.proxy.ollamaLogger
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.Logger
import org.slf4j.event.Level

fun Application.configureLmStudioServer() {
    configureServerCommon(lmStudioLogger)

    routing {
        get("/api/v0/models") {
            call.respond(LModelResponse(apiProviders.listModelNames().map { LModel(it) }))
        }

        post("/api/v0/chat/completions") {
            val request = call.receive<LChatCompletionRequest>()
            val apiProvider = apiProviders[request.model.substringBefore('/')]
            if (apiProvider == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }
            val requestWithOriginalModelName = request.copy(model = request.model.substringAfter('/'))
            call.respondChatSSE(
                streamPrefix = STREAM_PREFIX,
                streamEndToken = STREAM_END_TOKEN,
                apiProvider.generateLStream(requestWithOriginalModelName)
            )
        }
    }
}

fun Application.configureOllamaServer() {
    configureServerCommon(ollamaLogger)

    routing {
        get("/") {
            call.respond("Ollama is running")
        }
        get("/api/tags") {
            call.respond(
                apiProviders
                    .listModelNames()
                    .map { OModel(it) }
                    .let { OModelResponse(it) }
            )
        }

        post("/api/chat") {
            val request = call.receive<OChatRequest>()
            val apiProvider = apiProviders[request.model.substringBefore('/')]
            if (apiProvider == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }
            val requestWithOriginalModelName = request.copy(model = request.model.substringAfter('/'))
            call.respondChatSSE(
                streamPrefix = "",
                streamEndToken = "",
                apiProvider.generateOStream(requestWithOriginalModelName),
            )
        }
    }
}

private fun Application.configureServerCommon(callLogger: Logger) {
    install(ContentNegotiation) {
        json(globalJson)
    }

    install(CallLogging) {
        level = Level.INFO
        logger = callLogger
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = cause.stackTraceToString(), status = HttpStatusCode.InternalServerError)
            callLogger.error("Error processing request.", cause)
        }
    }
}