package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.*

class KMPImplementedIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "kmp")
        project.setup()
    }

    private fun verify(jarName: String, contents: List<String>) {
        val artifact = File(project.root, jarName)
        Assertions.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            contents.forEach {
                Assertions.assertTrue(jarFile.getEntry(it).size > 0)
            }
        }
    }

    private fun verifyKexe(path: String) {
        val artifact = File(project.root, path)
        Assertions.assertTrue(artifact.exists())
        Assertions.assertTrue(artifact.readBytes().isNotEmpty())
    }

    private fun checkExecutionOptimizations(log: String) {
        Assertions.assertFalse(
            log.contains("Execution optimizations have been disabled"),
            "Execution optimizations have been disabled"
        )
    }

    @Test
    fun testAndroid() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-android:build"
        ).build().let {
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-android:build")?.outcome)
            verify(
                "workload-android/build/intermediates/compile_library_classes_jar/androidMain/" +
                    "bundleAndroidMainClassesToCompileJar/classes.jar",
                listOf(
                    "com/example/Foo.class",
                    "com/example/Bar.class",
                    "com/example/Baz.class",
                    "com/example/ToBeValidated.class"
                )
            )
            Assertions.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assertions.assertTrue(it.output.contains("w: [ksp] platforms: [JVM"))
            Assertions.assertTrue(it.output.contains("w: [ksp] List has superTypes: true"))
            checkExecutionOptimizations(it.output)
        }
    }

    @Test
    fun testJvm() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-jvm:build"
        ).build().let {
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-jvm:build")?.outcome)
            verify(
                "workload-jvm/build/libs/workload-jvm-jvm-1.0-SNAPSHOT.jar",
                listOf(
                    "com/example/Foo.class"
                )
            )
            Assertions.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assertions.assertTrue(it.output.contains("w: [ksp] platforms: [JVM"))
            Assertions.assertTrue(it.output.contains("w: [ksp] List has superTypes: true"))
            checkExecutionOptimizations(it.output)
        }
    }

    @Test
    fun testJvmErrorLog() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload-jvm/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"process\") }\n")
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-jvm:build"
        ).buildAndFail().let {
            val errors = it.output.lines().filter { it.startsWith("e: [ksp]") }
            Assertions.assertEquals("e: [ksp] java.lang.Exception: Test Exception in process", errors.first())
        }
        project.restore("workload-jvm/build.gradle.kts")
    }

    @Test
    fun testJs() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-js:build"
        ).build().let {
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-js:build")?.outcome)
            verify(
                "workload-js/build/libs/workload-js-js-1.0-SNAPSHOT.klib",
                listOf(
                    "default/ir/types.knt"
                )
            )
            Assertions.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assertions.assertTrue(it.output.contains("w: [ksp] platforms: [JS"))
            Assertions.assertTrue(it.output.contains("w: [ksp] List has superTypes: true"))
            checkExecutionOptimizations(it.output)
        }
    }

    @Test
    fun triggerException() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val path = "workload/src/commonMain/kotlin/com/example/FooBar.kt"
        val file = File(project.root, path)

        fun setup(shouldFail: Boolean) {
            project.restore(path)

            // Add an annotation that'll will make the processor trigger an exception.
            if (shouldFail) {
                file.writeText(
                    file.readText()
                        .replace("//@TriggerExceptionAnnotation", "@TriggerExceptionAnnotation")
                )
            }
        }

        // Start the kotlin daemon?
        setup(shouldFail = false)
        gradleRunner.withArguments("compileKotlinJvm").build()

        // Make the processor fail
        setup(shouldFail = true)
        gradleRunner.withArguments("compileKotlinJvm").buildAndFail()

        // Should not trigger the caching issue
        setup(shouldFail = false)
        gradleRunner.withArguments("compileKotlinJvm").build()
    }

    @Test
    fun testWasm() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-wasm:build"
        ).build().let {
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-wasm:build")?.outcome)
            verify(
                "workload-wasm/build/libs/workload-wasm-wasm-js-1.0-SNAPSHOT.klib",
                listOf(
                    "default/ir/types.knt"
                )
            )
            Assertions.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assertions.assertTrue(it.output.contains("w: [ksp] platforms: [wasm-js"))
            Assertions.assertTrue(it.output.contains("w: [ksp] List has superTypes: true"))
            checkExecutionOptimizations(it.output)
        }
    }

    @Test
    fun testDefaultArgumentsImpl() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val newSrc = File(project.root, "workload-wasm/src/wasmJsMain/kotlin/com/example/AnnoOnProperty.kt")
        newSrc.appendText(
            """
@Target(AnnotationTarget.PROPERTY)
annotation class OnProperty

class AnnoOnProperty {
    @OnProperty
    val value: Int = 0
}
            """.trimIndent()
        )

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-wasm:build"
        ).build()
    }

    @Test
    fun testJsErrorLog() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload-js/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"process\") }\n")
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-js:build"
        ).buildAndFail().let {
            val errors = it.output.lines().filter { it.startsWith("e: [ksp]") }
            Assertions.assertEquals("e: [ksp] java.lang.Exception: Test Exception in process", errors.first())
        }
        project.restore("workload-js/build.gradle.kts")
    }

    @Test
    fun testJsFailWarning() {
        File(project.root, "workload-js/build.gradle.kts")
            .appendText("\nksp {\n  allWarningsAsErrors = true\n}\n")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-js:build"
        ).buildAndFail()
    }

    @Test
    fun testAndroidNative() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-androidNative:build"
        ).build().let {
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-androidNative:build")?.outcome)
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeX64/debugShared/libworkload_androidNative.so"
            )
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeX64/releaseShared/libworkload_androidNative.so"
            )
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeArm64/debugShared/libworkload_androidNative.so"
            )
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeArm64/releaseShared/libworkload_androidNative.so"
            )
            Assertions.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assertions.assertTrue(it.output.contains("w: [ksp] platforms: [Native"))
            checkExecutionOptimizations(it.output)
        }
    }

    @Test
    fun testLinuxX64() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val genDir = File(project.root, "workload-linuxX64/build/generated/ksp/linuxX64/linuxX64Main/kotlin")

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-linuxX64:build"
        ).build().let {
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-linuxX64:build")?.outcome)
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-linuxX64:kspTestKotlinLinuxX64")?.outcome)
            verifyKexe("workload-linuxX64/build/bin/linuxX64/debugExecutable/workload-linuxX64.kexe")
            verifyKexe("workload-linuxX64/build/bin/linuxX64/releaseExecutable/workload-linuxX64.kexe")

            // TODO: Enable after CI's Xcode version catches up.
            // Assertions.assertTrue(
            //     result.task(":workload-linuxX64:kspKotlinIosArm64")?.outcome == TaskOutcome.SUCCESS ||
            //         result.task(":workload-linuxX64:kspKotlinIosArm64")?.outcome == TaskOutcome.SKIPPED
            // )
            // Assertions.assertTrue(
            //     result.task(":workload-linuxX64:kspKotlinMacosX64")?.outcome == TaskOutcome.SUCCESS ||
            //         result.task(":workload-linuxX64:kspKotlinMacosX64")?.outcome == TaskOutcome.SKIPPED
            // )
            Assertions.assertTrue(
                it.task(":workload-linuxX64:kspKotlinMingwX64")?.outcome == TaskOutcome.SUCCESS ||
                    it.task(":workload-linuxX64:kspKotlinMingwX64")?.outcome == TaskOutcome.SKIPPED
            )
            Assertions.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assertions.assertTrue(it.output.contains("w: [ksp] platforms: [Native"))
            Assertions.assertTrue(it.output.contains("w: [ksp] List has superTypes: true"))
            Assertions.assertTrue(File(genDir, "Main_dot_kt.kt").exists())
            Assertions.assertTrue(File(genDir, "ToBeRemoved_dot_kt.kt").exists())
            checkExecutionOptimizations(it.output)
        }

        File(project.root, "workload-linuxX64/src/linuxX64Main/kotlin/ToBeRemoved.kt").delete()
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            ":workload-linuxX64:build"
        ).build().let {
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-linuxX64:build")?.outcome)
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-linuxX64:kspTestKotlinLinuxX64")?.outcome)
            verifyKexe("workload-linuxX64/build/bin/linuxX64/debugExecutable/workload-linuxX64.kexe")
            verifyKexe("workload-linuxX64/build/bin/linuxX64/releaseExecutable/workload-linuxX64.kexe")
            Assertions.assertTrue(File(genDir, "Main_dot_kt.kt").exists())
            Assertions.assertFalse(File(genDir, "ToBeRemoved_dot_kt.kt").exists())
            checkExecutionOptimizations(it.output)
        }
    }

    @Disabled
    @Test
    fun testNonEmbeddableArtifact() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "-Pkotlin.native.useEmbeddableCompilerJar=false",
            ":workload-linuxX64:kspTestKotlinLinuxX64"
        ).build()

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "-Pkotlin.native.useEmbeddableCompilerJar=true",
            ":workload-linuxX64:kspTestKotlinLinuxX64"
        ).build()

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            ":workload-linuxX64:kspTestKotlinLinuxX64"
        ).build()
    }

    @Test
    fun testLinuxX64ErrorLog() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload-linuxX64/build.gradle.kts")
            .appendText("\nksp { arg(\"exception\", \"process\") }\n")
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-linuxX64:build"
        ).buildAndFail().let {
            val errors = it.output.lines().filter { it.startsWith("e: [ksp]") }
            Assertions.assertEquals("e: [ksp] java.lang.Exception: Test Exception in process", errors.first())
        }
        project.restore("workload-js/build.gradle.kts")
    }

    private fun verifyAll(result: BuildResult) {
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)
        Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspTestKotlinLinuxX64")?.outcome)

        verify(
            "workload/build/libs/workload-jvm-1.0-SNAPSHOT.jar",
            listOf(
                "com/example/Foo.class"
            )
        )

        verify(
            "workload/build/libs/workload-js-1.0-SNAPSHOT.klib",
            listOf(
                "default/ir/types.knt"
            )
        )

        verifyKexe("workload/build/bin/linuxX64/debugExecutable/workload.kexe")
        verifyKexe("workload/build/bin/linuxX64/releaseExecutable/workload.kexe")
        verifyKexe("workload/build/bin/androidNativeX64/debugShared/libworkload.so")
        verifyKexe("workload/build/bin/androidNativeX64/releaseShared/libworkload.so")
        verifyKexe("workload/build/bin/androidNativeArm64/debugShared/libworkload.so")
        verifyKexe("workload/build/bin/androidNativeArm64/releaseShared/libworkload.so")

        // TODO: Enable after CI's Xcode version catches up.
        // Assertions.assertTrue(
        //     result.task(":workload:kspKotlinIosArm64")?.outcome == TaskOutcome.SUCCESS ||
        //         result.task(":workload:kspKotlinIosArm64")?.outcome == TaskOutcome.SKIPPED
        // )
        // Assertions.assertTrue(
        //     result.task(":workload:kspKotlinMacosX64")?.outcome == TaskOutcome.SUCCESS ||
        //         result.task(":workload:kspKotlinMacosX64")?.outcome == TaskOutcome.SKIPPED
        // )
        Assertions.assertTrue(
            result.task(":workload:kspKotlinMingwX64")?.outcome == TaskOutcome.SUCCESS ||
                result.task(":workload:kspKotlinMingwX64")?.outcome == TaskOutcome.SKIPPED
        )

        Assertions.assertFalse(result.output.contains("kotlin scripting plugin:"))
        Assertions.assertTrue(result.output.contains("w: [ksp] platforms: [JVM"))
        Assertions.assertTrue(result.output.contains("w: [ksp] platforms: [JS"))
        Assertions.assertTrue(result.output.contains("w: [ksp] platforms: [Native"))
    }

    @Test
    fun testMainConfiguration() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val buildScript = File(project.root, "workload/build.gradle.kts")
        val lines = buildScript.readLines().takeWhile {
            it.trimEnd() != "dependencies {"
        }
        buildScript.writeText(lines.joinToString(System.lineSeparator()))
        buildScript.appendText(System.lineSeparator())
        buildScript.appendText("dependencies {")
        buildScript.appendText(System.lineSeparator())
        buildScript.appendText("    add(\"ksp\", project(\":test-processor\"))")
        buildScript.appendText(System.lineSeparator())
        buildScript.appendText("}")

        val messages = listOf(
            "The 'ksp' configuration is deprecated in Kotlin Multiplatform projects. ",
            "Please use target-specific configurations like 'kspJvm' instead."
        )

        // KotlinNative doesn't support configuration cache yet.
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload:build",
            "-Pksp.allow.all.target.configuration=false"
        ).buildAndFail().apply {
            Assertions.assertTrue(
                messages.all {
                    output.contains(it)
                }
            )
            checkExecutionOptimizations(output)
        }

        // KotlinNative doesn't support configuration cache yet.
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload:build",
        ).build().apply {
            Assertions.assertTrue(
                messages.all {
                    output.contains(it)
                }
            )
            verifyAll(this)
            checkExecutionOptimizations(output)
        }
    }
}
