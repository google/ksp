pluginManagement {
    val kotlinVersion: String by settings
    val kspVersion: String by settings
    val testRepo: String by settings
    plugins {
        id("com.google.devtools.ksp") version kspVersion apply false
        kotlin("multiplatform") version kotlinVersion apply false
    }
    repositories {
        maven(testRepo)
        gradlePluginPortal()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
    }
}

rootProject.name = "playground"

include(":annotations")
include(":workload")
include(":workload-jvm")
include(":workload-js")
include(":workload-wasm")
include(":workload-linuxX64")
include(":workload-androidNative")
include(":test-processor")
