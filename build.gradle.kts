plugins {
    kotlin("jvm") version "1.4.0" apply false
}

group = "org.jetbrains.kotlin"
version = "1.4.0"

subprojects {
    repositories {
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}