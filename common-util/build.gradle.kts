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
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
}
