plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = "1.0-SNAPSHOT"

kotlin {
    js(IR) {
        browser()
        nodejs()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":annotations"))
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":test-processor"))
    add("kspJs", project(":test-processor"))
    add("kspJsTest", project(":test-processor"))
}
