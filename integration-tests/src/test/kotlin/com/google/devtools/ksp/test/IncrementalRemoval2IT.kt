package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IncrementalRemoval2IT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "incremental-removal2")
        project.setup()
    }

    @Test
    fun testRemoveOutputs() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val k2 = "workload/src/main/kotlin/p1/K2.kt"

        gradleRunner.withArguments("run").build().let { result ->
            Assertions.assertTrue(result.output.contains("Written: K1.kt"))
            Assertions.assertTrue(result.output.contains("Input: K1.kt"))
            Assertions.assertTrue(result.output.contains("Input: K2.kt"))
        }

        File(project.root, k2).delete()
        gradleRunner.withArguments("run").build().let { result ->
            Assertions.assertTrue(result.output.contains("Written: K1.kt"))
            Assertions.assertTrue(result.output.contains("Input: K1.kt"))
            Assertions.assertFalse(result.output.contains("Input: K2.kt"))
        }
    }
}
