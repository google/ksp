package com.google.devtools.ksp.test

import Artifact
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class IncrementalIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("incremental")

    val src2Dirty = listOf(
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
                    "w: [ksp] <root>/K1.kt",
            ),
            "workload/src/main/kotlin/p1/K2.kt" to setOf(
                    "w: [ksp] <root>/K2.kt",
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
                    "w: [ksp] p1/TestJ2K.java",
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
                val dirties = result.output.split("\n").filter { it.startsWith("w: [ksp]") }.toSet()
                Assert.assertEquals(dirties, expectedDirties)
            }
        }
        val incrementalArtifact = Artifact(File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar"))
        Assert.assertEquals(cleanArtifact, incrementalArtifact)
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
                val dirties = result.output.split("\n").filter { it.startsWith("w: [ksp]") }.toSet()
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
}
