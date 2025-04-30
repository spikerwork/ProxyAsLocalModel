package io.github.stream29.proxy

import org.slf4j.Logger

fun Logger.filterKtorLogging(): Logger =
    object : Logger by this {
        override fun info(msg: String) {
            if (msg.startsWith("Application started in") || msg.startsWith("Responding at"))
                return
        }
    }