rootProject.name = "ksp"

pluginManagement {
    val buildKotlinVersion: String by settings
    val buildKspVersion: String by settings

    plugins {
        kotlin("jvm") version buildKotlinVersion apply false
        id("com.google.devtools.ksp") version buildKspVersion apply false
    }

    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
        maven("https://www.jetbrains.com/intellij-repository/snapshots")
    }
}

include("api")
include("gradle-plugin")
include("common-deps")
include("common-util")
include("test-utils")
include("compiler-plugin")
include("symbol-processing")
include("symbol-processing-cmdline")
include("integration-tests")
include("kotlin-analysis-api")
include("symbol-processing-aa-embeddable")
include("cmdline-parser-gen")
