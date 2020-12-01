import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Symbol Processor"

val kotlinBaseVersion: String by project
val junitVersion: String by project

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinBaseVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinBaseVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")

    compileOnly(gradleApi())

    testImplementation(gradleApi())
    testImplementation("junit:junit:$junitVersion")
}

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
}
