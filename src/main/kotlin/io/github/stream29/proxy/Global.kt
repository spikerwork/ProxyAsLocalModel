package io.github.stream29.proxy

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.stream29.streamlin.AutoUpdateMode
import io.github.stream29.streamlin.AutoUpdatePropertyRoot
import io.github.stream29.streamlin.getValue
import io.github.stream29.streamlin.setValue
import io.ktor.server.engine.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

val logger = LoggerFactory.getLogger("Global")!!

val globalJson = Json {
    prettyPrint = false
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

val globalYaml = Yaml(
    configuration = YamlConfiguration(
        polymorphismStyle = PolymorphismStyle.Property,
        strictMode = false
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
            val previousConfig = config
            val newConfig = text.decodeYaml<Config>()
            if (previousConfig != newConfig) {
                config = newConfig
            }
            if (previousConfig.apiProviders != newConfig.apiProviders) {
                logger.info("Refresh apiProviders: ${newConfig.apiProviders.keys}")
                apiProviderProperty.set(newConfig.apiProviders)
            }
            if (previousConfig.lmStudio != newConfig.lmStudio) {
                logger.info("Restart lmStudioServer: port ${newConfig.lmStudio.port} enabled: ${newConfig.lmStudio.enabled}")
                val previousServer = lmStudioServer
                previousServer?.stop()
                lmStudioConfigProperty.set(newConfig.lmStudio)
            }
            if (previousConfig.ollama != newConfig.ollama) {
                logger.info("Restart ollamaServer: port ${newConfig.ollama.port} enabled: ${newConfig.ollama.enabled}")
                val previousServer = ollamaServer
                previousServer?.stop()
                ollamaConfigProperty.set(newConfig.ollama)
            }
        } catch (e: SerializationException) {
            logger.error("Config: ${e.message}")
        }
    }
}()

private val configProperty = AutoUpdatePropertyRoot(
    sync = true,
    mode = AutoUpdateMode.PROPAGATE,
    initValue = configFile.readText().decodeYaml<Config>()
)

var config by configProperty

private val apiProviderProperty = AutoUpdatePropertyRoot(
    sync = true,
    mode = AutoUpdateMode.PROPAGATE,
    initValue = config.apiProviders
)

val apiProviders by apiProviderProperty

private val lmStudioConfigProperty = AutoUpdatePropertyRoot(
    sync = true,
    mode = AutoUpdateMode.PROPAGATE,
    initValue = config.lmStudio
)

val lmStudioServer by lmStudioConfigProperty.subproperty {
    if (it.enabled)
        embeddedServer(
            factory = io.ktor.server.cio.CIO,
            port = it.port,
            watchPaths = emptyList()
        ) {
            configureLmStudioServer()
        }.apply { start(wait = false) }
    else null
}

private val ollamaConfigProperty = AutoUpdatePropertyRoot(
    sync = true,
    mode = AutoUpdateMode.PROPAGATE,
    initValue = config.ollama
)

val ollamaServer by ollamaConfigProperty.subproperty {
    if(it.enabled)
        embeddedServer(
            factory = io.ktor.server.cio.CIO,
            port = it.port,
            watchPaths = emptyList()
        ) {
            configureOllamaServer()
        }.apply { start(wait = false) }
    else null
}

inline fun <reified T> String.decodeYaml() = globalYaml.decodeFromString<T>(this)
inline fun <reified T> T.encodeJson() = globalJson.encodeToString(this)