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
package com.google.devtools.ksp.gradle.testing

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.reflect.KClass

/**
 * JUnit test rule to setup a [TestProject] which contains a KSP processor module and an
 * application. The application can either be an android app or jvm app.
 * Test must call [setupAppAsAndroidApp] or [setupAppAsJvmApp] before using the [runner].
 */
class KspIntegrationTestRule(
    private val tmpFolder: TemporaryFolder,
) : TestWatcher() {
    /**
     * Initialized when the test starts.
     */
    private lateinit var testProject: TestProject

    /**
     * The application module in the test project
     */
    val appModule
        get() = testProject.appModule

    /**
     * The processor module in the test project
     */
    val processorModule
        get() = testProject.processorModule

    /**
     * The configuration passed from the KSP's main build which includes important setup information
     * like KSP version, local maven repo etc.
     */
    val testConfig = TestConfig.read()

    /**
     * Returns a gradle runner that is ready to run tasks on the test project.
     */
    fun runner(): GradleRunner {
        testProject.writeFiles()
        return GradleRunner.create()
            .withProjectDir(testProject.rootDir)
    }

    /**
     * Adds the given [SymbolProcessorProvider] to the list of providers in the processor module.
     * The processors built with these providers will run on the test application.
     *
     * The passed argument must be a class with a name (e.g. not inline) as it will be added to
     * the classpath of the processor and will be re-loaded when the test runs. For this reason,
     * these classes cannot access to the rest of the test instance.
     */
    fun addProvider(provider: KClass<out SymbolProcessorProvider>) {
        val qName = checkNotNull(provider.java.name) {
            "Must provide a class that can be loaded by qualified name"
        }
        testProject.processorModule.kspServicesFile.appendText("$qName\n")
    }

    /**
     * Sets up the app module as a jvm app, adding necessary plugin dependencies.
     */
    fun setupAppAsJvmApp() {
        testProject.appModule.plugins.addAll(
            listOf(
                PluginDeclaration.kotlin("jvm", testConfig.kotlinBaseVersion),
                PluginDeclaration.id("com.google.devtools.ksp", testConfig.kspVersion)
            )
        )
    }

    /**
     * Sets up the app module as an android app, adding necessary plugin dependencies, a manifest
     * file and necessary gradle configuration. If [enableAgpBuiltInKotlinSupport] is true, enable AGP's built-in Kotlin
     * support instead of applying the Kotlin Android Gradle plugin. If [applyKspPluginFirst] is true, apply the KSP
     * plugin first.
     */
    fun setupAppAsAndroidApp(
        enableAgpBuiltInKotlinSupport: Boolean = false,
        applyKspPluginFirst: Boolean = false
    ) {
        testProject.appModule.plugins.addAll(
            if (applyKspPluginFirst) {
                listOf(
                    PluginDeclaration.id("com.google.devtools.ksp", testConfig.kspVersion),
                    PluginDeclaration.id("com.android.application", testConfig.androidBaseVersion)
                )
            } else {
                listOf(
                    PluginDeclaration.id("com.android.application", testConfig.androidBaseVersion),
                    PluginDeclaration.id("com.google.devtools.ksp", testConfig.kspVersion)
                )
            }
        )
        if (enableAgpBuiltInKotlinSupport) {
            testProject.appModule
                .plugins
                .add(PluginDeclaration.id("com.android.experimental.built-in-kotlin", testConfig.androidBaseVersion))
        } else {
            testProject.appModule.plugins.add(PluginDeclaration.kotlin("android", testConfig.kotlinBaseVersion))
        }
        addAndroidBoilerplate()
    }

    /**
     * Sets up the app module as a multiplatform app with the specified [targets], wrapped in a kotlin { } block.
     */
    fun setupAppAsMultiplatformApp(targets: String) {
        testProject.appModule.plugins.addAll(
            listOf(
                PluginDeclaration.id("com.android.application", testConfig.androidBaseVersion),
                PluginDeclaration.kotlin("multiplatform", testConfig.kotlinBaseVersion),
                PluginDeclaration.id("com.google.devtools.ksp", testConfig.kspVersion)
            )
        )
        testProject.appModule.buildFileAdditions.add(targets)
        addAndroidBoilerplate()
    }

    private fun addAndroidBoilerplate() {
        testProject.writeAndroidGradlePropertiesFile()
        testProject.appModule.buildFileAdditions.add(
            """
            android {
                namespace = "com.example.kspandroidtestapp"
                compileSdk = 34
                defaultConfig {
                    minSdk = 24
                }
            }
            
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:${testConfig.kotlinBaseVersion}")
            }
            """.trimIndent()
        )
        testProject.appModule.moduleRoot.resolve("src/main/AndroidManifest.xml")
            .also {
                it.parentFile.mkdirs()
            }.writeText(
                """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest />
                """.trimIndent()
            )
    }

    override fun starting(description: Description) {
        super.starting(description)
        testProject = TestProject(tmpFolder.newFolder(), testConfig)
    }
}
