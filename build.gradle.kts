plugins {
    kotlin("jvm") version "2.1.20"
    id("org.graalvm.buildtools.native") version "0.10.6"
    id("com.gradleup.shadow") version "8.3.5"
    kotlin("plugin.serialization") version "2.1.20"
}

group = "io.github.stream29"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core:3.1.2")
    implementation("io.ktor:ktor-server-cio:3.1.2")
    implementation("io.ktor:ktor-server-call-logging:3.1.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.1.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.10.2")

    implementation("com.alibaba:dashscope-sdk-java:2.19.4") {
        exclude("org.slf4j", "slf4j-simple")
    }

    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    implementation("ch.qos.logback:logback-classic:1.4.11")

    implementation("io.github.stream29:streamlin:3.1.0")
    implementation("io.github.stream29:json-schema-generator:1.0.2")
    implementation("com.charleskorn.kaml:kaml:0.77.1")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:3.1.2")
}

kotlin {
    jvmToolchain(21)
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("application")
            mainClass.set("io.github.stream29.MainKt")
        }
    }
}

