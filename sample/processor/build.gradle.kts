import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinBaseVersion: String by project
val junitVersion: String by project

plugins {
    kotlin("jvm")
    id("symbol-processing") version "1.4.20-dev-experimental-20201204"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

dependencies {
    implementation(kotlin("stdlib", kotlinBaseVersion))
    implementation(project(":api"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")

    testImplementation("junit:junit:$junitVersion")
}