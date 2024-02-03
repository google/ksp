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
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":test-processor"))
    ksp(project(":test-processor"))
}

android {
    compileSdkVersion(34)
    defaultConfig {
        minSdkVersion(34)
        targetSdkVersion(34)
    }
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}
