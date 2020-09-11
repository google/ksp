import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Symbol Processor"

val kotlinBaseVersion: String? by project
val junitVersion: String? by project

group = "org.jetbrains.kotlin"
version = kotlinBaseVersion

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinBaseVersion")

    compileOnly(gradleApi())

    testImplementation(gradleApi())
    testImplementation("junit:junit:$junitVersion")
}

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = "ksp-gradle-plugin"
            from(components["java"])
        }
        repositories {
            mavenLocal()
        }
    }
}

