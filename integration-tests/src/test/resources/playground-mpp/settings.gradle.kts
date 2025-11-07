pluginManagement {
    val kotlinVersion: String by settings
    val kspVersion: String by settings
    val agpVersion: String by settings
    val testRepo: String by settings
    plugins {
        id("com.google.devtools.ksp") version kspVersion apply false
        kotlin("multiplatform") version kotlinVersion apply false
        id("com.android.kotlin.multiplatform.library") version agpVersion apply false
    }
    repositories {
        maven(testRepo)
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
    }
}

rootProject.name = "playground"

include(":workload")
include(":test-processor")
