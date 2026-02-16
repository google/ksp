pluginManagement {
    val kotlinVersion: String by settings
    val kspVersion: String by settings
    val testRepo: String by settings
    val agpVersion: String by settings
    plugins {
        id("com.google.devtools.ksp") version kspVersion apply false
        kotlin("android") version kotlinVersion apply false
        id("com.android.library") version agpVersion apply false
    }
    repositories {
        maven(testRepo)
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
    }
}

include(":lib:common:collections")
include(":lib:common:media")
