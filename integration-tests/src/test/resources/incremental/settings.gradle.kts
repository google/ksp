pluginManagement {
    val kspVersion: String by settings
    val kotlinVersion: String by settings
    val testRepo: String by settings
    plugins {
        id("com.google.devtools.ksp") version kspVersion
        kotlin("jvm") version kotlinVersion
    }
    repositories {
        maven(testRepo)
        gradlePluginPortal()
        google()
    }
}

rootProject.name = "incremental-test"

include(":workload")
include(":validator")
