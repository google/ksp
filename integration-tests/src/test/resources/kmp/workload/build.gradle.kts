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
    sourceSets {
        val commonMain by getting {
            dependencies {
                configurations.get("ksp").dependencies.add(project(":test-processor"))
            }
            kotlin.srcDir("src/main/kotlin")
        }
    }
}
