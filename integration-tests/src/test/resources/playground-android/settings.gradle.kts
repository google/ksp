pluginManagement {
    val kotlinVersion: String by settings
    val kspVersion: String by settings
    val testRepo: String by settings
    val agpVersion: String by settings
    plugins {
        id("com.google.devtools.ksp") version kspVersion apply false
        kotlin("jvm") version kotlinVersion apply false
        kotlin("android") version kotlinVersion apply false
        id("com.android.application") version agpVersion apply false
    }
    repositories {
        maven(testRepo)
        gradlePluginPortal()
        google()
        jcenter()
        mavenCentral()
    }
}

rootProject.name = "playground"

include(":workload")
include(":test-processor")
