import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

description = "Kotlin Symbol Processing Util"

val junitVersion: String by project
val kotlinBaseVersion: String by project

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(project(":api"))
    implementation(kotlin("stdlib", kotlinBaseVersion))
    testImplementation("junit:junit:$junitVersion")
}

kotlin {
    compilerOptions {
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
}
