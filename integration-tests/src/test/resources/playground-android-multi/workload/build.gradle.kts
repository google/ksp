val testRepo: String by project

plugins {
    // DO NOT CHANGE THE ORDER.
    id("com.google.devtools.ksp")
    id("com.android.library")
    kotlin("android")
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    mavenCentral()
    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":test-processor"))
    ksp(project(":test-processor"))
}

android {
    namespace = "com.example.mylibrary"
    compileSdk = 34
    defaultConfig {
        minSdk = 34
        targetSdk = 34
    }
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}
