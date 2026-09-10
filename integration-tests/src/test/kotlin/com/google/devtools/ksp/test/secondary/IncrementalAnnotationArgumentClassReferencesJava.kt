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

package com.google.devtools.ksp.test.secondary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class IncrementalAnnotationArgumentClassReferencesJava(experimentalPsiResolution: Boolean) {

    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "incremental-annotation-argument-class-references-java",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)

        private const val TARGET: String = "downstream"
        private const val KSP_KOTLIN: String = ":$TARGET:kspKotlin"
        private const val ASSEMBLE: String = "assemble"
        private const val CLEAN: String = "clean"
        private const val PROCESSOR_LABEL: String = "[TestProcessor]"
        private const val GENERATED_FILE: String =
            "downstream/build/generated/ksp/main/kotlin/DownstreamClassGenerated.kt"
    }

    @Test
    fun testUpToDate() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":$TARGET:assemble")?.outcome)
        }

        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(KSP_KOTLIN)?.outcome)
        }
    }

    @Test
    fun testTransitiveJavaClasspathChange() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        // 1. Clean build
        gradleRunner.withArguments(CLEAN, ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            assertProcessed(result.output, "the clean build")
            Assert.assertTrue(
                "Expected the generated file to record the declarations visited by the processor",
                File(project.root, GENERATED_FILE).readText().contains("UpstreamChanges")
            )
        }

        // 2. Apply an ABI change to the upstream Java class that only the classpath refers to
        val fileToChange = File(project.root, "upstream/src/main/java/com/example/upstream/UpstreamChanges.java")
        val addedMember = "    public void addedMember() {\n    }\n}\n"
        fileToChange.writeText(fileToChange.readText().trimEnd().removeSuffix("}") + addedMember)

        // 3. Rebuild
        gradleRunner.withArguments(ASSEMBLE).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(KSP_KOTLIN)?.outcome)
            // Bug: the downstream sources are wrongly considered up to date and nothing is
            // reprocessed, because invalidation stops at the class file path of the changed class.
            Assert.assertEquals(
                emptyList<String>(),
                result.output.lines().filter { it.startsWith(PROCESSOR_LABEL) }
            )
            Assert.assertFalse(
                "Expected the generated file to remain stale",
                File(project.root, GENERATED_FILE).readText().contains("addedMember")
            )
        }
    }

    private fun assertProcessed(output: String, buildDescription: String) {
        val processed = output.lines().filter { it.startsWith(PROCESSOR_LABEL) }
        listOf("DownstreamClass", "UpstreamEntryPoint", "UpstreamChanges").forEach { simpleName ->
            Assert.assertTrue(
                "Expected $simpleName to be processed by $buildDescription, but the processor reported:\n" +
                    processed.joinToString("\n"),
                processed.contains("$PROCESSOR_LABEL Processing $simpleName")
            )
        }
    }
}
