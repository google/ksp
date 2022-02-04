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
    fun testJvm() {
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
        }
    }

    @Test
    fun testJs() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-js:build"
        ).build().let {
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-js:build")?.outcome)
            verify(
                "workload-js/build/libs/workload-js-jslegacy-1.0-SNAPSHOT.jar",
                listOf(
                    "playground-workload-js-js-legacy.js"
                )
            )
            verify(
                "workload-js/build/libs/workload-js-jsir-1.0-SNAPSHOT.klib",
                listOf(
                    "default/ir/types.knt"
                )
            )
            Assert.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assert.assertTrue(it.output.contains("w: [ksp] platforms: [JS"))
        }
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
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "clean",
            ":workload-androidNative:build"
        ).build().let {
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload-androidNative:build")?.outcome)
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeX64/debugExecutable/workload-androidNative.so"
            )
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeX64/releaseExecutable/workload-androidNative.so"
            )
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeArm64/debugExecutable/workload-androidNative.so"
            )
            verifyKexe(
                "workload-androidNative/build/bin/androidNativeArm64/releaseExecutable/workload-androidNative.so"
            )
            Assert.assertFalse(it.output.contains("kotlin scripting plugin:"))
            Assert.assertTrue(it.output.contains("w: [ksp] platforms: [Native"))
        }
    }

    @Test
    fun testLinuxX64() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

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
            "workload/build/libs/workload-jslegacy-1.0-SNAPSHOT.jar",
            listOf(
                "playground-workload-js-legacy.js"
            )
        )

        verify(
            "workload/build/libs/workload-jsir-1.0-SNAPSHOT.klib",
            listOf(
                "default/ir/types.knt"
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

        Assert.assertFalse(result.output.contains("kotlin scripting plugin:"))
        Assert.assertTrue(result.output.contains("w: [ksp] platforms: [JVM"))
        Assert.assertTrue(result.output.contains("w: [ksp] platforms: [JS"))
        Assert.assertTrue(result.output.contains("w: [ksp] platforms: [Native"))
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
            "-Pksp.allow.all.target.configuration=false"
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
