package io.github.stream29.proxy

import io.ktor.server.application.*
import io.ktor.server.engine.*

fun <TEngine : ApplicationEngine, TConfiguration : ApplicationEngine.Configuration> embeddedServer(
    factory: ApplicationEngineFactory<TEngine, TConfiguration>,
    port: Int,
    host: String = "0.0.0.0",
    environment: ApplicationEnvironment = applicationEnvironment(),
    module: Application.() -> Unit = {}
) = embeddedServer(
    factory = factory,
    environment = environment,
    configure = {
        connectors.add(
            EngineConnectorBuilder().also {
                it.port = port
                it.host = host
            }
        )
    },
    module = module
)