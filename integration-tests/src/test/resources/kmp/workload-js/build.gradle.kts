plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = "1.0-SNAPSHOT"

kotlin {
    js(BOTH) {
        browser()
        nodejs()
    }
    sourceSets {
        val commonMain by getting {
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-deprecated-legacy-compiler"
}
