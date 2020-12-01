pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}

include("api")
include("gradle-plugin")
include("compiler-plugin")
include("symbol-processing")

val kotlinProjectPath: String? by settings
if (kotlinProjectPath != null) {
    includeBuild(kotlinProjectPath!!) {
        dependencySubstitution {
            substitute(module("org.jetbrains.kotlin:kotlin-compiler")).with(project(":include:kotlin-compiler"))
//            substitute(module("org.jetbrains.kotlin:kotlin-compiler-tests")).with(project(":include:kotlin-compiler-tests"))
        }
    }
}
