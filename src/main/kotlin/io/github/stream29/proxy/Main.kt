package io.github.stream29.proxy

import kotlinx.coroutines.delay

suspend fun main() {
    globalServer.start(wait = false)
    while (true) {
        delay(10000)
    }
}