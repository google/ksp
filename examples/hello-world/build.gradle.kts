plugins {
    kotlin("jvm") version "2.3.0" apply false
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "2.3.0"))
    }
}

group = "com.example"
version = "1.0-SNAPSHOT"

tasks.register<GradleBuild>("run") {
    tasks.add(":app:run")
}
