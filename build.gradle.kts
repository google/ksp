plugins {
    kotlin("jvm") version "1.4.0" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}