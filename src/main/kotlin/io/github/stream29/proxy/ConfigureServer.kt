package io.github.stream29.proxy

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.slf4j.event.Level

fun Application.configureServer() {
    install(ContentNegotiation) {
        json(globalJson)
    }

    install(CallLogging) {
        level = Level.INFO
    }

    routing {
        get("/api/v0/models") {
            call.respond(
                apiProviders
                    .flatMap { (name, apiProvider) -> apiProvider.getModelList().map { "$name/$it" } }
                    .map { LModel(it) }
                    .let { LModelResponse(it) }
            )
        }

        post("/api/v0/chat/completions") {
            val request = call.receive<LChatCompletionRequest>()
            val apiProvider = apiProviders[request.model.substringBefore('/')]
            if (apiProvider == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }
            val requestWithOriginalModelName = request.copy(model = request.model.substringAfter('/'))
            val chunkFlow = apiProvider.generate(requestWithOriginalModelName)
            call.respondChatSSE(chunkFlow)
        }
    }
}