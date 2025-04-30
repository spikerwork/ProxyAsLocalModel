package io.github.stream29.proxy

import kotlinx.coroutines.delay

suspend fun main() {
    // Just to initialize Global.kt
    config
    while (true) {
        delay(10000)
    }
}