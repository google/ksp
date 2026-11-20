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
package com.google.devtools.ksp.gradle

import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.gradle.testing.TestConfig
import com.google.devtools.ksp.gradle.utils.MINIMUM_SUPPORTED_GRADLE_VERSION
import groovy.json.JsonSlurper
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.w3c.dom.Element

class VersionCheckTest {
    @Rule @JvmField val tmpDir: TemporaryFolder = TemporaryFolder()

    private val testConfig = TestConfig.read()

    @Test
    fun gradleBelowMinimumFails() {
        writeSingleModuleProject(kgpVersion = MINIMUM_KGP_VERSION)

        val result = runner("8.12.1", "help").buildAndFail()

        assertThat(result.output)
            .contains(
                "The minimum supported Gradle version is ${MINIMUM_SUPPORTED_GRADLE_VERSION.version}. " +
                    "Please upgrade the Gradle version in your project."
            )
    }

    @Test
    fun kgpBelowMinimumFails() {
        writeSingleModuleProject(kgpVersion = "2.2.0")

        val result = runner(MINIMUM_SUPPORTED_GRADLE_VERSION.version, "help").buildAndFail()

        assertThat(result.output)
            .contains(
                "The minimum supported Kotlin Gradle Plugin version is $MINIMUM_KGP_VERSION. " +
                    "Please upgrade the Kotlin Gradle Plugin version in your project."
            )
    }

    @Test
    fun kgpBelowMinimumFailsWhenKspAppliedFirst() {
        writeSingleModuleProject(kgpVersion = "2.2.0", applyKspFirst = true)

        val result = runner(MINIMUM_SUPPORTED_GRADLE_VERSION.version, "help").buildAndFail()

        assertThat(result.output)
            .contains(
                "The minimum supported Kotlin Gradle Plugin version is $MINIMUM_KGP_VERSION. " +
                    "Please upgrade the Kotlin Gradle Plugin version in your project."
            )
    }

    @Test
    fun minimumSupportedGradleAndKgpSucceeds() {
        val gradleFloor = MINIMUM_SUPPORTED_GRADLE_VERSION.version

        tmpDir.root
            .resolve("settings.gradle.kts")
            .writeText(
                """
            pluginManagement {
                repositories {
                    maven("${testConfig.mavenRepoPath}")
                    gradlePluginPortal()
                    mavenCentral()
                    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
                }
            }
            dependencyResolutionManagement {
                repositories {
                    maven("${testConfig.mavenRepoPath}")
                    mavenCentral()
                    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
                }
            }
            rootProject.name = "ksp-floor-check"
            include("processor", "app")
            """
                    .trimIndent()
            )
        tmpDir.root
            .resolve("gradle.properties")
            .writeText("kotlin.jvm.target.validation.mode=warning\n")

        val processorDir = tmpDir.root.resolve("processor").apply { mkdirs() }
        processorDir
            .resolve("build.gradle.kts")
            .writeText(
                """
            plugins {
                kotlin("jvm") version "$MINIMUM_KGP_VERSION"
            }
            dependencies {
                implementation("com.google.devtools.ksp:symbol-processing-api:${testConfig.kspVersion}")
            }
            """
                    .trimIndent()
            )
        processorDir
            .resolve("src/main/kotlin/TestProcessor.kt")
            .apply { parentFile.mkdirs() }
            .writeText(
                """
                import com.google.devtools.ksp.processing.*
                import com.google.devtools.ksp.symbol.KSAnnotated

                class TestProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
                    private var generated = false
                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        if (!generated && resolver.getAllFiles().any { it.fileName == "App.kt" }) {
                            generated = true
                            codeGenerator.createNewFile(Dependencies.ALL_FILES, "", "Generated").use { out ->
                                out.writer().use { it.write("class Generated") }
                            }
                        }
                        return emptyList()
                    }
                }

                class TestProcessorProvider : SymbolProcessorProvider {
                    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                        TestProcessor(environment.codeGenerator)
                }
                """
                    .trimIndent()
            )
        processorDir
            .resolve(
                "src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider"
            )
            .apply { parentFile.mkdirs() }
            .writeText("TestProcessorProvider\n")

        val appDir = tmpDir.root.resolve("app").apply { mkdirs() }
        appDir
            .resolve("build.gradle.kts")
            .writeText(
                """
            plugins {
                kotlin("jvm") version "$MINIMUM_KGP_VERSION"
                id("com.google.devtools.ksp") version "${testConfig.kspVersion}"
            }
            dependencies {
                ksp(project(":processor"))
            }
            """
                    .trimIndent()
            )
        appDir
            .resolve("src/main/kotlin/App.kt")
            .apply { parentFile.mkdirs() }
            .writeText(
                """
                class App {
                    val generated = Generated()
                }
                """
                    .trimIndent()
            )

        val result = runner(gradleFloor, ":app:compileKotlin").build()

        assertThat(result.task(":processor:compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":app:kspKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":app:compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun publishedArtifactsUseKotlinStdlibFloor() {
        val kspGroupDir = testConfig.mavenRepoDir.resolve("com/google/devtools/ksp")
        val expectedFloor = testConfig.kotlinStdlibFloor

        val artifactsWithPom =
            listOf(
                "symbol-processing-api",
                "symbol-processing-common-deps",
                "symbol-processing-aa",
                "symbol-processing-aa-embeddable",
            )
        for (artifact in artifactsWithPom) {
            val versionDir = kspGroupDir.resolve("$artifact/${testConfig.kspVersion}")
            val pomFile =
                versionDir.listFiles()?.filter { it.extension == "pom" }?.maxByOrNull { it.name }
            checkNotNull(pomFile) { "No .pom file found in $versionDir" }
            val stdlibVersions = readKotlinStdlibVersionsFromPom(pomFile.readText())
            assertThat(stdlibVersions).containsExactly(expectedFloor)
        }

        val artifactsWithModuleMetadata =
            listOf(
                "symbol-processing-api",
                "symbol-processing-common-deps",
            )
        for (artifact in artifactsWithModuleMetadata) {
            val versionDir = kspGroupDir.resolve("$artifact/${testConfig.kspVersion}")
            val moduleFile =
                versionDir.listFiles()?.filter { it.extension == "module" }?.maxByOrNull { it.name }
            checkNotNull(moduleFile) { "No .module file found in $versionDir" }
            val stdlibVersions = readKotlinStdlibVersionsFromModuleMetadata(moduleFile.readText())
            assertThat(stdlibVersions).isNotEmpty()
            assertThat(stdlibVersions.toSet()).containsExactly(expectedFloor)
        }
    }

    private fun writeSingleModuleProject(kgpVersion: String, applyKspFirst: Boolean = false) {
        tmpDir.root
            .resolve("settings.gradle.kts")
            .writeText(
                """
            pluginManagement {
                repositories {
                    maven("${testConfig.mavenRepoPath}")
                    gradlePluginPortal()
                    mavenCentral()
                    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
                }
            }
            rootProject.name = "ksp-version-check"
            """
                    .trimIndent()
            )
        val kotlinPlugin = """id("org.jetbrains.kotlin.jvm") version "$kgpVersion""""
        val kspPlugin = """id("com.google.devtools.ksp") version "${testConfig.kspVersion}""""
        val orderedPlugins =
            if (applyKspFirst) "$kspPlugin\n    $kotlinPlugin" else "$kotlinPlugin\n    $kspPlugin"
        tmpDir.root
            .resolve("build.gradle.kts")
            .writeText(
                """
            plugins {
                $orderedPlugins
            }
            """
                    .trimIndent()
            )
    }

    private fun runner(gradleVersion: String, vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(tmpDir.root)
            .withGradleVersion(gradleVersion)
            .withArguments(*arguments, "--stacktrace")

    private fun readKotlinStdlibVersionsFromPom(pomXml: String): List<String> {
        val doc =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(pomXml.byteInputStream())
        val dependencies = doc.getElementsByTagName("dependency")
        return buildList {
            for (i in 0 until dependencies.length) {
                val dep = dependencies.item(i) as Element
                val groupId = dep.getElementsByTagName("groupId").item(0)?.textContent
                val artifactId = dep.getElementsByTagName("artifactId").item(0)?.textContent
                if (groupId == "org.jetbrains.kotlin" && artifactId == "kotlin-stdlib") {
                    add(dep.getElementsByTagName("version").item(0).textContent)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun readKotlinStdlibVersionsFromModuleMetadata(moduleJson: String): List<String> {
        val root = JsonSlurper().parseText(moduleJson) as Map<String, Any?>
        val variants = (root["variants"] as? List<Map<String, Any?>>).orEmpty()
        return variants.flatMap { variant ->
            val deps = (variant["dependencies"] as? List<Map<String, Any?>>).orEmpty()
            deps.mapNotNull { dep ->
                if (dep["group"] == "org.jetbrains.kotlin" && dep["module"] == "kotlin-stdlib") {
                    val versionObj = dep["version"] as? Map<String, Any?>
                    versionObj?.get("requires") as? String
                } else {
                    null
                }
            }
        }
    }

    companion object {
        private const val MINIMUM_KGP_VERSION = "2.2.10"
    }
}
