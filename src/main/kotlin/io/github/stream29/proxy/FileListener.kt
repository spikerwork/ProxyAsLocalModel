package io.github.stream29.proxy

import kotlinx.coroutines.*
import java.io.File

@OptIn(DelicateCoroutinesApi::class)
fun watch(file: File, onUpdate: suspend (File) -> Unit) {
    GlobalScope.launch {
        var lastModified = file.lastModified()
        while (currentCoroutineContext().isActive) {
            delay(1000)
            if (file.lastModified() == lastModified) continue
            lastModified = file.lastModified()
            onUpdate(file)
        }
    }
}