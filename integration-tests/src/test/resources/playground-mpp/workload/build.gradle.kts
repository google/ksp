plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        withJava()
    }
    linuxX64()
    mingwX64()
    macosX64()
    ios()
    js(IR) {
        browser()
        nodejs()
    }
    sourceSets {
        val commonMain by getting
        val jvmMain by getting {
            dependencies {
                implementation(project(":test-processor"))
                project.dependencies.add("kspJvm", project(":test-processor"))
            }
            kotlin.srcDir("src/main/java")
        }
    }
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}
