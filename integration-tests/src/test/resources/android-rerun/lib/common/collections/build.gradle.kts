import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.google.devtools.ksp")
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.example.common.collections"

    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        minSdk = 26
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
}
