package io.github.stream29.proxy

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
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
                streamPrefix = "data:",
                streamEndToken = "data: [DONE]",
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
}