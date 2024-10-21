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
        val commonMain by getting {
            dependencies {
                implementation("me.tatarka.inject:kotlin-inject-runtime:0.7.2")
            }
        }

        val jvmMain by getting {
        }
    }
}

dependencies {
    add("kspJvm", "me.tatarka.inject:kotlin-inject-compiler-ksp:0.7.2")
}
