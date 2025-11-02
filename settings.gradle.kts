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
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
        maven("https://www.jetbrains.com/intellij-repository/snapshots")
        google()
    }
}

plugins {
    id("com.gradle.develocity") version "4.3"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
        val isCI = providers.environmentVariable("CI").isPresent
        publishing.onlyIf { isCI }
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
