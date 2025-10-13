package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class IncrementalEmptyCPIT() {
    @Rule
    @JvmField
    val project: TemporaryTestProject =
        TemporaryTestProject("incremental-classpath2", "incremental-classpath")

    private val emptyMessage = setOf("w: [ksp] processing done")

    private val prop2Dirty = listOf(
        "l1/src/main/kotlin/p1/TopProp1.kt" to setOf(
            "w: [ksp] p1/K1.kt",
            "w: [ksp] p1/K2.kt",
            "w: [ksp] p1/K3.kt",
        ),
    )

    @Test
    fun testCPChangesForProperties() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        // Dummy changes
        prop2Dirty.forEach { (src, _) ->
            File(project.root, src).appendText("\n\n")
            gradleRunner.withArguments("assemble").build().let { result ->
                // Trivial changes should not result in re-processing.
                Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
            }
        }

        // Value changes. It is not really used but should still trigger reprocessing of aggregating outputs.
        prop2Dirty.forEach { (src, expectedDirties) ->
            File(project.root, src).writeText("package p1\n\nval MyTopProp1: Int = 1")
            gradleRunner.withArguments("assemble").build().let { result ->
                // Value changes will result in re-processing of aggregating outputs.
                Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
            }
        }
    }
}
