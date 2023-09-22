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
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class AndroidIncrementalIT(useK2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android-multi", "playground", useK2)

    private fun testWithExtraFlags(vararg extras: String) {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            *extras,
            "clean", ":application:compileDebugKotlin", "--configuration-cache-problems=warn", "--debug", "--stacktrace"
        ).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:compileDebugKotlin")?.outcome)
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":application:compileDebugKotlin")?.outcome)
        }

        project.root.resolve("workload/src/main/java/com/example/A.kt").also {
            it.appendText(
                """

                class Unused
                """.trimIndent()
            )
        }

        gradleRunner.withArguments(
            *extras,
            ":application:compileDebugKotlin", "--configuration-cache-problems=warn", "--debug", "--stacktrace"
        ).build().let { result ->
            Assert.assertEquals(
                setOf("workload/src/main/java/com/example/A.kt".replace('/', File.separatorChar)),
                BuildResultFixture(result).compiledKotlinSources,
            )
        }
    }

    @Test
    fun testPlaygroundAndroid() {
        testWithExtraFlags()
    }

    @Test
    fun testPlaygroundAndroidUseClasspathSnapshotFalse() {
        testWithExtraFlags("-Pkotlin.incremental.useClasspathSnapshot=false")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "K2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
