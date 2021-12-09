plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        withJava()
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
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":annotations"))
            }
        }
        val androidNativeX64Main by getting
        val androidNativeArm64Main by getting
    }
}

dependencies {
    add("kspMetadata", project(":test-processor"))
    add("kspJvm", project(":test-processor"))
    add("kspJvmTest", project(":test-processor"))
    add("kspAndroidNativeX64", project(":test-processor"))
    add("kspAndroidNativeX64Test", project(":test-processor"))
    add("kspAndroidNativeArm64", project(":test-processor"))
    add("kspAndroidNativeArm64Test", project(":test-processor"))
}
