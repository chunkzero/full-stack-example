import java.security.MessageDigest

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.chunkzero.chunk")
}

kotlin {
    jvmToolchain(25)
    compilerOptions { allWarningsAsErrors = true }
}

dependencies {
    api(libs.chunk.multistom.kotlin)
    api(libs.window.runtime)
    api(libs.window.multistom)
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
