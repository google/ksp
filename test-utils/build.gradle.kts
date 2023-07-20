import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinBaseVersion: String by project
val intellijVersion: String by project
val junit5Version: String by project

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all-compatibility")
}
plugins {
    kotlin("jvm")
}

version = "2.0.255-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")
    implementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinBaseVersion")

    implementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")

    implementation(project(":api"))
    implementation(project(":compiler-plugin"))
}
