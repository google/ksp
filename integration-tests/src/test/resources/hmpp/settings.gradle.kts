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
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
    }
}

rootProject.name = "hmpp"

include(":workload")
include(":test-processor")
