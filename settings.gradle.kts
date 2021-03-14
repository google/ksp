pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
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

val kotlinProjectPath: String? by settings
if (kotlinProjectPath != null) {
    includeBuild(kotlinProjectPath!!) {
        dependencySubstitution {
            substitute(module("org.jetbrains.kotlin:kotlin-compiler")).with(project(":include:kotlin-compiler"))
        }
    }
}
