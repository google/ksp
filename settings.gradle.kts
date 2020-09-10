pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

include("api")
include("gradle-plugin")
include("compiler-plugin")

//val kotlinProjectPath: String? by settings
val kotlinProjectPath = "/usr/local/google/home/laszio/working/kotlin"
if (kotlinProjectPath != null) {
    includeBuild(kotlinProjectPath!!) {
        dependencySubstitution {
            substitute(module("org.jetbrains.kotlin:kotlin-compiler")).with(project(":include:kotlin-compiler"))
            substitute(module("org.jetbrains.kotlin:kotlin-compiler-tests")).with(project(":include:kotlin-compiler-tests"))
            substitute(module("org.jetbrains.kotlin:kotlin-scripting-compiler")).with(project(":kotlin-scripting-compiler"))
//            substitute(module("org.jetbrains.kotlin:kotlin-compiler-test-instrumenter")).with(project(":include:kotlin-compiler-test-instrumenter"))
        }
    }
}
