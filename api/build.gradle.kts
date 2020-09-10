import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.pill.PillExtension
import org.jetbrains.dokka.gradle.DokkaTask

description = "Kotlin Symbol Processing API"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.dokka")
}

pill {
    variant = PillExtension.Variant.FULL
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

tasks {
    withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xallow-kotlin-package",
            "-module-name", project.name
        )
    }

    named<DokkaTask>("dokka") {
        outputFormat = "html"
        includes = listOf("$projectDir/ReadMe.md")
    }
}

sourceSets {
    "main" {
        projectDefault()
    }
}

dependencies {
    if (hasProperty("kspBaseVersion")) {
        val kspBaseVersion = properties["kspBaseVersion"] as String
        implementation(kotlin("stdlib", kspBaseVersion))
    } else {
        implementation(kotlinStdlib())
    }
}

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

publish()

sourcesJar()
javadocJar()
runtimeJar()
