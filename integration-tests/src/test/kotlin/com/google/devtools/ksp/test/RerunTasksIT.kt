package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/**
 * This test is used mostly to confirm Windows releases file locks after symbol processing is complete.
 * However, it will run on Linux too for completeness purposes
 */
class RerunTasksIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("android-rerun")

    @Test
    fun testPlaygroundAndroid() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            ":lib:common:media:assembleDebug",
            "--no-configuration-cache",
            "--no-build-cache",
            "--rerun-tasks",
        )
            .build().let { result ->
                val output = result.output.lines()
                val kspTask = output.filter {
                    it.contains(":lib:common:media:kspDebugKotlin")
                }
                Assert.assertTrue(kspTask.isNotEmpty())
            }

        gradleRunner.withArguments(
            ":lib:common:media:assembleDebug",
            "--no-configuration-cache",
            "--no-build-cache",
            "--rerun-tasks",
        )
            .build().let { result ->
                val output = result.output.lines()
                val kspTask = output.filter {
                    it.contains(":lib:common:media:kspDebugKotlin")
                }
                Assert.assertTrue(kspTask.isNotEmpty())
            }
    }
}
