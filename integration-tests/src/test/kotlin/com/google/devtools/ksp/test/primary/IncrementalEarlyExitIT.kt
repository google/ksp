/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.ksp.test.primary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Incremental runs of `:workload:kspKotlin` with nothing to process: KSP exits early, and the Gradle plugin neither
 * loads the processors nor deletes the outputs.
 *
 * The processor's outputs are isolating; otherwise KSP would process on every classpath change.
 */
class IncrementalEarlyExitIT {
    @Rule
    @JvmField
    val project = TemporaryTestProject(
        "incremental-early-exit",
        "incremental-classpath",
        experimentalPsiResolution = false,
    )

    private val outputDir by lazy { File(project.root, "workload/build/generated/ksp/main") }
    private val k1Output by lazy { File(outputDir, "resources/p1/K1.kt.log") }

    @Test
    fun `an unrelated classpath change neither loads the processors nor touches the outputs`() {
        assertTrue(build().output.contains(LOADED_PROVIDERS))
        val content = k1Output.readText()
        val timestamp = k1Output.lastModified()
        // Not in an output directory, so Gradle ignores it. Its presence shows that outputDir wasn't cleaned: a
        // restored output would have the same content and timestamp.
        val marker = File(outputDir, "marker").apply { writeText("") }

        addUnrelatedClassToClasspath()
        val result = build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        assertFalse(result.output.contains(LOADED_PROVIDERS))
        assertTrue("the output directory must not be cleaned", marker.exists())
        assertEquals(content, k1Output.readText())
        assertEquals(timestamp, k1Output.lastModified())
    }

    @Test
    fun `a source change after an early exit loads the processors and regenerates the outputs`() {
        build()
        addUnrelatedClassToClasspath()
        build()

        File(project.root, "workload/src/main/kotlin/p1/K1.kt").appendText("\nclass K1b\n")
        val result = build()

        assertTrue(result.output.contains(LOADED_PROVIDERS))
        assertTrue(k1Output.readText().contains("p1.K1b"))
    }

    private fun build(): BuildResult =
        GradleRunner.create().withProjectDir(project.root).withArguments("--info", ":workload:kspKotlin").build()

    /** Changes the ABI of l5, which is on the classpath of workload but not used by it. */
    private fun addUnrelatedClassToClasspath() {
        File(project.root, "l5/src/main/kotlin/p5/Unrelated.kt").apply { parentFile.mkdirs() }
            .writeText("package p5\n\nclass Unrelated\n")
    }

    private companion object {
        const val LOADED_PROVIDERS = "i: [ksp] loaded provider(s): [TestProcessorProvider]"
    }
}
