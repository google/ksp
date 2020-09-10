import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Symbol Processor"

group = "org.jetbrains.kotlin"
version = "1.4.0"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.4.0")

    compileOnly(gradleApi())

    testImplementation(gradleApi())
    testImplementation("junit:junit:4.12")
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

