pluginManagement {
    val kotlinVersion: String by settings
    val kspVersion: String by settings
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

rootProject.name = "init-plus-provider"

include(":workload")
include(":init-processor")
include(":provider-processor")
