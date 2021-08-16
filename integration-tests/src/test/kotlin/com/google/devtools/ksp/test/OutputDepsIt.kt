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

    val src2Output = mapOf(
        "workload/src/main/java/p1/J1.java" to setOf(
            "kotlin/p1/J1Generated.kt",
            "kotlin/p1/K1Generated.kt",
            "kotlin/p1/K2Generated.kt",
            "resources/p1.Anno1.log",
            "resources/p1.Anno2.log",
        ),
        "workload/src/main/java/p1/J2.java" to setOf(
            "kotlin/p1/J2Generated.kt",
            "resources/p1.Anno1.log",
            "resources/p1.Anno2.log",
        ),
        "workload/src/main/kotlin/p1/K1.kt" to setOf(
            "kotlin/p1/J1Generated.kt",
            "kotlin/p1/K1Generated.kt",
            "kotlin/p1/K2Generated.kt",
            "resources/p1.Anno1.log",
            "resources/p1.Anno2.log",
        ),
        "workload/src/main/kotlin/p1/K2.kt" to setOf(
            "kotlin/p1/J1Generated.kt",
            "kotlin/p1/K1Generated.kt",
            "kotlin/p1/K2Generated.kt",
            "resources/p1.Anno1.log",
            "resources/p1.Anno2.log",
        ),
    )

    val deletedSrc2Output = listOf(
        "workload/src/main/java/p1/J1.java" to listOf(
            "kotlin/p1/Anno1Generated.kt",
            "kotlin/p1/Anno2Generated.kt",
            "kotlin/p1/J2Generated.kt",
            "kotlin/p1/K1Generated.kt",
            "kotlin/p1/K2Generated.kt",
            "resources/p1.Anno1.log",
            "resources/p1.Anno2.log",
        ),
        "workload/src/main/java/p1/J2.java" to listOf(
            "kotlin/p1/Anno1Generated.kt",
            "kotlin/p1/Anno2Generated.kt",
            "kotlin/p1/K1Generated.kt",
            "kotlin/p1/K2Generated.kt",
            "resources/p1.Anno1.log",
            "resources/p1.Anno2.log",
        ),
        "workload/src/main/kotlin/p1/K1.kt" to listOf(
            "kotlin/p1/Anno1Generated.kt",
            "kotlin/p1/Anno2Generated.kt",
            "kotlin/p1/K2Generated.kt",
            "resources/p1.Anno1.log",
            "resources/p1.Anno2.log",
        ),
        "workload/src/main/kotlin/p1/K2.kt" to listOf(
            "kotlin/p1/Anno1Generated.kt",
            "kotlin/p1/Anno2Generated.kt",
            "resources/p1.Anno1.log",
            "resources/p1.Anno2.log",
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
            val srcFile = File(project.root, src)
            // In case that the test goes faster than the precision of timestamps.
            // It's 1s on Github's CI.
            Thread.sleep(1000)
            srcFile.appendText("\n\n")
            Thread.sleep(1000)
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.split("\n").filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(expectedDirties, dirties)

                val outputRoot = File(project.root, "workload/build/generated/ksp/main/")
                outputRoot.walk().filter { it.isFile() }.forEach {
                    if (it.toRelativeString(outputRoot) in src2Output[src]!!) {
                        Assert.assertTrue(it.lastModified() > srcFile.lastModified())
                    } else {
                        Assert.assertTrue(it.lastModified() < srcFile.lastModified())
                    }
                }
            }
        }
        val incrementalArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))
        Assert.assertEquals(cleanArtifact, incrementalArtifact)
    }

    @Test
    fun testDeletion() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        deletedSrc2Output.forEach { (src, expectedDirties) ->
            File(project.root, src).delete()
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val outputRoot = File(project.root, "workload/build/generated/ksp/main/")
                val outputs = outputRoot.walk().filter { it.isFile() }.map { it.toRelativeString(outputRoot) }
                    .toList().sorted()
                Assert.assertEquals(expectedDirties, outputs)
            }
        }
    }
}
