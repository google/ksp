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
package com.google.devtools.ksp.gradle

import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.gradle.testing.TestConfig
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProjectIsolationTest {
    @Rule @JvmField val tmpDir = TemporaryFolder()

    @Test
    fun detectsCommandLineIsolationAndReusesConfigurationCache() {
        writeBuild()
        val runner = runner("9.7.1", "--isolated-projects")
        assertThat(runner.build().output).contains("KSP isolation: true")
        assertThat(runner.build().output).contains("Reusing configuration cache.")
    }

    @Test
    fun commandLineDisablingIsolationOverridesGradleProperty() {
        writeBuild()
        tmpDir.root.resolve("gradle.properties").writeText("org.gradle.isolated-projects=true")
        assertThat(runner("9.7.1", "--no-isolated-projects").build().output)
            .contains("KSP isolation: false")
    }

    @Test
    fun explicitKspOptInStillWorksWhenIsolationIsDisabled() {
        writeBuild()
        assertThat(
                runner("9.7.1", "--no-isolated-projects", "-Pksp.project.isolation.enabled=true")
                    .build()
                    .output
            )
            .contains("KSP isolation: true")
    }

    @Test
    fun supportsGradleBeforeBuildFeaturesWasIntroduced() {
        writeBuild()
        assertThat(runner("8.4").build().output).contains("KSP isolation: false")
        assertThat(runner("8.4", "-Pksp.project.isolation.enabled=true").build().output)
            .contains("KSP isolation: true")
    }

    @Test
    fun detectsLegacyIsolationPropertyThroughBuildFeatures() {
        writeBuild()
        assertThat(runner("8.5", "-Dorg.gradle.unsafe.isolated-projects=true").build().output)
            .contains("KSP isolation: true")
    }

    @Test
    fun compilesGeneratedSourcesWithCommandLineIsolation() {
        writeBuild()
        tmpDir.root.resolve("settings.gradle").appendText("\ninclude('processor', 'app')")
        val processor = tmpDir.root.resolve("processor").also { it.mkdirs() }
        processor
            .resolve("build.gradle")
            .writeText(
                """
            plugins { id 'org.jetbrains.kotlin.jvm' }
            repositories {
                maven { url = uri('${TestConfig.read().mavenRepoPath}') }
                mavenCentral()
            }
            dependencies {
                implementation 'com.google.devtools.ksp:symbol-processing-api:${TestConfig.read().kspVersion}'
            }
            """
                    .trimIndent()
            )
        processor
            .resolve("src/main/kotlin/ProcessorProvider.kt")
            .also { it.parentFile.mkdirs() }
            .writeText(
                """
                import com.google.devtools.ksp.processing.*
                import com.google.devtools.ksp.symbol.KSAnnotated

                class ProcessorProvider : SymbolProcessorProvider {
                    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                        object : SymbolProcessor {
                            private var generated = false
                            override fun process(resolver: Resolver): List<KSAnnotated> {
                                if (!generated) {
                                    environment.codeGenerator.createNewFile(Dependencies(false), "", "Generated")
                                        .writer().use { it.write("class Generated") }
                                    generated = true
                                }
                                return emptyList()
                            }
                        }
                }
                """
                    .trimIndent()
            )
        processor
            .resolve(
                "src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider"
            )
            .also { it.parentFile.mkdirs() }
            .writeText("ProcessorProvider")
        val app = tmpDir.root.resolve("app").also { it.mkdirs() }
        app.resolve("build.gradle")
            .writeText(
                """
            plugins {
                id 'org.jetbrains.kotlin.jvm'
                id 'com.google.devtools.ksp'
            }
            repositories {
                maven { url = uri('${TestConfig.read().mavenRepoPath}') }
                mavenCentral()
            }
            dependencies { ksp project(':processor') }
            """
                    .trimIndent()
            )
        app.resolve("src/main/kotlin/UsesGenerated.kt")
            .also { it.parentFile.mkdirs() }
            .writeText("class UsesGenerated(val generated: Generated)")
        val runner = runner("9.7.1", "app:compileKotlin", "--isolated-projects")
        runner.build()
        assertThat(app.resolve("build/classes/kotlin/main/Generated.class").exists()).isTrue()
        assertThat(app.resolve("build/classes/kotlin/main/UsesGenerated.class").exists()).isTrue()
        assertThat(runner.build().output).contains("Reusing configuration cache.")
    }

    private fun writeBuild() {
        tmpDir.root
            .resolve("settings.gradle")
            .writeText(
                """
            pluginManagement {
                repositories {
                    maven { url = uri('${TestConfig.read().mavenRepoPath}') }
                    gradlePluginPortal()
                }
            }
            rootProject.name = 'isolation'
            """
                    .trimIndent()
            )
        tmpDir.root
            .resolve("build.gradle")
            .writeText(
                """
            import com.google.devtools.ksp.gradle.utils.KgpUtilsKt

            plugins {
                id 'org.jetbrains.kotlin.jvm' version '${TestConfig.read().kotlinBaseVersion}' apply false
                id 'com.google.devtools.ksp' version '${TestConfig.read().kspVersion}' apply false
            }

            def isolated = KgpUtilsKt.enableProjectIsolationCompatibleCodepath(project)
            tasks.register('checkIsolation') {
                inputs.property('isolated', isolated)
                doLast { println("KSP isolation: " + inputs.properties.isolated) }
            }
            """
                    .trimIndent()
            )
    }

    private fun runner(version: String, vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(tmpDir.root)
            .withGradleVersion(version)
            .withArguments("checkIsolation", "--stacktrace", *arguments)
}
