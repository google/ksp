package com.google.devtools.ksp.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.*

class KMPImplementedIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("kmp")

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

    @Test
    fun testAll() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        // KotlinNative doesn't support configuration cache yet.
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            "build"
        ).build().let {
            verifyAll(it)
        }
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
            "workload/build/libs/workload-js-1.0-SNAPSHOT.jar",
            listOf(
                "playground-workload.js"
            )
        )

        verify(
            "workload/build/libs/workload-metadata-1.0-SNAPSHOT.jar",
            listOf(
                "com/example/Foo.kotlin_metadata"
            )
        )

        verifyKexe("workload/build/bin/linuxX64/debugExecutable/workload.kexe")
        verifyKexe("workload/build/bin/linuxX64/releaseExecutable/workload.kexe")
        verifyKexe("workload/build/bin/androidNativeX64/debugExecutable/workload.so")
        verifyKexe("workload/build/bin/androidNativeX64/releaseExecutable/workload.so")
        verifyKexe("workload/build/bin/androidNativeArm64/debugExecutable/workload.so")
        verifyKexe("workload/build/bin/androidNativeArm64/releaseExecutable/workload.so")

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
    }

    @Test
    fun testMainConfiguration() {
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
            "build",
            "-Pksp.allow_all_target_configuration=false"
        ).buildAndFail().apply {
            Assert.assertTrue(
                messages.all {
                    output.contains(it)
                }
            )
        }

        // KotlinNative doesn't support configuration cache yet.
        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            "build"
        ).build().apply {
            Assert.assertTrue(
                messages.all {
                    output.contains(it)
                }
            )
            verifyAll(this)
        }
    }
}
