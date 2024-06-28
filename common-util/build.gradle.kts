import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

evaluationDependsOn(":api")

description = "Kotlin Symbol Processing Util"

val junitVersion: String by project
val kotlinBaseVersion: String by project

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all-compatibility")
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(project(":api"))
    implementation(kotlin("stdlib", kotlinBaseVersion))
    testImplementation("junit:junit:$junitVersion")
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
