package io.github.stream29.proxy

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.stream29.streamlin.AutoUpdateMode
import io.github.stream29.streamlin.AutoUpdatePropertyRoot
import io.github.stream29.streamlin.getValue
import io.github.stream29.streamlin.setValue
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLogger
import java.io.File

val configLogger = LoggerFactory.getLogger("Config")!!
val apiLogger = LoggerFactory.getLogger("API")!!
val lmStudioLogger = LoggerFactory.getLogger("LM Studio Server")!!
val ollamaLogger = LoggerFactory.getLogger("Ollama Server")!!

val globalJson = Json {
    prettyPrint = false
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
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
                apiProviderProperty.set(newConfig.apiProviders)
            }
            if (previousConfig.lmStudio != newConfig.lmStudio) {
                val previousServer = lmStudioServer
                previousServer?.stop()
                lmStudioConfigProperty.set(newConfig.lmStudio)
            }
            if (previousConfig.ollama != newConfig.ollama) {
                val previousServer = ollamaServer
                previousServer?.stop()
                ollamaConfigProperty.set(newConfig.ollama)
            }
        } catch (e: SerializationException) {
            configLogger.error("${e.message}")
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

@OptIn(DelicateCoroutinesApi::class)
val apiProviders by apiProviderProperty.subproperty {
    GlobalScope.launch { apiLogger.info("Model list loaded with: ${it.listModelNames()}") }
    it
}

private val lmStudioConfigProperty = AutoUpdatePropertyRoot(
    sync = true,
    mode = AutoUpdateMode.PROPAGATE,
    initValue = config.lmStudio
)

val lmStudioServer by lmStudioConfigProperty.subproperty { config ->
    if (config.enabled) {
        lmStudioLogger.info("LM Studio Server started at ${config.port}")
        embeddedServer(
            factory = CIO,
            environment = applicationEnvironment { log = lmStudioLogger.filterKtorLogging() },
            port = config.port,
            host = config.host,
            module = { configureLmStudioServer() }
        ).apply { start(wait = false) }
    } else null
}

private val ollamaConfigProperty = AutoUpdatePropertyRoot(
    sync = true,
    mode = AutoUpdateMode.PROPAGATE,
    initValue = config.ollama
)

val ollamaServer by ollamaConfigProperty.subproperty {
    if (it.enabled) {
        ollamaLogger.info("Ollama Server started at ${it.port}")
        embeddedServer(
            factory = CIO,
            environment = applicationEnvironment { log = ollamaLogger.filterKtorLogging() },
            port = it.port,
            host = it.host,
            module = { configureOllamaServer() }
        ).apply { start(wait = false) }
    } else null
}

inline fun <reified T> String.decodeYaml() = globalYaml.decodeFromString<T>(this)
inline fun <reified T> T.encodeJson() = globalJson.encodeToString(this)