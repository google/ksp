/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.BuildResultFixture
import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AndroidIncrementalIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "playground-android-multi", "playground")
        project.setup()
    }

    private fun testWithExtraFlags() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "clean", ":application:compileDebugKotlin", "--configuration-cache-problems=warn", "--debug", "--stacktrace"
        ).build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:compileDebugKotlin")?.outcome)
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":application:compileDebugKotlin")?.outcome)
        }

        project.root.resolve("workload/src/main/java/com/example/A.kt").also {
            it.appendText(
                """

                class Unused
                """.trimIndent()
            )
        }

        gradleRunner.withArguments(
            ":application:compileDebugKotlin", "--configuration-cache-problems=warn", "--debug", "--stacktrace"
        ).build().let { result ->
            Assertions.assertEquals(
                setOf("workload/src/main/java/com/example/A.kt".replace('/', File.separatorChar)),
                BuildResultFixture(result).compiledKotlinSources,
            )
        }
    }

    @Test
    fun testPlaygroundAndroid() {
        testWithExtraFlags()
    }
}
