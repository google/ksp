pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
        maven("https://www.jetbrains.com/intellij-repository/snapshots")
    }
    val kotlinBaseVersion: String by settings
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.jvm") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinBaseVersion")
            }
        }
    }
}

include("api")
include("gradle-plugin")
include("common-util")
include("test-utils")
include("compiler-plugin")
include("symbol-processing")
include("symbol-processing-cmdline")
include("integration-tests")

val kotlinProjectPath: String? by settings
if (kotlinProjectPath != null) {
    includeBuild(kotlinProjectPath!!) {
        dependencySubstitution {
            substitute(module("org.jetbrains.kotlin:kotlin-compiler")).using(project(":include:kotlin-compiler"))
        }
    }
}
