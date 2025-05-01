plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.graalvm.buildtools)
    alias(libs.plugins.gradleup.shadow)
    alias(libs.plugins.kotlin.serialization)
}

group = "io.github.stream29"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)

    implementation(libs.ktor.client.okhttp)
    implementation(libs.openai.client)
    
    implementation(libs.kotlinx.coroutines.rx2)
    
    implementation(libs.dashscope.sdk.java) {
        exclude("org.slf4j", "slf4j-simple")
    }
    
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.logback.classic)
    
    implementation(libs.streamlin)
    implementation(libs.json.schema.generator)
    implementation(libs.kaml)
    
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("Proxy")
            mainClass.set("io.github.stream29.proxy.MainKt")
        }
    }
}

