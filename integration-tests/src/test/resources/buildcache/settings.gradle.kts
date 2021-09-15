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
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
    }
}

buildCache {
    val buildCacheDir: String by settings
    local {
        directory = File(buildCacheDir)
        removeUnusedEntriesAfterDays = 30
    }
}

rootProject.name = "playground"

include(":workload")
include(":test-processor")
