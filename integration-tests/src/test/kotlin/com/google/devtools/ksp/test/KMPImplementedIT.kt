package com.google.devtools.ksp.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assume
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.jar.*

@RunWith(Parameterized::class)
class KMPImplementedIT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("kmp", useKSP2 = useKSP2)

    private fun verify(jarName: String, contents: List<String>) {
        val artifact = File(project.root, jarName)
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            contents.forEach {
                Assert.assertTrue(jarFile.getEntry(it).size > 0)
            }
        }
    }

    private fun verifyKexe(path: String) {
        val artifact = File(project.root, path)
        Assert.assertTrue(artifact.exists())
        Assert.assertTrue(artifact.readBytes().size > 0)
    }

    private fun checkExecutionOptimizations(log: String) {
        Assert.assertFalse(
            "Execution optimizations have been disabled",
            log.contains("Execution optimizations have been disabled")
        )
    }

    @Test
    fun testJvm() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-jvm:build"
        ).build().let {
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-jvm:build")?.outcome)
            verify(
                "workload-jvm/build/libs/workload-jvm-jvm-1.0-SNAPSHOT.jar",
                listOf(
                    "com/example/Foo.class"
                )
            )
            Assert.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assert.assertTrue(it.output.contains("w: [ksp] platforms: [JVM"))
            Assert.assertTrue(it.output.contains("w: [ksp] List has superTypes: true"))
            checkExecutionOptimizations(it.output)
        }
    }

    @Test
    fun testJvmErrorLog() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload-jvm/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"process\") }\n")
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-jvm:build"
        ).buildAndFail().let {
            val errors = it.output.lines().filter { it.startsWith("e: [ksp]") }
            Assert.assertEquals("e: [ksp] java.lang.Exception: Test Exception in process", errors.first())
        }
        project.restore("workload-jvm/build.gradle.kts")
    }

    @Test
    fun testJs() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-js:build"
        ).build().let {
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-js:build")?.outcome)
            verify(
                "workload-js/build/libs/workload-js-js-1.0-SNAPSHOT.klib",
                listOf(
                    "default/ir/types.knt"
                )
            )
            Assert.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assert.assertTrue(it.output.contains("w: [ksp] platforms: [JS"))
            Assert.assertTrue(it.output.contains("w: [ksp] List has superTypes: true"))
            checkExecutionOptimizations(it.output)
        }
    }

    @Test
    fun testWasm() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-wasm:build"
        ).build().let {
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-wasm:build")?.outcome)
            verify(
                "workload-wasm/build/libs/workload-wasm-wasm-js-1.0-SNAPSHOT.klib",
                listOf(
                    "default/ir/types.knt"
                )
            )
            Assert.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assert.assertTrue(it.output.contains("w: [ksp] platforms: [wasm-js"))
            Assert.assertTrue(it.output.contains("w: [ksp] List has superTypes: true"))
            checkExecutionOptimizations(it.output)
        }
    }

    @Test
    fun testJsErrorLog() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload-js/build.gradle.kts").appendText("\nksp { arg(\"exception\", \"process\") }\n")
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-js:build"
        ).buildAndFail().let {
            val errors = it.output.lines().filter { it.startsWith("e: [ksp]") }
            Assert.assertEquals("e: [ksp] java.lang.Exception: Test Exception in process", errors.first())
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
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-androidNative:build"
        ).build().let {
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-androidNative:build")?.outcome)
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeX64/debugExecutable/workload-androidNative.kexe"
            )
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeX64/releaseExecutable/workload-androidNative.kexe"
            )
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeArm64/debugExecutable/workload-androidNative.kexe"
            )
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeArm64/releaseExecutable/workload-androidNative.kexe"
            )
            Assert.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assert.assertTrue(it.output.contains("w: [ksp] platforms: [Native"))
            checkExecutionOptimizations(it.output)
        }
    }

    @Test
    fun testLinuxX64() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val genDir = File(project.root, "workload-linuxX64/build/generated/ksp/linuxX64/linuxX64Main/kotlin")

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-linuxX64:build"
        ).build().let {
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-linuxX64:build")?.outcome)
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-linuxX64:kspTestKotlinLinuxX64")?.outcome)
            verifyKexe("workload-linuxX64/build/bin/linuxX64/debugExecutable/workload-linuxX64.kexe")
            verifyKexe("workload-linuxX64/build/bin/linuxX64/releaseExecutable/workload-linuxX64.kexe")

            // TODO: Enable after CI's Xcode version catches up.
            // Assert.assertTrue(
            //     result.task(":workload-linuxX64:kspKotlinIosArm64")?.outcome == TaskOutcome.SUCCESS ||
            //         result.task(":workload-linuxX64:kspKotlinIosArm64")?.outcome == TaskOutcome.SKIPPED
            // )
            // Assert.assertTrue(
            //     result.task(":workload-linuxX64:kspKotlinMacosX64")?.outcome == TaskOutcome.SUCCESS ||
            //         result.task(":workload-linuxX64:kspKotlinMacosX64")?.outcome == TaskOutcome.SKIPPED
            // )
            Assert.assertTrue(
                it.task(":workload-linuxX64:kspKotlinMingwX64")?.outcome == TaskOutcome.SUCCESS ||
                    it.task(":workload-linuxX64:kspKotlinMingwX64")?.outcome == TaskOutcome.SKIPPED
            )
            Assert.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assert.assertTrue(it.output.contains("w: [ksp] platforms: [Native"))
            Assert.assertTrue(it.output.contains("w: [ksp] List has superTypes: true"))
            Assert.assertTrue(File(genDir, "Main_dot_kt.kt").exists())
            Assert.assertTrue(File(genDir, "ToBeRemoved_dot_kt.kt").exists())
            checkExecutionOptimizations(it.output)
        }

        File(project.root, "workload-linuxX64/src/linuxX64Main/kotlin/ToBeRemoved.kt").delete()
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            ":workload-linuxX64:build"
        ).build().let {
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-linuxX64:build")?.outcome)
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-linuxX64:kspTestKotlinLinuxX64")?.outcome)
            verifyKexe("workload-linuxX64/build/bin/linuxX64/debugExecutable/workload-linuxX64.kexe")
            verifyKexe("workload-linuxX64/build/bin/linuxX64/releaseExecutable/workload-linuxX64.kexe")
            Assert.assertTrue(File(genDir, "Main_dot_kt.kt").exists())
            Assert.assertFalse(File(genDir, "ToBeRemoved_dot_kt.kt").exists())
            checkExecutionOptimizations(it.output)
        }
    }

    @Ignore
    @Test
    fun testNonEmbeddableArtifact() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
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
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload-linuxX64/build.gradle.kts")
            .appendText("\nksp { arg(\"exception\", \"process\") }\n")
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-linuxX64:build"
        ).buildAndFail().let {
            val errors = it.output.lines().filter { it.startsWith("e: [ksp]") }
            Assert.assertEquals("e: [ksp] java.lang.Exception: Test Exception in process", errors.first())
        }
        project.restore("workload-js/build.gradle.kts")
    }

    private fun verifyAll(result: BuildResult) {
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspTestKotlinLinuxX64")?.outcome)

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
        verifyKexe("workload/build/bin/androidNativeX64/debugExecutable/workload.kexe")
        verifyKexe("workload/build/bin/androidNativeX64/releaseExecutable/workload.kexe")
        verifyKexe("workload/build/bin/androidNativeArm64/debugExecutable/workload.kexe")
        verifyKexe("workload/build/bin/androidNativeArm64/releaseExecutable/workload.kexe")

        // TODO: Enable after CI's Xcode version catches up.
        // Assert.assertTrue(
        //     result.task(":workload:kspKotlinIosArm64")?.outcome == TaskOutcome.SUCCESS ||
        //         result.task(":workload:kspKotlinIosArm64")?.outcome == TaskOutcome.SKIPPED
        // )
        // Assert.assertTrue(
        //     result.task(":workload:kspKotlinMacosX64")?.outcome == TaskOutcome.SUCCESS ||
        //         result.task(":workload:kspKotlinMacosX64")?.outcome == TaskOutcome.SKIPPED
        // )
        Assert.assertTrue(
            result.task(":workload:kspKotlinMingwX64")?.outcome == TaskOutcome.SUCCESS ||
                result.task(":workload:kspKotlinMingwX64")?.outcome == TaskOutcome.SKIPPED
        )

        Assert.assertFalse(result.output.contains("kotlin scripting plugin:"))
        Assert.assertTrue(result.output.contains("w: [ksp] platforms: [JVM"))
        Assert.assertTrue(result.output.contains("w: [ksp] platforms: [JS"))
        Assert.assertTrue(result.output.contains("w: [ksp] platforms: [Native"))
    }

    @Test
    fun testMainConfiguration() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
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
            Assert.assertTrue(
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
            Assert.assertTrue(
                messages.all {
                    output.contains(it)
                }
            )
            verifyAll(this)
            checkExecutionOptimizations(output)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
