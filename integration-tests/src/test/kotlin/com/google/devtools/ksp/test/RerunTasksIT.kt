package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * This test is used mostly to confirm Windows releases file locks after symbol processing is complete.
 * However, it will run on Linux too for completeness purposes
 */
class RerunTasksIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "android-rerun")
        project.setup()
    }

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
                Assertions.assertTrue(kspTask.isNotEmpty())
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
                Assertions.assertTrue(kspTask.isNotEmpty())
            }
    }
}
