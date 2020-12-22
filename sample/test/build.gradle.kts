import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinBaseVersion: String by project
val junitVersion: String by project

plugins {
    kotlin("jvm")
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