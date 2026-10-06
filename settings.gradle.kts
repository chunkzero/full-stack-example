import com.chunkzero.chunk.gradle.ChunkSettingsExtension

pluginManagement {
    repositories {
        maven("https://maven.chunkzero.com/nightlies") {
            mavenContent { includeGroupAndSubgroups("com.chunkzero") }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
    id("com.chunkzero.chunk.settings") version "0.1.0-nightly.20261006035928.gb1df764164b0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

extensions.configure<ChunkSettingsExtension> {
    javaPackage.set("com.chunkzero.example.generated")
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.chunkzero.com/nightlies") {
            mavenContent { includeGroupAndSubgroups("com.chunkzero") }
        }
        mavenCentral()
    }
}

rootProject.name = "full-stack-example"
include(":shared")
