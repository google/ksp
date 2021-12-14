pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
        maven("https://www.jetbrains.com/intellij-repository/snapshots")
    }
    val kotlinBaseVersion: String by settings
    resolutionStrategy {
        eachPlugin {
            if ( requested.id.id == "org.jetbrains.kotlin.jvm" ) {
                useModule( "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinBaseVersion" )
            }
        }
    }
}

include("api")
include("gradle-plugin")
include("compiler-plugin")
include("symbol-processing")
include("integration-tests")
include("util")

val kotlinProjectPath: String? by settings
if (kotlinProjectPath != null) {
    includeBuild(kotlinProjectPath!!) {
        dependencySubstitution {
            substitute(module("org.jetbrains.kotlin:kotlin-compiler")).with(project(":include:kotlin-compiler"))
        }
    }
}
