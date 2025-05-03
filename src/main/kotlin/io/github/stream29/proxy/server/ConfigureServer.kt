package io.github.stream29.proxy.server

import io.github.stream29.proxy.*
import io.github.stream29.proxy.client.STREAM_END_TOKEN
import io.github.stream29.proxy.client.STREAM_PREFIX
import io.github.stream29.proxy.client.listModelNames
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.Logger
import org.slf4j.event.Level

fun createLmStudioServer(config: LmStudioConfig): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> =
    embeddedServer(
        factory = CIO,
        environment = applicationEnvironment { log = lmStudioLogger.filterKtorLogging() },
        port = config.port,
        host = config.host
    )
    {
        configureServerCommon(lmStudioLogger)
        routing {
            route(config.path) {
                get("/api/v0/models") {
                    call.respond<LModelResponse>(LModelResponse(apiProviders.listModelNames().map { LModel(it) }))
                }
                post("/api/v0/chat/completions") {
                    val request = call.receive<LChatCompletionRequest>()
                    val apiProvider = apiProviders[request.model.substringBefore('/')]
                    if (apiProvider == null) {
                        call.respond<HttpStatusCode>(HttpStatusCode.NotFound)
                        return@post
                    }
                    val requestWithOriginalModelName = request.copy(model = request.model.substringAfter('/'))
                    call.respondChatSSE<LChatCompletionResponseChunk>(
                        streamPrefix = STREAM_PREFIX,
                        streamEndToken = STREAM_END_TOKEN,
                        apiProvider.generateLStream(requestWithOriginalModelName)
                    )
                }
            }

        }
    }

fun createOllamaServer(config: OllamaConfig): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> =
    embeddedServer(
        factory = CIO,
        environment = applicationEnvironment { log = ollamaLogger.filterKtorLogging() },
        port = config.port,
        host = config.host
    ) {
        configureServerCommon(ollamaLogger)
        routing {
            route(config.path) {
                get("/") {
                    call.respond<String>("Ollama is running")
                }
                get("/api/tags") {
                    call.respond<OModelResponse>(
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
                        call.respond<HttpStatusCode>(HttpStatusCode.NotFound)
                        return@post
                    }
                    val requestWithOriginalModelName = request.copy(model = request.model.substringAfter('/'))
                    call.respondChatSSE<OChatResponseChunk>(
                        streamPrefix = "",
                        streamEndToken = "",
                        apiProvider.generateOStream(requestWithOriginalModelName),
                    )
                }
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
