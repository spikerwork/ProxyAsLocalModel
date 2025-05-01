package io.github.stream29.proxy.server

import org.slf4j.Logger

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
fun Logger.filterKtorLogging(): Logger =
    object : Logger by this {
        override fun info(msg: String) {
            if (ktorLoggingPrefixes.any { msg.startsWith(it) }) return
            this@filterKtorLogging.info(msg)
        }
    }

private val ktorLoggingPrefixes = listOf(
    "Application started in",
    "Responding at",
    "Autoreload is disabled because the development mode is off."
)