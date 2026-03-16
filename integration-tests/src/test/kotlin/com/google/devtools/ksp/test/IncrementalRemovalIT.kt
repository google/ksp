package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IncrementalRemovalIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "incremental-removal")
        project.setup()
    }

    @Test
    fun testRemoveOutputs() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val k1 = "workload/src/main/kotlin/p1/K1.kt"

        gradleRunner.withArguments("run").build().let { result ->
            Assertions.assertTrue(result.output.contains("result: generated"))
        }

        File(project.root, k1).writeText(
            "package p1\n\nclass K1\n\nclass Foo : Bar { override fun s() = \"crafted\" }\n"
        )
        gradleRunner.withArguments("run").build().let { result ->
            Assertions.assertTrue(result.output.contains("result: crafted"))
        }

        project.restore(k1)
        gradleRunner.withArguments("run").build().let { result ->
            Assertions.assertTrue(result.output.contains("result: generated"))
        }
    }
}
