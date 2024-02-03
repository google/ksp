val testRepo: String by project

plugins {
    id("com.android.application")
    kotlin("android")
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":workload"))
}

android {
    compileSdkVersion(34)
    defaultConfig {
        applicationId = "org.gradle.kotlin.dsl.samples.androidstudio"
        minSdkVersion(34)
        targetSdkVersion(34)
        versionCode = 1
        versionName = "1.0"
    }
}
