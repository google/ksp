package com.google.devtools.ksp.test.primary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.RandomAccessFile

@RunWith(Parameterized::class)
class IncrementalCPIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "incremental-classpath",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    private val src2Dirty = listOf(
        "l1/src/main/kotlin/p1/L1.kt" to setOf(
            "w: [ksp] p1/K1.kt",
            "w: [ksp] processing done",
        ),
        "l2/src/main/kotlin/p1/L2.kt" to setOf(
            "w: [ksp] p1/K1.kt",
            "w: [ksp] p1/K2.kt",
            "w: [ksp] processing done",
        ),
        "l3/src/main/kotlin/p1/L3.kt" to setOf(
            "w: [ksp] p1/K3.kt",
            "w: [ksp] processing done",
        ),
        "l4/src/main/kotlin/p1/L4.kt" to setOf(
            "w: [ksp] p1/K3.kt",
            "w: [ksp] processing done",
        ),
        "l5/src/main/kotlin/p1/L5.kt" to setOf(
            "w: [ksp] processing done",
        ),
    )

    private val emptyMessage = setOf("w: [ksp] processing done")

    @Test
    fun testCorruptedClasspathSnapshotRecovery() {
        val task = ":workload:kspKotlin"
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
            .withArguments(task, "--no-build-cache", "--info")

        val initial = gradleRunner.build()
        Assert.assertEquals(TaskOutcome.SUCCESS, initial.task(task)?.outcome)

        val snapshot = File(project.root, "workload/build/kspCaches/main/classpath-structure.bin")
        Assert.assertTrue("Expected a populated classpath snapshot", snapshot.length() > 4)
        RandomAccessFile(snapshot, "rw").use { it.setLength(it.length() / 2) }

        File(project.root, "workload/src/main/kotlin/p1/K1.kt").appendText("\n// Trigger KSP again.\n")
        val rebuilt = gradleRunner.build()
        Assert.assertEquals(TaskOutcome.SUCCESS, rebuilt.task(task)?.outcome)
        Assert.assertTrue(rebuilt.output.contains("[ksp] Could not read the previous classpath snapshot"))
        Assert.assertTrue(rebuilt.output.contains("at java.base/java.io.ObjectInputStream."))
        val processed = rebuilt.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
        Assert.assertEquals(
            emptyMessage + setOf("w: [ksp] p1/K1.kt", "w: [ksp] p1/K2.kt", "w: [ksp] p1/K3.kt"),
            processed,
        )

        File(project.root, "l2/src/main/kotlin/p1/L2.kt").appendText("\n{ val v100 = 100 }\n")
        val incremental = gradleRunner.build()
        Assert.assertEquals(TaskOutcome.SUCCESS, incremental.task(task)?.outcome)
        val dirties = incremental.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
        Assert.assertEquals(emptyMessage + setOf("w: [ksp] p1/K1.kt", "w: [ksp] p1/K2.kt"), dirties)
    }

    @Test
    fun testCPChanges() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        src2Dirty.forEach { (src, _) ->
            File(project.root, src).appendText("\n\n")
            gradleRunner.withArguments("assemble").build().let { result ->
                // Trivial changes should not result in re-processing.
                Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
            }
        }

        var i = 100
        src2Dirty.forEach { (src, expectedDirties) ->
            File(project.root, src).appendText("\n{ val v$i = ${i++} }\n")
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(expectedDirties, dirties)
            }
        }

        src2Dirty.forEach { (src, _) ->
            File(project.root, src).appendText("\n\nclass C${i++}\n")
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
                // Non-signature changes should not affect anything.
                Assert.assertEquals(emptyMessage, dirties)
            }
        }
    }

    private val func2Dirty = listOf(
        "l1/src/main/kotlin/p1/TopFunc1.kt" to setOf(
            "w: [ksp] processing done",
        ),
    )

    @Test
    fun testCPChangesForFunctions() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        // Dummy changes
        func2Dirty.forEach { (src, _) ->
            File(project.root, src).appendText("\n\n")
            gradleRunner.withArguments("assemble").build().let { result ->
                // Trivial changes should not result in re-processing.
                Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
            }
        }

        // Value changes
        func2Dirty.forEach { (src, _) ->
            File(project.root, src).writeText("package p1\n\nfun MyTopFunc1(): Int = 1")
            gradleRunner.withArguments("assemble").build().let { result ->
                // Value changes should not result in re-processing.
                Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
            }
        }

        // Signature changes
        func2Dirty.forEach { (src, expectedDirties) ->
            File(project.root, src).writeText("package p1\n\nfun MyTopFunc1(): Double = 1.0")
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(expectedDirties, dirties)
            }
        }
    }

    private val prop2Dirty = listOf(
        "l1/src/main/kotlin/p1/TopProp1.kt" to setOf(
            "w: [ksp] processing done",
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

        // Value changes
        prop2Dirty.forEach { (src, _) ->
            File(project.root, src).writeText("package p1\n\nval MyTopProp1: Int = 1")
            gradleRunner.withArguments("assemble").build().let { result ->
                // Value changes should not result in re-processing.
                Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
            }
        }

        // Signature changes
        prop2Dirty.forEach { (src, expectedDirties) ->
            File(project.root, src).writeText("package p1\n\nval MyTopProp1: Double = 1.0")
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(expectedDirties, dirties)
            }
        }
    }

    private fun toggleFlags(vararg extras: String) {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            *extras,
            "--rerun-tasks",
            "-Pksp.incremental=true",
            "-Pksp.incremental.intermodule=true",
            "assemble"
        ).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        gradleRunner.withArguments(
            *extras,
            "--rerun-tasks",
            "-Pksp.incremental=false",
            "-Pksp.incremental.intermodule=true",
            "assemble"
        ).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        gradleRunner.withArguments(
            *extras,
            "--rerun-tasks",
            "-Pksp.incremental=true",
            "-Pksp.incremental.intermodule=false",
            "assemble"
        ).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        gradleRunner.withArguments(
            *extras,
            "--rerun-tasks",
            "-Pksp.incremental=false",
            "-Pksp.incremental.intermodule=false",
            "assemble"
        ).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }
    }

    @Test
    fun toggleIncrementalFlagsWithoutConfigurationCache() {
        toggleFlags("--no-configuration-cache")
    }

    @Test
    fun toggleIncrementalFlagsWithConfigurationCache() {
        toggleFlags("--configuration-cache")
    }
}
