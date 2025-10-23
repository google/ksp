plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = "1.0-SNAPSHOT"

kotlin {
    jvm {
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":test-processor"))
}
