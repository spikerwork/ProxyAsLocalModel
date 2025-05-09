import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    // Kotlin & Java
    alias(libs.plugins.kotlin.jvm)
    java

    // Shadow (fat-jar) + GraalVM + Serialization
    alias(libs.plugins.gradleup.shadow)
    alias(libs.plugins.graalvm.buildtools)
    alias(libs.plugins.kotlin.serialization)

    application
}

group = "io.github.stream29"
version = "0.0.9"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)

    // Ktor client
    implementation(libs.ktor.client)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)

    // Serialization, logging, etc.
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

application {
    mainClass.set("io.github.stream29.proxy.MainKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("Proxy")
            mainClass.set("io.github.stream29.proxy.MainKt")
            buildArgs("--enable-native-access=ALL-UNNAMED")
        }
    }
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("ProxyAsLocalModel")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")      // drop the "-all" suffix
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

