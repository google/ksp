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
        binaries {
            executable()
        }
    }
    // requires Android SDK
    androidNativeX64() {
        binaries {
            executable()
        }
    }
    // requires Android SDK
    androidNativeArm64() {
        binaries {
            executable()
        }
    }
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
//    add("kspCommonMainMetadata", project(":processors"))
    add("kspJvm", project(":processors"))
    add("kspJvmTest", project(":processors"))
    add("kspJs", project(":processors"))
    add("kspJsTest", project(":processors"))
    add("kspAndroidNativeX64", project(":processors"))
    add("kspAndroidNativeX64Test", project(":processors"))
    add("kspAndroidNativeArm64", project(":processors"))
    add("kspAndroidNativeArm64Test", project(":processors"))
    add("kspLinuxX64", project(":processors"))
//    add("kspLinuxX64Test", project(":processors"))
    add("kspMingwX64", project(":processors"))
//    add("kspMingwX64Test", project(":processors"))

    // The universal "ksp" configuration has performance issue and is deprecated on multiplatform since 1.0.1
    // ksp(project(":processors"))
}
