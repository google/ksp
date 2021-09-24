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
    add("kspMetadata", project(":test-processor"))
    add("kspJvm", project(":test-processor"))
    add("kspJvmTest", project(":test-processor"))
    add("kspJs", project(":test-processor"))
    add("kspJsTest", project(":test-processor"))
    add("kspAndroidNativeX64", project(":test-processor"))
    add("kspAndroidNativeX64Test", project(":test-processor"))
    add("kspAndroidNativeArm64", project(":test-processor"))
    add("kspAndroidNativeArm64Test", project(":test-processor"))
    add("kspLinuxX64", project(":test-processor"))
    add("kspLinuxX64Test", project(":test-processor"))
    add("kspMingwX64", project(":test-processor"))
    add("kspMingwX64Test", project(":test-processor"))
}
