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
 * Incremental runs of `:workload:kspKotlin`, whose processor lists the classes of package `p6` of the classpath in an
 * aggregating output. KSP reruns it only when a class in `p6` changes.
 */
class IncrementalAggregatingClasspathIT {
    @Rule
    @JvmField
    val project = TemporaryTestProject(
        "incremental-aggregating-classpath",
        "incremental-classpath",
        experimentalPsiResolution = false,
    )

    private val p6Log by lazy { File(project.root, "workload/build/generated/ksp/main/resources/p6.log") }

    @Test
    fun `the aggregating output is regenerated only when a class in its package changes`() {
        addToClasspath("p6", "A")
        build()
        assertEquals("p6.A\n", p6Log.readText())

        addToClasspath("p5", "Unrelated")
        build().let { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
            assertFalse(result.output.contains(LOADED_PROVIDERS))
        }
        assertEquals("p6.A\n", p6Log.readText())

        addToClasspath("p6", "B")
        assertTrue(build().output.contains(LOADED_PROVIDERS))
        assertEquals("p6.A\np6.B\n", p6Log.readText())

        removeFromClasspath("p6", "A")
        build()
        assertEquals("p6.B\n", p6Log.readText())
    }

    @Test
    fun `a processor that found nothing in a classpath package runs when a class is added to it`() {
        build()
        assertFalse(p6Log.exists())

        addToClasspath("p6", "A")
        build()
        assertEquals("p6.A\n", p6Log.readText())
    }

    private fun build(): BuildResult =
        GradleRunner.create().withProjectDir(project.root).withArguments("--info", ":workload:kspKotlin").build()

    /** l4 is on the classpath of workload. */
    private fun classInL4(packageName: String, className: String) =
        File(project.root, "l4/src/main/kotlin/$packageName/$className.kt")

    private fun addToClasspath(packageName: String, className: String) {
        classInL4(packageName, className).apply { parentFile.mkdirs() }
            .writeText("package $packageName\n\nclass $className\n")
    }

    private fun removeFromClasspath(packageName: String, className: String) {
        assertTrue(classInL4(packageName, className).delete())
    }

    private companion object {
        const val LOADED_PROVIDERS = "i: [ksp] loaded provider(s): [TestProcessorProvider]"
    }
}
