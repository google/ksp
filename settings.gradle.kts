rootProject.name = "ksp"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
        maven("https://www.jetbrains.com/intellij-repository/snapshots")
        google()
    }
}

include("api")
include("gradle-plugin")
include("common-deps")
include("common-util")
include("test-utils")
include("symbol-processing")
include("integration-tests")
include("kotlin-analysis-api")
include("symbol-processing-aa-embeddable")
include("cmdline-parser-gen")
