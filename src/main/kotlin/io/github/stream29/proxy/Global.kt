package io.github.stream29.proxy

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.stream29.streamlin.AutoUpdateMode
import io.github.stream29.streamlin.AutoUpdatePropertyRoot
import io.github.stream29.streamlin.getValue
import io.ktor.server.engine.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

val globalJson = Json {
    prettyPrint = false
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

val globalYaml = Yaml(
    configuration = YamlConfiguration(
        polymorphismStyle = PolymorphismStyle.Property
    )
)

val configFile: File = File("config.yml")

@Suppress("unused")
val unused = {
    watch(configFile) {
        if (!it.exists())
            return@watch
        val text = it.readText()
        try {
            val deserialized = text.decodeYaml<Config>()
            if (deserialized.port != config.port) {
                val previousServer = globalServer
                previousServer.stop()
                globalServer.start(wait = false)
            }
            configProperty.set(deserialized)
        } catch (_: SerializationException) {
        }
    }
}()

val configProperty = AutoUpdatePropertyRoot(
    sync = true,
    mode = AutoUpdateMode.PROPAGATE,
    initValue = configFile.readText().decodeYaml<Config>()
)

val config by configProperty

val apiProviders by configProperty.subproperty { it.apiProviders }

val globalServer by configProperty.subproperty {
    embeddedServer(
        factory = io.ktor.server.cio.CIO,
        port = it.port,
        watchPaths = emptyList()
    ) {
        configureServer()
    }
}

inline fun <reified T> String.decodeJson() = globalJson.decodeFromString<T>(this)
inline fun <reified T> String.decodeYaml() = globalYaml.decodeFromString<T>(this)
inline fun <reified T> T.encodeJson() = globalJson.encodeToString(this)