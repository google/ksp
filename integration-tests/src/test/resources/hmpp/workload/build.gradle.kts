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
    }

    linuxX64() {
        binaries {
            executable()
        }
    }

    sourceSets {
        val commonMain by getting

        val jvmJs by sourceSets.creating {
            dependsOn(commonMain)
        }

        val jvmLinuxX64 by sourceSets.creating {
            dependsOn(commonMain)
        }

        val jvmOnly by sourceSets.creating {
            dependsOn(jvmJs)
            dependsOn(jvmLinuxX64)
        }

        val linuxX64Main by getting {
            dependsOn(jvmLinuxX64)
        }

        val jvmMain by getting {
            dependsOn(jvmOnly)
        }

        val jsMain by getting {
            dependsOn(jvmJs)
        }
    }
}

configurations.configureEach {
    val targetConfigurations = setOf(
        "kspCommonMainMetadata",
        "kspJvmJsMetadata",
        "kspJvmLinuxX64Metadata",
        "kspJvm",
        "kspJs",
        "kspLinuxX64"
    )
    if (name in targetConfigurations) {
        project.dependencies.add(this.name, project.dependencies.project(":test-processor"))
    }
}
