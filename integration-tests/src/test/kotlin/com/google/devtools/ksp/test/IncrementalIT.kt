package com.google.devtools.ksp.test

import Artifact
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class IncrementalIT(val useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("incremental", useKSP2 = useKSP2)

    val src2DirtyKSP1 = listOf(
        "workload/src/main/java/p1/J1.java" to setOf(
            "w: [ksp] p1/TestK2J.kt",
            "w: [ksp] p1/TestJ2J.java",
            "w: [ksp] p1/J1.java",
        ),
        "workload/src/main/java/p1/J2.java" to setOf(
            "w: [ksp] p1/J2.java",
        ),
        "workload/src/main/java/p1/TestJ2J.java" to setOf(
            "w: [ksp] p1/TestJ2J.java",
        ),
        "workload/src/main/java/p1/TestJ2K.java" to setOf(
            "w: [ksp] p1/TestJ2K.java",
        ),
        "workload/src/main/java/p2/J2.java" to setOf(
            "w: [ksp] p1/TestK2J.kt",
            "w: [ksp] p2/J2.java",
            "w: [ksp] p1/TestJ2J.java",
        ),
        "workload/src/main/java/p3/J1.java" to setOf(
            "w: [ksp] p3/J1.java",
        ),
        "workload/src/main/java/p3/J2.java" to setOf(
            "w: [ksp] p3/J2.java",
        ),
        "workload/src/main/java/p3/J3.java" to setOf(
            "w: [ksp] p1/TestK2J.kt",
            "w: [ksp] p1/TestJ2J.java",
            "w: [ksp] p3/J3.java",
        ),
        "workload/src/main/kotlin/p1/K1.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
            "w: [ksp] p1/K1.kt",
            "w: [ksp] p1/TestJ2K.java",
        ),
        "workload/src/main/kotlin/p1/K2.kt" to setOf(
            "w: [ksp] p1/K2.kt",
        ),
        "workload/src/main/kotlin/p1/TestK2J.kt" to setOf(
            "w: [ksp] p1/TestK2J.kt",
        ),
        "workload/src/main/kotlin/p1/TestK2K.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
        ),
        "workload/src/main/kotlin/p2/K2.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
            "w: [ksp] p2/K2.kt",
            "w: [ksp] p1/TestJ2K.java",
        ),
        "workload/src/main/kotlin/p3/K1.kt" to setOf(
            "w: [ksp] p3/K1.kt",
        ),
        "workload/src/main/kotlin/p3/K2.kt" to setOf(
            "w: [ksp] p3/K2.kt",
        ),
        "workload/src/main/kotlin/p3/K3.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
            "w: [ksp] p3/K3.kt",
            "w: [ksp] p1/TestJ2K.java",
        )
    )

    // K2 did some more lookups, which might be a spec change or potential optimization opportunity.
    // TODO: check with JetBrains.
    val src2DirtyKSP2 = listOf(
        "workload/src/main/java/p1/J1.java" to setOf(
            "w: [ksp] p1/TestK2J.kt",
            "w: [ksp] p1/TestJ2J.java",
            "w: [ksp] p1/J1.java",
        ),
        "workload/src/main/java/p1/J2.java" to setOf(
            "w: [ksp] p1/TestK2J.kt",
            "w: [ksp] p1/J2.java",
        ),
        "workload/src/main/java/p1/TestJ2J.java" to setOf(
            "w: [ksp] p1/TestJ2J.java",
        ),
        "workload/src/main/java/p1/TestJ2K.java" to setOf(
            "w: [ksp] p1/TestJ2K.java",
        ),
        "workload/src/main/java/p2/J2.java" to setOf(
            "w: [ksp] p1/TestK2J.kt",
            "w: [ksp] p2/J2.java",
            "w: [ksp] p1/TestJ2J.java",
        ),
        "workload/src/main/java/p3/J1.java" to setOf(
            "w: [ksp] p1/TestK2J.kt",
            "w: [ksp] p3/J1.java",
        ),
        "workload/src/main/java/p3/J2.java" to setOf(
            "w: [ksp] p1/TestK2J.kt",
            "w: [ksp] p3/J2.java",
        ),
        "workload/src/main/java/p3/J3.java" to setOf(
            "w: [ksp] p1/TestK2J.kt",
            "w: [ksp] p1/TestJ2J.java",
            "w: [ksp] p3/J3.java",
        ),
        "workload/src/main/kotlin/p1/K1.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
            "w: [ksp] p1/K1.kt",
            "w: [ksp] p1/TestJ2K.java",
        ),
        "workload/src/main/kotlin/p1/K2.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
            "w: [ksp] p1/K2.kt",
        ),
        "workload/src/main/kotlin/p1/TestK2J.kt" to setOf(
            "w: [ksp] p1/TestK2J.kt",
        ),
        "workload/src/main/kotlin/p1/TestK2K.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
        ),
        "workload/src/main/kotlin/p2/K2.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
            "w: [ksp] p2/K2.kt",
            "w: [ksp] p1/TestJ2K.java",
        ),
        "workload/src/main/kotlin/p3/K1.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
            "w: [ksp] p3/K1.kt",
        ),
        "workload/src/main/kotlin/p3/K2.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
            "w: [ksp] p3/K2.kt",
        ),
        "workload/src/main/kotlin/p3/K3.kt" to setOf(
            "w: [ksp] p1/TestK2K.kt",
            "w: [ksp] p3/K3.kt",
            "w: [ksp] p1/TestJ2K.java",
        )
    )

    val src2Dirty = if (useKSP2) src2DirtyKSP2 else src2DirtyKSP1

    @Test
    fun testUpToDate() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        gradleRunner.withArguments("assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":workload:kspKotlin")?.outcome)
        }
    }

    @Test
    fun testIsolating() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }
        val cleanArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))

        src2Dirty.forEach { (src, expectedDirties) ->
            File(project.root, src).appendText("\n\n")
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(expectedDirties, dirties)
            }
        }
        val incrementalArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))
        Assert.assertEquals(cleanArtifact, incrementalArtifact)
    }

    val changeSets = listOf(
        listOf(7, 5),
        listOf(0, 12),
        listOf(13, 14),
        listOf(8, 10),
        listOf(11, 4),
        listOf(3, 15),
        listOf(6, 9),
        listOf(2, 1),
        listOf(3, 1, 12),
        listOf(13, 0, 11),
        listOf(6, 8, 4),
        listOf(10, 9, 15),
        listOf(2, 14, 5, 7),
        listOf(5, 0, 13, 15),
        listOf(3, 2, 6, 7),
        listOf(4, 14, 10, 1),
        listOf(12, 9, 8, 11),
        listOf(12, 13, 5, 14, 7),
        listOf(11, 2, 8, 8, 9),
        listOf(11, 2, 8, 8, 9),
        listOf(4, 0, 15, 1, 10),
    )

    @Test
    fun testMultipleChanges() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        changeSets.forEach { changeSet ->
            changeSet.forEach {
                File(project.root, src2Dirty[it].first).appendText("\n\n")
            }
            val expectedDirties = changeSet.flatMapTo(mutableSetOf()) {
                src2Dirty[it].second
            }
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(expectedDirties, dirties)
            }
        }
    }

    @Test
    fun testMultipleDeletes() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }

        val srcs = src2Dirty.map { it.first }

        changeSets.forEach { changeSet ->
            val notChanged = IntRange(0, srcs.size - 1).filter { it !in changeSet }

            // Touch a file so that Gradle won't UP_TO_DATE for us.
            notChanged.first().let {
                File(project.root, srcs[it]).appendText("\n\n")
            }

            // Delete files
            changeSet.forEach {
                File(project.root, srcs[it]).delete()
            }

            // in: "workload/src/main/kotlin/p1/K2.kt"
            // out:                         "p1/K2.kt"
            val expectedOutputs = notChanged.map() {
                srcs[it].split("/").subList(4, 6).joinToString(File.separator) + ".log"
            }.sorted()

            gradleRunner.withArguments(":workload:kspKotlin").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val outputRoot = File(project.root, "workload/build/generated/ksp/main/resources")
                val outputs = outputRoot.walk().filter { it.isFile() }.map {
                    it.toRelativeString(outputRoot)
                }.sorted().toList()

                Assert.assertEquals(expectedOutputs, outputs)
            }

            changeSet.forEach {
                project.restore(srcs[it])
            }
        }
    }

    @Test
    fun testArgChange() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }
        val cleanArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))

        val expectedDirties = src2Dirty.map { it.second }.flatten().toSet()
        fun buildAndCheck() {
            gradleRunner.withArguments("assemble").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                val dirties = result.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(dirties, expectedDirties)
            }
            val incrementalArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))
            Assert.assertEquals(cleanArtifact, incrementalArtifact)
        }

        File(project.root, "workload/build.gradle.kts").appendText("\nksp { arg(\"option1\", \"value1\") }\n")
        buildAndCheck()

        project.restore("workload/build.gradle.kts")
        File(project.root, "workload/build.gradle.kts").appendText("\nksp { arg(\"option1\", \"value2\") }\n")
        buildAndCheck()

        File(project.root, "workload/build.gradle.kts").appendText("\nksp { arg(\"option2\", \"value2\") }\n")
        buildAndCheck()

        project.restore("workload/build.gradle.kts")
        buildAndCheck()
    }

    @Test
    fun testProcessorChange() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("build").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
            Assert.assertEquals(TaskOutcome.NO_SOURCE, result.task(":workload:kspTestKotlin")?.outcome)
        }
        val cleanArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))

        val expectedDirties = src2Dirty.map { it.second }.flatten().toSet()
        fun buildAndCheck() {
            gradleRunner.withArguments("build").build().let { result ->
                Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
                Assert.assertEquals(TaskOutcome.NO_SOURCE, result.task(":workload:kspTestKotlin")?.outcome)
                val dirties = result.output.lines().filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(dirties, expectedDirties)
            }
            val incrementalArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))
            Assert.assertEquals(cleanArtifact, incrementalArtifact)
        }

        File(project.root, "validator/src/main/kotlin/Validator.kt").appendText("\n")
        buildAndCheck()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
