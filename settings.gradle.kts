import dev.chunkzero.gradle.ChunkSettingsExtension

pluginManagement {
    repositories {
        maven("https://maven.chunkzero.com")
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.10" apply false
    id("dev.chunkzero.chunk.settings") version "0.1.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

extensions.configure<ChunkSettingsExtension> {
    javaPackage.set("dev.chunkzero.example.generated")
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.chunkzero.com")
        maven("https://maven.chunkzero.com/snapshots") {
            mavenContent { includeModule("net.minestom", "minestom") }
        }
        mavenCentral()
    }
}

rootProject.name = "full-stack-example"
include(":shared")
