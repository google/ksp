import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

evaluationDependsOn(":api")

description = "Kotlin Symbol Processing Util"

val kotlinBaseVersion: String by project
val intellijVersion: String by project
val junitVersion: String by project

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all-compatibility")
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    listOf(
        "com.jetbrains.intellij.platform:util-rt",
        "com.jetbrains.intellij.platform:util-class-loader",
        "com.jetbrains.intellij.platform:util-text-matching",
        "com.jetbrains.intellij.platform:util",
        "com.jetbrains.intellij.platform:util-base",
        "com.jetbrains.intellij.platform:util-xml-dom",
        "com.jetbrains.intellij.platform:core",
        "com.jetbrains.intellij.platform:core-impl",
        "com.jetbrains.intellij.platform:extensions",
        "com.jetbrains.intellij.java:java-psi",
        "com.jetbrains.intellij.java:java-psi-impl",
    ).forEach {
        implementation("$it:$intellijVersion") { isTransitive = false }
    }

    implementation(kotlin("stdlib", kotlinBaseVersion))

    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")

    implementation(project(":api"))
    testImplementation("junit:junit:$junitVersion")
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
