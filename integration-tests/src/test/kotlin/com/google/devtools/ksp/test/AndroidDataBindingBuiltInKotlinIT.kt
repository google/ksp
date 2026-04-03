package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AndroidDataBindingBuiltInKotlinIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "android-data-binding-builtinkotlin")
        project.setup()
    }

    @Test
    fun testPlaygroundAndroid() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        // Disabling configuration cache. See https://github.com/google/ksp/issues/299 for details
        gradleRunner.withArguments(
            "clean",
            ":app:assemble",
            "--configuration-cache-problems=warn",
            "--info",
            "--stacktrace"
        )
            .build().let { result ->
                val output = result.output.lines()
                val kspTask = output.filter {
                    it.contains(":app:kspDebugKotlin")
                }
                Assertions.assertTrue(kspTask.isNotEmpty())
            }
    }
}
