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
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class ClassLoaderLeakIT {
    @Rule
    @JvmField
    val project = TemporaryTestProject("playground", experimentalPsiResolution = false)

    @Test
    fun `KSP2 shuts down engine threads and allows old plugin classloader GC across classpath changes`() {
        val generationDir = File(project.root, "workload/generation").apply { mkdirs() }
        val generationFile = File(generationDir, "value").apply { writeText("0") }

        val buildFile = File(project.root, "workload/build.gradle.kts")
        val original = buildFile.readText()
        buildFile.writeText(
            """
            import java.lang.management.ManagementFactory

            buildscript {
                dependencies {
                    classpath(files("generation"))
                }
            }
            """.trimIndent() + "\n" + original + "\n" + """
            tasks.register("reportKspState") {
                doLast {
                    val threads = Thread.getAllStackTraces().keys
                    val kspThreads = threads.filter { t ->
                        t.name == "Periodic tasks thread" ||
                            t.javaClass.name.startsWith("ksp.") ||
                            (t !== Thread.currentThread() && t.contextClassLoader?.toString()?.contains("project-workload") == true)
                    }
                    val metaspaceMb = ManagementFactory.getMemoryPoolMXBeans()
                        .first { it.name == "Metaspace" }
                        .usage.used / (1024 * 1024)
                    val clId = System.identityHashCode(com.google.devtools.ksp.gradle.KspAATask::class.java.classLoader)
                    println("KSP_STATE clId=${'$'}clId metaspaceMb=${'$'}metaspaceMb kspThreads=${'$'}{kspThreads.size}")
                    kspThreads.forEach { t ->
                        println("KSP_THREAD=" + t.name + " | class=" + t.javaClass.name + " | ccl=" + t.contextClassLoader)
                    }
                }
            }
            """.trimIndent()
        )

        val runner = GradleRunner.create().withProjectDir(project.root)

        val seenClassLoaderIds = mutableSetOf<String>()
        for (iteration in 1..3) {
            generationFile.writeText(iteration.toString())

            val checkBeforeKsp = runner.withArguments(
                ":workload:reportKspState",
                "--no-configuration-cache",
            ).build()
            val stateLine = checkBeforeKsp.output.lines().first { it.startsWith("KSP_STATE ") }
            val kspThreads = stateLine.substringAfter("kspThreads=").trim().toInt()
            assertEquals(
                "Expected no KSP engine threads surviving before iteration $iteration:\n${checkBeforeKsp.output}",
                0,
                kspThreads,
            )
            seenClassLoaderIds.add(stateLine.substringAfter("clId=").substringBefore(" "))

            val kspBuild = runner.withArguments(
                ":workload:kspKotlin",
                "--rerun",
                "--no-configuration-cache",
            ).build()
            assertEquals(TaskOutcome.SUCCESS, kspBuild.task(":workload:kspKotlin")?.outcome)
        }

        // Verify the test actually triggered new plugin classloaders across iterations.
        assertEquals(3, seenClassLoaderIds.size)

        // Final check after the last KSP build finishes.
        generationFile.writeText("final")
        val finalCheck = runner.withArguments(
            ":workload:reportKspState",
            "--no-configuration-cache",
        ).build()
        val finalState = finalCheck.output.lines().first { it.startsWith("KSP_STATE ") }
        val finalThreads = finalState.substringAfter("kspThreads=").trim().toInt()
        assertEquals(
            "Expected no KSP engine threads surviving after final build:\n${finalCheck.output}",
            0,
            finalThreads,
        )
    }

    @Test
    fun `KSP2 shuts down engine threads with configuration cache enabled`() {
        val buildFile = File(project.root, "workload/build.gradle.kts")
        buildFile.appendText(
            "\n" + """
            tasks.register("checkKspThreads") {
                doLast {
                    val threads = Thread.getAllStackTraces().keys
                    val kspThreads = threads.filter { t ->
                        t.name == "Periodic tasks thread" || t.javaClass.name.startsWith("ksp.")
                    }
                    println("KSP_THREADS_COUNT=" + kspThreads.size)
                }
            }
            """.trimIndent()
        )

        val runner = GradleRunner.create().withProjectDir(project.root)

        // Run 1: stores configuration cache entry and runs KSP.
        val build1 = runner.withArguments(":workload:kspKotlin", "--rerun").build()
        assertEquals(TaskOutcome.SUCCESS, build1.task(":workload:kspKotlin")?.outcome)

        // Run 2: reuses configuration cache entry and runs KSP again.
        val build2 = runner.withArguments(":workload:kspKotlin", "--rerun").build()
        assertEquals(TaskOutcome.SUCCESS, build2.task(":workload:kspKotlin")?.outcome)

        // Run 3: verify no KSP engine threads survived after the cached build finished.
        val check = runner.withArguments(":workload:checkKspThreads").build()
        val count = check.output.lines()
            .firstOrNull { it.startsWith("KSP_THREADS_COUNT=") }
            ?.substringAfter("=")?.toInt()
        assertEquals(
            "Expected no KSP engine threads surviving after configuration-cached build:\n${check.output}",
            0,
            count,
        )
    }
}
