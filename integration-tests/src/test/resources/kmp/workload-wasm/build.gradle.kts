plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = "1.0-SNAPSHOT"

kotlin {
    wasmJs {
        binaries.executable()
        browser()
    }
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":annotations"))
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":test-processor"))
    add("kspWasmJs", project(":test-processor"))
    add("kspWasmJsTest", project(":test-processor"))
}
