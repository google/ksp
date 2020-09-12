import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Symbol Processing API"

val kotlinBaseVersion: String by project
val kspVersion: String? by project

group = "com.google.devtools.kotlin"
version = kspVersion ?: kotlinBaseVersion

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

plugins {
    kotlin("jvm")
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = "symbol-processing-api"
            from(components["java"])
        }
        repositories {
            mavenLocal()
        }
    }
}

