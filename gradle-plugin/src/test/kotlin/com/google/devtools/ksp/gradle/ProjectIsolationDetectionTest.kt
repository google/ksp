/*
 * Copyright 2026 Google LLC
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
package com.google.devtools.ksp.gradle

import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.gradle.testing.TestConfig
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests for the Project Isolation detection that decides whether KSP registers its generated sources through the
 * Project Isolation compatible codepath.
 *
 * Isolated Projects can be turned on and off from the command line, which takes precedence over any property, so
 * KSP has to ask Gradle for the effective state instead of reading the properties itself. See
 * https://github.com/google/ksp/issues/3188.
 */
class ProjectIsolationDetectionTest {
    @Rule
    @JvmField
    val tmpDir: TemporaryFolder = TemporaryFolder()

    private val testConfig = TestConfig.read()

    @Before
    fun setupProject() {
        tmpDir.root.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven("${testConfig.mavenRepoPath}")
                    gradlePluginPortal()
                    mavenCentral()
                    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
                }
            }
            rootProject.name = "ksp-isolation-detection"
            """.trimIndent()
        )
        // The plugins are only declared to put their classes on the build script classpath; the detection itself
        // doesn't need a Kotlin project. KSP's utilities reference KGP types, so KGP has to be there as well.
        tmpDir.root.resolve("build.gradle.kts").writeText(
            """
            import com.google.devtools.ksp.gradle.utils.enableProjectIsolationCompatibleCodepath

            plugins {
                id("org.jetbrains.kotlin.jvm") version "${testConfig.kotlinBaseVersion}" apply false
                id("com.google.devtools.ksp") version "${testConfig.kspVersion}" apply false
            }

            val isolationCompatible = project.enableProjectIsolationCompatibleCodepath()
            tasks.register("printKspIsolation") {
                inputs.property("isolationCompatible", isolationCompatible)
                doLast {
                    println("$ISOLATION_OUTPUT_PREFIX" + inputs.properties["isolationCompatible"])
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun isolationEnabledOnCommandLine() {
        assertThat(runBuild(MODERN_ISOLATION_GRADLE_VERSION, "--isolated-projects")).isTrue()
    }

    @Test
    fun isolationDisabledOnCommandLineOverridesProperty() {
        writeGradleProperties("org.gradle.isolated-projects=true")

        assertThat(runBuild(MODERN_ISOLATION_GRADLE_VERSION, "--no-isolated-projects")).isFalse()
    }

    @Test
    fun isolationEnabledByProperty() {
        writeGradleProperties("org.gradle.isolated-projects=true")

        assertThat(runBuild(MODERN_ISOLATION_GRADLE_VERSION)).isTrue()
    }

    @Test
    fun isolationEnabledByLegacyProperty() {
        writeGradleProperties("org.gradle.unsafe.isolated-projects=true")

        assertThat(runBuild()).isTrue()
    }

    @Test
    fun kspOptInWinsOverDisabledIsolation() {
        assertThat(
            runBuild(
                MODERN_ISOLATION_GRADLE_VERSION,
                "--no-isolated-projects",
                "-Pksp.project.isolation.enabled=true"
            )
        ).isTrue()
    }

    @Test
    fun isolationDisabledByDefault() {
        assertThat(runBuild()).isFalse()
    }

    private fun writeGradleProperties(contents: String) {
        tmpDir.root.resolve("gradle.properties").writeText(contents)
    }

    /**
     * Runs the test build and returns the value KSP computed for the Project Isolation compatible codepath.
     */
    private fun runBuild(gradleVersion: String? = null, vararg arguments: String): Boolean {
        val runner = GradleRunner.create()
            .withProjectDir(tmpDir.root)
            .withArguments("printKspIsolation", "--stacktrace", *arguments)
        gradleVersion?.let { runner.withGradleVersion(it) }
        val output = runner.build().output
        val reported = output.lineSequence()
            .filter { it.startsWith(ISOLATION_OUTPUT_PREFIX) }
            .map { it.removePrefix(ISOLATION_OUTPUT_PREFIX).trim() }
            .toList()
        check(reported.size == 1) {
            "Expected exactly one line starting with '$ISOLATION_OUTPUT_PREFIX', found $reported in:\n$output"
        }
        return reported.single().toBooleanStrict()
    }

    companion object {
        private const val ISOLATION_OUTPUT_PREFIX = "KSP isolation compatible codepath: "

        /**
         * Gradle 9.7 is the first version with the `--isolated-projects` / `--no-isolated-projects` command line
         * options, and the first one that accepts the `org.gradle.isolated-projects` property name. Before that,
         * Isolated Projects could only be turned on through `org.gradle.unsafe.isolated-projects`.
         */
        private const val MODERN_ISOLATION_GRADLE_VERSION = "9.7.1"
    }
}
