import dev.chunkzero.gradle.ChunkSettingsExtension

// Chunk and Window are built from local checkouts until their nightlies are published; see `just local-deps`.
pluginManagement {
    repositories {
        mavenLocal { content { includeGroupByRegex("dev\\.chunkzero(\\..*)?") } }
        maven("https://maven.chunkzero.com")
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
    id("dev.chunkzero.chunk.settings") version "0.1.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

extensions.configure<ChunkSettingsExtension> {
    javaPackage.set("dev.chunkzero.example.generated")
}

dependencyResolutionManagement {
    repositories {
        mavenLocal { content { includeGroupByRegex("dev\\.(chunkzero|oglass\\.window)(\\..*)?") } }
        maven("https://maven.chunkzero.com")
        maven("https://maven.chunkzero.com/snapshots") {
            mavenContent { includeModule("net.minestom", "minestom") }
        }
        mavenCentral()
    }
}

rootProject.name = "full-stack-example"
include(":shared")
