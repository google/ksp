plugins {
    kotlin("jvm")
}

version = rootProject.extra.get("kspVersion") as String

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

dependencies {
    implementation(project(":api"))
    implementation(kotlin("reflect"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }
}
