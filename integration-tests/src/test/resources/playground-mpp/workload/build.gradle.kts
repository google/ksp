plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        val commonMain by getting
        val jvmMain by getting {
            dependencies {
                implementation(project(":test-processor"))
                configurations.get("ksp").dependencies.add(project(":test-processor"))
            }
            kotlin.srcDir("src/main/java")
        }
    }
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}