import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Ksp - Symbol processing for Kotlin"

val kotlinBaseVersion: String by project

group = "org.jetbrains.kotlin"
version = kotlinBaseVersion

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "6.0.0"
    `maven-publish`
}

val packedJars by configurations.creating

dependencies {
    packedJars(project(":gradle-plugin")) { isTransitive = false }
    packedJars(project(":compiler-plugin")) { isTransitive = false }
    packedJars(project(":api")) { isTransitive = false }
}

tasks.withType<ShadowJar>() {
    classifier = ""
    from(packedJars)
    relocate("com.intellij", "org.jetbrains.kotlin.com.intellij")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

publishing {
    publications {
        val publication = create<MavenPublication>("shadow") {
            artifactId = "symbol-processing"
        }
        project.shadow.component(publication)
        repositories {
            mavenLocal()
        }
    }
}
