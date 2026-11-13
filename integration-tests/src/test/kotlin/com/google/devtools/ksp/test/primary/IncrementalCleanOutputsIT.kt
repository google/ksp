package com.google.devtools.ksp.test.primary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Regression coverage for https://github.com/google/ksp/issues/2536. */
@RunWith(Parameterized::class)
class IncrementalCleanOutputsIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project = TemporaryTestProject(
        "incremental-clean-outputs",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    private val producer: File
        get() = File(project.root, "workload/src/main/kotlin/fixture/Producer.kt")
    private val consumer: File
        get() = File(project.root, "workload/src/main/kotlin/fixture/Consumer.kt")
    private val outputPaths = listOf("kotlin/fixture/KotlinModule.kt", "java/fixture/JavaModule.java")

    private fun build(): String = GradleRunner.create().withProjectDir(project.root)
        .withArguments(":workload:compileKotlin", "--no-build-cache", "--info", "-Pksp.incremental=true")
        .build().output

    private fun generated(path: String) = File(project.root, "workload/build/generated/ksp/main/$path")

    private fun baseline() {
        val output = build()
        assertTrue(output, output.contains("round=1 producers=1"))
        assertTrue(output, output.contains("round=1 kotlinPresent=false javaPresent=false"))
        assertTrue(output, output.contains("checked next round"))
        outputPaths.forEach { assertTrue(it, generated(it).isFile) }
    }

    @Test
    fun cleanOutputsAreResolvableButNotReprocessed() {
        baseline()
        val contents = outputPaths.associateWith { generated(it).readText() }
        repeat(2) { iteration ->
            consumer.appendText("\n// comment-only change $iteration\n")
            val output = build()
            assertTrue(output, output.contains("round=1 producers=0 cachedCandidates=0"))
            assertTrue(output, output.contains("round=1 kotlinPresent=true javaPresent=true"))
            assertTrue(output, output.contains("round=2 kotlinPresent=true javaPresent=true"))
            assertTrue(output, output.contains("checked next round"))
            val candidateLines = output.lineSequence().filter {
                "newFiles=" in it || "allFiles=" in it || "cachedCandidates=" in it
            }.toList()
            assertTrue(output, candidateLines.isNotEmpty())
            candidateLines.forEach { line ->
                assertFalse(line, "KotlinModule.kt" in line || "JavaModule.java" in line)
                if ("cachedCandidates=" in line) assertTrue(line, "cachedCandidates=0" in line)
            }

            val dirtyLog = File(project.root, "workload/build/kspCaches/main/logs/kspDirtySet.log")
                .readText().replace('\\', '/')
            val dirty = dirtyLog.substringAfter("Dirty:\n").substringBefore("\nDirty / All:")
            assertTrue(dirtyLog, "fixture/Consumer.kt" in dirty)
            assertFalse(dirtyLog, "fixture/Producer.kt" in dirty)

            val outputLog = File(project.root, "workload/build/kspCaches/main/logs/kspSourceToOutputs.log")
                .readText()
            val reprocessed = outputLog.substringAfter("All reprocessed outputs\n")
            outputPaths.forEach { path ->
                assertFalse(outputLog, File(path).name in reprocessed)
                assertTrue(path, generated(path).readText() == contents.getValue(path))
            }
        }
    }

    @Test
    fun dirtyOutputsAreRegeneratedInsteadOfRestored() {
        baseline()
        producer.writeText(producer.readText().replace("version = 1", "version = 2"))
        consumer.appendText("\n// reprocess consumer too\n")
        val output = build()
        assertTrue(output, output.contains("round=1 producers=1"))
        assertTrue(output, output.contains("round=1 kotlinPresent=false javaPresent=false"))
        assertTrue(output, output.contains("checked next round"))
        outputPaths.forEach { path ->
            assertTrue(path, "version=2" in generated(path).readText())
        }
    }

    @Test
    fun outputsOfRemovedSourceAreNotRestored() {
        assertRemovedOutputsNotRestored { assertTrue(producer.delete()) }
    }

    @Test
    fun outputsOfRemovedAnnotationAreNotRestored() {
        assertRemovedOutputsNotRestored {
            producer.writeText("package fixture\n\nclass Producer\n")
        }
    }

    private fun assertRemovedOutputsNotRestored(removeContribution: () -> Unit) {
        baseline()
        removeContribution()
        consumer.writeText(consumer.readText().replace("expectPresent = true", "expectPresent = false"))
        repeat(2) { iteration ->
            if (iteration != 0) consumer.appendText("\n// check again after cache update\n")
            val output = build()
            assertTrue(output, output.contains("round=1 producers=0 cachedCandidates=0"))
            assertTrue(output, output.contains("round=1 kotlinPresent=false javaPresent=false"))
            assertTrue(output, output.contains("round=2 kotlinPresent=false javaPresent=false"))
            assertTrue(output, output.contains("checked next round"))
            outputPaths.forEach { path ->
                assertFalse(path, generated(path).exists())
                // The old backup may remain; its existence must not make a removed output visible.
                assertTrue(path, File(project.root, "workload/build/kspCaches/main/backups/$path").exists())
            }
        }
    }
}
