description = "Kotlin Symbol Processing implementation using Kotlin Analysis API"

val intellijVersion: String by project
val kotlinBaseVersion: String by project
val junitVersion: String by project

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.6.4"
    id("org.jetbrains.dokka") version ("1.4.32")
}

intellij {
    version = intellijVersion
}

fun ModuleDependency.includeJars(vararg names: String) {
    names.forEach {
        artifact {
            name = it
            type = "jar"
            extension = "jar"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib", kotlinBaseVersion))
    implementation("org.jetbrains.kotlin:high-level-api-for-ide:1.6.255")
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")

    implementation(project(":api"))
}
repositories {
    flatDir {
        dirs("${project.rootDir}/third_party/prebuilt/repo/")
    }
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
}
