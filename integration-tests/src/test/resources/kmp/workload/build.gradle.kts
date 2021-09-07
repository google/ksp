plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        withJava()
    }
    js() {
        browser()
        nodejs()
    }
    linuxX64() {
        binaries {
            executable()
        }
    }
    androidNativeX64() {
        binaries {
            executable()
        }
    }
    androidNativeArm64() {
        binaries {
            executable()
        }
    }
    // TODO: Enable after CI's Xcode version catches up.
    // iosArm64()
    // macosX64()
    mingwX64()
    sourceSets {
        val commonMain by getting
        val linuxX64Main by getting
        val linuxX64Test by getting
        val androidNativeX64Main by getting
        val androidNativeArm64Main by getting
    }
}

dependencies {
    ksp(project(":test-processor"))
    add("kspLinuxX64Test", project(":test-processor"))
}
