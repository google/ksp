import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all-compatibility")
}
plugins {
    kotlin("jvm")
}

version = "2.0.255-SNAPSHOT"

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

dependencies {
    implementation(project(":api"))
    implementation(kotlin("reflect"))
}
