plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = "1.0-SNAPSHOT"

kotlin {
    jvm {
    }
    js(IR) {
        browser()
        nodejs()
    }
    linuxX64() {
    }
    androidNativeX64() {
    }
    androidNativeArm64() {
    }
    // TODO: Enable after CI's Xcode version catches up.
    // iosArm64()
    // macosX64()
    mingwX64()
    sourceSets {
        val commonMain by getting
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-deprecated-legacy-compiler"
}
