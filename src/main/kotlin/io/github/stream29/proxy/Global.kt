package io.github.stream29.proxy

import java.io.File
import ch.qos.logback.classic.LoggerContext
import com.charleskorn.kaml.*
import io.github.stream29.proxy.client.listModelNames
import io.github.stream29.proxy.server.createLmStudioServer
import io.github.stream29.proxy.server.createOllamaServer
import io.github.stream29.streamlin.AutoUpdateMode
import io.github.stream29.streamlin.AutoUpdatePropertyRoot
import io.github.stream29.streamlin.getValue
import io.github.stream29.streamlin.setValue
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level as LogbackLevel



val helpLogger = LoggerFactory.getLogger("Help")!!
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

val configFile = File("config.yml")

private fun setActualLogbackLevel(levelToSet: String, origin: String) {
    try {
        val loggerContext = LoggerFactory.getILoggerFactory() as? LoggerContext
        if (loggerContext != null) {
            val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
            val logbackLevel = LogbackLevel.valueOf(levelToSet.uppercase())
            rootLogger.level = logbackLevel
            configLogger.info("Logging level set to $levelToSet (from $origin).")
        } else {
            configLogger.warn("Could not get Logback LoggerContext to set logging level to $levelToSet (from $origin).")
        }
    } catch (e: IllegalArgumentException) {
        configLogger.error("Invalid logging level string '$levelToSet' (from $origin).", e)
    } catch (e: Exception) {
        configLogger.error("Error setting Logback level to $levelToSet (from $origin): ${e.message}", e)
    }
}

fun applyLogLevelFromConfiguration() {
    val configuredLevel: String
    try {
        // Attempt to read the logging level from the initialized config
        configuredLevel = config.logging.level
        configLogger.debug("Read logging level '$configuredLevel' from config. Attempting to apply it.")
    } catch (e: Exception) {
        // This will catch the streamlin error if it still occurs during config.logging.level access,
        // or any other error if Config/LoggingConfig defaults are missing and YAML section is absent.
        configLogger.error(
            "Failed to read logging level from configuration: ${e.message}. Falling back to INFO.", e
        )
        // Fallback to a default level if config access fails
        setActualLogbackLevel("INFO", "fallback-due-to-config-error")
        return
    }

    // If we successfully read from config, use that.
    setActualLogbackLevel(configuredLevel, "configuration")
}


@Suppress("unused")
val unused = {
    if (!configFile.exists()) {
        helpLogger.info("It looks that you are starting the program for the first time here.")
        helpLogger.info("A default config file is created at ${configFile.absolutePath} with schema annotation.")
        configFile.writeText(
            """
# ${'$'}schema: https://github.com/Stream29/ProxyAsLocalModel/raw/master/config_v4.schema.json
lmStudio:
  port: 1234
ollama:
  port: 11434
apiProviders: {}
logging: # Added default logging config
  level: INFO
"""
        )
    }

    // Keep the file watcher logic
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
            if (previousConfig.client != newConfig.client) {
                val previousClient = globalClient
                previousClient.close()
                clientConfigProperty.set(newConfig.client)
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
    configLogger.info("Config file watcher started at ${configFile.absolutePath}")
}()

private val configProperty = AutoUpdatePropertyRoot(
    sync = true,
    mode = AutoUpdateMode.PROPAGATE,
    initValue = configFile.readText().decodeYaml<Config>()
)

var config by configProperty


val clientConfigProperty = AutoUpdatePropertyRoot(
    sync = true,
    mode = AutoUpdateMode.PROPAGATE,
    initValue = config.client
)

@OptIn(DelicateCoroutinesApi::class)
val globalClient by clientConfigProperty.subproperty {
    configLogger.info("Ktor Client created with: $it")
    val client = HttpClient(OkHttp) {
        engine {
            if (it.proxyEnabled) {
                configLogger.info("Ktor Client using socks proxy ${it.proxyHost}:${it.proxyPort}")
                proxy = ProxyBuilder.socks(it.proxyHost, it.proxyPort)
            }
        }
        install(ContentNegotiation) {
            json(globalJson)
        }
        install(HttpTimeout) {
            socketTimeoutMillis = it.socketTimeout
            connectTimeoutMillis = it.connectTimeout
            requestTimeoutMillis = it.requestTimeout
        }
        install(HttpRequestRetry) {
            retryOnException(maxRetries = it.retry)
            constantDelay(it.delayBeforeRetry)
        }
        expectSuccess = true
    }

    if (it.proxyEnabled) {
        GlobalScope.launch {
            try {
                // Checking external IP
                val body: String = client.get("https://api.ipify.org/?format=json").bodyAsText()
                configLogger.info("Public IP via proxy: $body")
            } catch (e: Exception) {
                configLogger.error("Failed to fetch public IP via proxy", e)
            }
        }
    }

    client

}

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
        createLmStudioServer(config).apply { start(wait = false) }
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
        createOllamaServer(it).apply { start(wait = false) }
    } else null
}

inline fun <reified T> String.decodeYaml() = globalYaml.decodeFromString<T>(this)
inline fun <reified T> T.encodeYaml() = globalYaml.encodeToString(this)
inline fun <reified T> T.encodeJson() = globalJson.encodeToString(this)