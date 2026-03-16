package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IncrementalEmptyCPIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "incremental-classpath2", "incremental-classpath")
        project.setup()
    }

    private val prop2Dirty = listOf(
        "l1/src/main/kotlin/p1/TopProp1.kt" to setOf(
            "w: [ksp] p1/K1.kt",
            "w: [ksp] p1/K2.kt",
            "w: [ksp] p1/K3.kt",
        ),
    )

    @Test
    fun testTrivialChanges() {
        // Trivial (non-AST) changes should not result in re-processing.
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        prop2Dirty.forEach { (src, _) ->
            File(project.root, src).appendText("\n\n")
            gradleRunner.withArguments("assemble").build().let { result ->
                Assertions.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
            }
        }
    }

    @Test
    fun testValueChanges() {
        // Value changes should not result in re-processing, since values are not part of the AST.
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        prop2Dirty.forEach { (src, _) ->
            File(project.root, src).writeText("package p1\n\nval MyTopProp1: Int = 1")
            gradleRunner.withArguments("assemble").build().let { result ->
                Assertions.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
            }
        }
    }

    @Test
    fun testTypeChanges() {
        // Type changes should result in re-processing of aggregating outputs, since such changes apply to the AST.
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        prop2Dirty.forEach { (src, expectedDirties) ->
            File(project.root, src).writeText("package p1\n\nval MyTopProp1: Boolean = true")
            gradleRunner.withArguments("assemble").build().let { result ->
                Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
                Assertions.assertEquals(expectedDirties, dirties)
            }
        }
    }
}
