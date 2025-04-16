val testRepo: String by project

plugins {
    id("com.android.application")
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
    implementation(project(":workload"))
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34
    defaultConfig {
        applicationId = "org.gradle.kotlin.dsl.samples.androidstudio"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
