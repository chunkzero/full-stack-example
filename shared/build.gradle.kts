import java.security.MessageDigest

plugins {
    id("com.chunkzero.chunk.kotlin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }
kotlin { compilerOptions { allWarningsAsErrors = true } }

dependencies {
    implementation(libs.window.runtime)
    implementation(libs.polar)
    implementation(libs.slf4j.api)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test { useJUnitPlatform() }

val prepareResourcePack =
    tasks.register("prepareResourcePack") {
        val pack = rootProject.layout.projectDirectory.file("assets/full-stack-example.zip")
        val output = layout.buildDirectory.dir("generated/resourcePack")
        inputs.file(pack)
        outputs.dir(output)
        doLast {
            val sha1 = MessageDigest.getInstance("SHA-1").digest(pack.asFile.readBytes())
            output.get().file("pack.sha1").asFile.apply {
                parentFile.mkdirs()
                writeText(sha1.joinToString("") { "%02x".format(it) } + "\n")
            }
        }
    }
sourceSets.main { resources.srcDir(prepareResourcePack) }
