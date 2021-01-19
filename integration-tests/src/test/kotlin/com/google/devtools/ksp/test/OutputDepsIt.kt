package com.google.devtools.ksp.test

import Artifact
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class OutputDepsIt {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("output-deps")

    val src2Dirty = listOf(
            "workload/src/main/java/p1/J1.java" to setOf(
                    "w: [ksp] p1/J1.java",
                    "w: [ksp] p1/K1.kt",
                    "w: [ksp] p1/K2.kt",
            ),
            "workload/src/main/java/p1/J2.java" to setOf(
                    "w: [ksp] p1/J2.java",
            ),
            "workload/src/main/kotlin/p1/K1.kt" to setOf(
                    "w: [ksp] p1/J1.java",
                    "w: [ksp] p1/K1.kt",
                    "w: [ksp] p1/K2.kt",
            ),
            "workload/src/main/kotlin/p1/K2.kt" to setOf(
                    "w: [ksp] p1/J1.java",
                    "w: [ksp] p1/K1.kt",
                    "w: [ksp] p1/K2.kt",
            ),
    )

    @Test
    fun testOutputDeps() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }
        val cleanArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))

        src2Dirty.forEach { (src, expectedDirties) ->
            File(project.root, src).appendText("\n\n")
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.split("\n").filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(expectedDirties, dirties)
            }
        }
        val incrementalArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))
        Assert.assertEquals(cleanArtifact, incrementalArtifact)
    }
}
