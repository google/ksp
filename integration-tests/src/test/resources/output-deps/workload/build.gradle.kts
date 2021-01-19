val testRepo: String by project

plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    ksp(project(":test-processor"))
}

