pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "symbol-processing" ->
                    useModule("com.google.devtools.ksp:symbol-processing:${requested.version}")
            }
        }
    }
    repositories {
        gradlePluginPortal()
        google()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

include("api")
include("gradle-plugin")
include("compiler-plugin")
include("symbol-processing")
include("sample:processor")
include("sample:test")

val kotlinProjectPath: String? by settings
if (kotlinProjectPath != null) {
    includeBuild(kotlinProjectPath!!) {
        dependencySubstitution {
            substitute(module("org.jetbrains.kotlin:kotlin-compiler")).with(project(":include:kotlin-compiler"))
//            substitute(module("org.jetbrains.kotlin:kotlin-compiler-tests")).with(project(":include:kotlin-compiler-tests"))
        }
    }
}