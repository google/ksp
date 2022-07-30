@file:Suppress("UNUSED_VARIABLE")

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
    application
}

version = "1.0-SNAPSHOT"

application {
    mainClass.set("MainKt")
}

kotlin {
    jvm {
        withJava()
    }

    js(IR) {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            ksp {
                processor(project(":test-processor"))
                arg("a", "a_commonMain")
                arg("c", "c_commonMain")
            }
            dependencies {
                implementation(project(":annotations"))
            }
        }

        val clientMain by creating {
            ksp {
                arg("a", "a_clientMain")
                arg("d", "d_clientMain")
                inheritable = false
            }
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(clientMain)
        }

        val jsMain by getting {
            dependsOn(clientMain)
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmTest by getting {
            ksp {
                processor(project(":test-processor"))
            }
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
            }
        }
    }
}

ksp {
    arg("a", "a_global")
    arg("b", "b_global")
}

tasks {
    val jvmTest by getting(Test::class) {
        useJUnitPlatform()
        // Show stdout/stderr and stack traces on console – https://stackoverflow.com/q/65573633/2529022
        testLogging {
            events("PASSED", "FAILED", "SKIPPED")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
            showStackTraces = true
        }
    }

    val jsNodeTest by getting(org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest::class) {
        // Show stdout/stderr and stack traces on console – https://stackoverflow.com/q/65573633/2529022
        testLogging {
            events("PASSED", "FAILED", "SKIPPED")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
            showStackTraces = true
        }
    }

    val showMe by registering {
        doLast {
            fun Any.asText() = when (this) {
                is Task -> name
                is KotlinSourceSet -> name
                is TaskProvider<*> -> get().name
                else -> toString()
            }.let {
                "`$it`"
            }

            fun Iterable<Any>.asStableText(transformed: String.() -> String? = { this }) =
                mapNotNull { it.asText().transformed() }.sorted().joinToString()

            val prefix = "[showMe] "
            fun log(message: String = "") = println(message.lines().joinToString("\n") { "$prefix$it" })

            log("\nKotlin targets/compilations/allKotlinSourceSets:\n")
            kotlin.targets.forEach { target ->
                log("* target `${target.targetName}`")
                target.compilations.forEach { compilation ->
                    val commonMark =
                        if (compilation is KotlinCommonCompilation) " [common]" else ""
                    log(
                        "  * compilation `${compilation.name}`$commonMark," +
                            " default sourceSet: `${compilation.defaultSourceSet.name}`"
                    )
                    compilation.allKotlinSourceSets.forEach {
                        val dependencies =
                            if (it.dependsOn.isEmpty()) "" else ", depends on ${it.dependsOn.asStableText()}"
                        log("    * sourceSet `${it.name}`$dependencies")
                    }
                }
            }

            fun KotlinSourceSet.allDependencies(): List<KotlinSourceSet> =
                if (dependsOn.isEmpty()) {
                    listOf(this)
                } else {
                    listOf(this) + dependsOn.flatMap { it.allDependencies() }
                }

            log("\nKotlin targets/compilations/bottomUpSourceSets:\n")
            kotlin.targets.forEach { target ->
                log("* target `${target.targetName}`")
                target.compilations.forEach { compilation ->
                    val commonMark =
                        if (compilation is KotlinCommonCompilation) " [common]" else ""
                    log(
                        "  * compilation `${compilation.name}`$commonMark," +
                            " ordered source sets: " +
                            compilation.defaultSourceSet.allDependencies().joinToString { "`${it.name}`" }
                    )
                }
            }

            log("\nKSP configurations:\n")
            project.configurations.forEach { config ->
                if (config.name.startsWith("ksp")) {
                    log(
                        "* `${config.name}`, artifacts: ${config.allArtifacts.map { it.name }}," +
                            " dependencies: ${config.dependencies.map { it.name }}"
                    )
                }
            }

            val selection: List<String>? = listOf("compile", "ksp")
            log("\nTasks ${selection ?: "(all)"} and their ksp/compile dependencies:\n")
            project.tasks.forEach { task ->
                if (selection == null || selection.any { task.name.startsWith(it) }) {
                    log(
                        "* `${task.name}` depends on [${
                        task.dependsOn.asStableText {
                            when {
                                "ksp" in this -> Regex("""[^\w](ksp\w+)""").find(this)?.groupValues?.get(1)
                                startsWith("`compile") -> this
                                else -> null
                            }
                        }
                        }]"
                    )
                }
            }
            log()
        }
    }
}
