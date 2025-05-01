package io.github.stream29.proxy

import com.charleskorn.kaml.*
import io.github.stream29.jsonschemagenerator.SchemaGenerator
import io.github.stream29.jsonschemagenerator.schemaOf
import io.github.stream29.proxy.client.listModelNames
import io.github.stream29.proxy.server.configureLmStudioServer
import io.github.stream29.proxy.server.configureOllamaServer
import io.github.stream29.proxy.server.embeddedServer
import io.github.stream29.proxy.server.filterKtorLogging
import io.github.stream29.streamlin.AutoUpdateMode
import io.github.stream29.streamlin.AutoUpdatePropertyRoot
import io.github.stream29.streamlin.getValue
import io.github.stream29.streamlin.setValue
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

val helpLogger = LoggerFactory.getLogger("Help")!!
val schemaGenerator = SchemaGenerator()
val configLogger = LoggerFactory.getLogger("Config")!!
val modelListLogger = LoggerFactory.getLogger("Model List")!!
val lmStudioLogger = LoggerFactory.getLogger("LM Studio Server")!!
val ollamaLogger = LoggerFactory.getLogger("Ollama Server")!!
val clientLogger = LoggerFactory.getLogger("Ktor Client")!!

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
        strictMode = false,
        singleLineStringStyle = SingleLineStringStyle.PlainExceptAmbiguous,
        multiLineStringStyle = MultiLineStringStyle.Literal,
        encodeDefaults = false
    )
)

val globalClient = HttpClient(io.ktor.client.engine.cio.CIO) {
    install(ContentNegotiation) {
        json(globalJson)
    }
    expectSuccess = true
}

val configFile = File("config.yml")

@Suppress("unused")
val unused = {
    if (!configFile.exists()) {
        helpLogger.info("It looks that you are starting the program for the first time here.")
        helpLogger.info("A default config file is created at ${configFile.absolutePath} with schema annotation.")
        configFile.writeText(
            """
# ${'$'}schema: https://github.com/Stream29/ProxyAsLocalModel/raw/master/config_v0.schema.json
lmStudio:
  port: 1234
ollama:
  port: 11434
apiProviders: {}
"""
        )
    }
    watch(configFile) { file ->
        if (!file.exists())
            return@watch
        val text = file.readText()
        try {
            val previousConfig = config
            val newConfig = text.decodeYaml<Config>()
            if (previousConfig != newConfig) {
                config = newConfig
            }
            if (previousConfig.apiProviders != newConfig.apiProviders) {
                val previousApiProviders = apiProviders
                previousApiProviders.values.forEach { it.close() }
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
    GlobalScope.launch { modelListLogger.info("Model list loaded with: ${it.listModelNames()}") }
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
inline fun <reified T> T.encodeYaml() = globalYaml.encodeToString(this)
inline fun <reified T> T.encodeJson() = globalJson.encodeToString(this)