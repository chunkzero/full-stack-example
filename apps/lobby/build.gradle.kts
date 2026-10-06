plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.chunkzero.chunk")
}

kotlin {
    jvmToolchain(25)
    compilerOptions { allWarningsAsErrors = true }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.chunk.multistom.kotlin)
    implementation(libs.window.runtime)
    implementation(libs.slf4j.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test { useJUnitPlatform() }

application { mainClass = "com.chunkzero.example.lobby.LobbySessionsKt" }
