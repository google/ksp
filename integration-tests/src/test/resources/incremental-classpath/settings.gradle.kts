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
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
    }
}

rootProject.name = "incremental-test"

include(":workload")
include(":validator")
include(":l1")
include(":l2")
include(":l3")
include(":l4")
include(":l5")
