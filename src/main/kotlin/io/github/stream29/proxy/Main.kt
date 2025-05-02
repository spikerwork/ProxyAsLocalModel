package io.github.stream29.proxy

import kotlinx.coroutines.delay

suspend fun main() {
    try {
        // Just to initialize Global.kt
        config
    } catch (t: Throwable) {
        t.printStackTrace()
        println("Failed to init proxy: ${t.message}")
    }
    while (true) {
        delay(10000)
    }
}