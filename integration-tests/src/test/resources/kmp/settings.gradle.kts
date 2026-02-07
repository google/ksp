pluginManagement {
    val kotlinVersion: String by settings
    val kspVersion: String by settings
    val testRepo: String by settings
    val agpVersion: String by settings
    plugins {
        id("com.google.devtools.ksp") version kspVersion apply false
        kotlin("multiplatform") version kotlinVersion apply false
        id("com.android.kotlin.multiplatform.library") version agpVersion apply false
    }
    repositories {
        google()
        maven(testRepo)
        gradlePluginPortal()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
    }
}

rootProject.name = "playground"

include(":annotations")
include(":workload")
include(":workload-android")
include(":workload-jvm")
include(":workload-js")
include(":workload-wasm")
include(":workload-linuxX64")
include(":workload-androidNative")
include(":test-processor")
