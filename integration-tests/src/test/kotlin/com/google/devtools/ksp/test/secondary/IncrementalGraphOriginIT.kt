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

@RunWith(Parameterized::class)
class IncrementalGraphOriginIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "incremental",
        experimentalPsiResolution = experimentalPsiResolution,
        incrementalLogging = null, // Setting to null means the property is not declared
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    @Test
    fun testGraphOriginOptionSetToValidNameLoggingEnabled() {
        project.appendProperty("ksp.incremental.log=true")
        project.appendProperty("ksp.incremental.log.graph.origin=p1.TestK2K")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        val dotFiles = project.root.walk().filter { it.name == "kspLookupGraphOrigin.dot" }.toList()
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to be generated", dotFiles.isNotEmpty())
        val content = dotFiles.first().readText()
        Assert.assertTrue("Expected digraph header in dot file", content.startsWith("digraph {"))
        Assert.assertTrue("Expected p1.TestK2K origin in dot file", content.contains("\"p1.TestK2K\""))
    }

    @Test
    fun testGraphOriginOptionSetToValidNameLoggingDisabled() {
        project.appendProperty("ksp.incremental.log=false")
        project.appendProperty("ksp.incremental.log.graph.origin=p1.TestK2K")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        val dotFiles = project.root.walk().filter { it.name == "kspLookupGraphOrigin.dot" }.toList()
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot not to be generated: $dotFiles", dotFiles.isEmpty())
    }

    @Test
    fun testGraphOriginOptionSetToValidNameLoggingUnset() {
        project.appendProperty("ksp.incremental.log.graph.origin=p1.TestK2K")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        val dotFiles = project.root.walk().filter { it.name == "kspLookupGraphOrigin.dot" }.toList()
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot not to be generated: $dotFiles", dotFiles.isEmpty())
    }

    @Test
    fun testGraphOriginOptionSetToInvalidNameLoggingEnabled() {
        project.appendProperty("ksp.incremental.log=true")
        val invalidName = "InvalidNameThatDoesNotExist"
        project.appendProperty("ksp.incremental.log.graph.origin=$invalidName")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        val dotFiles = project.root.walk().filter { it.name == "kspLookupGraphOrigin.dot" }.toList()
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot to be generated", dotFiles.isNotEmpty())
        val content = dotFiles.first().readText().trim()
        val digraphContentForInvalidName =
            """
            digraph {
              "$invalidName";
            }
            """.trimIndent()
        Assert.assertEquals(digraphContentForInvalidName, content)
    }

    @Test
    fun testGraphOriginOptionSetToInvalidNameLoggingDisabled() {
        project.appendProperty("ksp.incremental.log=false")
        val invalidName = "InvalidNameThatDoesNotExist"
        project.appendProperty("ksp.incremental.log.graph.origin=$invalidName")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        val dotFiles = project.root.walk().filter { it.name == "kspLookupGraphOrigin.dot" }.toList()
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot not to be generated: $dotFiles", dotFiles.isEmpty())
    }

    @Test
    fun testGraphOriginOptionSetToInvalidNameLoggingUnset() {
        val invalidName = "InvalidNameThatDoesNotExist"
        project.appendProperty("ksp.incremental.log.graph.origin=$invalidName")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        val dotFiles = project.root.walk().filter { it.name == "kspLookupGraphOrigin.dot" }.toList()
        Assert.assertTrue("Expected kspLookupGraphOrigin.dot not to be generated: $dotFiles", dotFiles.isEmpty())
    }

    @Test
    fun testGraphOriginOptionNotSetButLoggingEnabled() {
        project.appendProperty("ksp.incremental.log=true")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        val dotFiles = project.root.walk().filter { it.name == "kspLookupGraphOrigin.dot" }.toList()
        Assert.assertTrue(
            "Expected kspLookupGraphOrigin.dot NOT to be generated when option is unset $dotFiles",
            dotFiles.isEmpty()
        )
    }

    @Test
    fun testGraphOriginOptionNotSetButLoggingDisabled() {
        project.appendProperty("ksp.incremental.log=false")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        val dotFiles = project.root.walk().filter { it.name == "kspLookupGraphOrigin.dot" }.toList()
        Assert.assertTrue(
            "Expected kspLookupGraphOrigin.dot NOT to be generated when option is unset $dotFiles",
            dotFiles.isEmpty()
        )
    }

    @Test
    fun testGraphOriginOptionNotSetLoggingUnset() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("clean", "assemble").build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:kspKotlin")?.outcome)
        }

        val dotFiles = project.root.walk().filter { it.name == "kspLookupGraphOrigin.dot" }.toList()
        Assert.assertTrue(
            "Expected kspLookupGraphOrigin.dot NOT to be generated when option is unset $dotFiles",
            dotFiles.isEmpty()
        )
    }
}
