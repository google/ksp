/*
 * Copyright 2021 Google LLC
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
package com.google.devtools.ksp.gradle.testing

import java.io.File

/**
 * Simple wrapper that represents a test project.
 */
class TestProject(
    val rootDir: File,
    val testConfig: TestConfig,
    val useKSP2: Boolean,
) {
    val processorModule = TestModule(
        rootDir.resolve("processor")
    ).also {
        it.plugins.add(PluginDeclaration.kotlin("jvm", testConfig.kotlinBaseVersion))
        it.dependencies.add(
            DependencyDeclaration.artifact(
                "implementation",
                "com.google.devtools.ksp:symbol-processing-api:${testConfig.kspVersion}"
            )
        )
        // add gradle-plugin test classpath as a dependency to be able to load processors.
        testConfig.processorClasspath.split(File.pathSeparatorChar).forEach { path ->
            it.dependencies.add(
                DependencyDeclaration.files("implementation", path.replace(File.separatorChar, '/'))
            )
        }
    }

    val appModule = TestModule(
        rootDir.resolve("app")
    )

    fun writeFiles() {
        writeBuildFile()
        writeSettingsFile()
        writeRootProperties()
        appModule.writeBuildFile()
        processorModule.writeBuildFile()
    }

    private fun writeRootProperties() {
        val contents = """
            
            kotlin.jvm.target.validation.mode=warning
            ksp.useKSP2=$useKSP2
        """.trimIndent()
        rootDir.resolve("gradle.properties").appendText(contents)
    }

    private fun writeSettingsFile() {
        val contents = """
                include("processor")
                include("app")
                pluginManagement {
                    repositories {
                        maven("${testConfig.mavenRepoPath}")
                        gradlePluginPortal()
                        google()
                        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
                    }
                }
        """.trimIndent()
        rootDir.resolve("settings.gradle.kts").writeText(contents)
    }

    fun writeAndroidGradlePropertiesFile() {
        val contents = """
            android.useAndroidX=true
            org.gradle.jvmargs=-Xmx2048M -XX:MaxMetaspaceSize=512m
        """.trimIndent()
        rootDir.resolve("gradle.properties").writeText(contents)
    }

    private fun writeBuildFile() {
        val rootBuildFile = buildString {
            appendLine("plugins {")
            val allPlugins = (processorModule.plugins + appModule.plugins).distinct()
            allPlugins.forEach {
                appendLine("""    ${it.text} version "${it.version}" apply false """)
            }
            appendLine("}")
            appendLine(
                """
            repositories {
                maven("${testConfig.mavenRepoPath}")
                mavenCentral()
                maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
                google()
            }
            configurations.all {
                resolutionStrategy.eachDependency {
                    if (requested.group == "org.jetbrains.kotlin") {
                        useVersion("${testConfig.kotlinBaseVersion}")
                    }
                }
            }
            subprojects {
                repositories {
                    maven("${testConfig.mavenRepoPath}")
                    mavenCentral()
                    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
                    google()
                }
                configurations.all {
                    resolutionStrategy.eachDependency {
                        if (requested.group == "org.jetbrains.kotlin") {
                            useVersion("${testConfig.kotlinBaseVersion}")
                        }
                    }
                }
            }
                """.trimIndent()
            )
        }
        rootDir.resolve("build.gradle.kts").writeText(rootBuildFile)
    }
}
