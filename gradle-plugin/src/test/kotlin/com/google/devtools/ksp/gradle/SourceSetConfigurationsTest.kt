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
import com.google.devtools.ksp.gradle.testing.DependencyDeclaration.Companion.module
import com.google.devtools.ksp.gradle.processor.TestSymbolProcessor
import com.google.devtools.ksp.gradle.testing.KspIntegrationTestRule
import com.google.devtools.ksp.gradle.testing.PluginDeclaration
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SourceSetConfigurationsTest {
    @Rule
    @JvmField
    val tmpDir = TemporaryFolder()

    @Rule
    @JvmField
    val testRule = KspIntegrationTestRule(tmpDir)

    @Test
    fun configurationsForJvmApp() {
        testRule.setupAppAsJvmApp()
        testRule.appModule.addSource("Foo.kt", "class Foo")
        val result = testRule.runner()
            .withArguments(":app:dependencies")
            .build()

        assertThat(result.output.lines()).containsAtLeast("ksp", "kspTest")
    }

    @Test
    fun configurationsForAndroidApp() {
        testRule.setupAppAsAndroidApp()
        testRule.appModule.addSource("Foo.kt", "class Foo")
        val result = testRule.runner()
            .withArguments(":app:dependencies")
            .build()

        assertThat(result.output.lines()).containsAtLeast(
            "ksp",
            "kspAndroidTest",
            "kspAndroidTestDebug",
            "kspAndroidTestRelease",
            "kspDebug",
            "kspRelease",
            "kspTest",
            "kspTestDebug",
            "kspTestRelease"
        )
    }

    @Test
    fun configurationsForAndroidApp_withBuildFlavorsMatchesKapt() {
        testRule.setupAppAsAndroidApp()
        testRule.appModule.buildFileAdditions.add("""
            android {
                flavorDimensions("version")
                productFlavors {
                    create("free") {
                        dimension = "version"
                        applicationId = "foo.bar"
                    }
                    create("paid") {
                        dimension = "version"
                        applicationId = "foo.baz"
                    }
                }
            }
        """.trimIndent())
        testRule.appModule.plugins.add(PluginDeclaration.kotlin("kapt", testRule.testConfig.kotlinBaseVersion))
        testRule.appModule.addSource("Foo.kt", "class Foo")
        val result = testRule.runner()
            .withArguments(":app:dependencies")
            .build()

        val kaptConfigurations = result.output.lines().filter {
            it.startsWith("kapt")
        }
        val kspConfigurations = result.output.lines().filter {
            it.startsWith("ksp")
        }
        assertThat(kspConfigurations).containsExactlyElementsIn(
            kaptConfigurations.map {
                it.replace("kapt", "ksp")
            }
        )
        assertThat(kspConfigurations).isNotEmpty()
    }

    @Test
    fun kspForTests_jvm() {
        kspForTests(androidApp = false, useAndroidTest = false)
    }

    @Test
    fun kspForTests_android_androidTest() {
        kspForTests(androidApp = true, useAndroidTest = true)
    }

    @Test
    fun kspForTests_android_junit() {
        kspForTests(androidApp = true, useAndroidTest = false)
    }

    private fun kspForTests(androidApp:Boolean, useAndroidTest: Boolean) {
        if (androidApp) {
            testRule.setupAppAsAndroidApp()
        } else {
            testRule.setupAppAsJvmApp()
        }
        if (useAndroidTest) {
            check(androidApp) {
                "cannot set use android test w/o android app"
            }
        }

        testRule.appModule.addSource("App.kt", """
            @Suppress("app")
            class InApp {
            }
        """.trimIndent())
        val testSource = """
                @Suppress("test")
                class InTest {
                    val impl = InTest_Impl()
                }
                """.trimIndent()
        if (useAndroidTest) {
            testRule.appModule.addAndroidTestSource("InTest.kt", testSource)
        } else {
            testRule.appModule.addTestSource("InTest.kt", testSource)
        }

        class Processor : TestSymbolProcessor() {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                resolver.getSymbolsWithAnnotation(Suppress::class.qualifiedName!!)
                    .filterIsInstance<KSClassDeclaration>()
                    .forEach {
                        if (it.simpleName.asString() == "InApp") {
                            error("should not run on the app sources")
                        }
                        val genClassName = "${it.simpleName.asString()}_Impl"
                        codeGenerator.createNewFile(Dependencies.ALL_FILES, "", genClassName).use {
                            it.writer().use {
                                it.write("class $genClassName")
                            }
                        }
                    }
                return emptyList()
            }
        }
        testRule.addProcessor(Processor::class)
        if (useAndroidTest) {
            testRule.appModule.dependencies.add(
                module("kspAndroidTest", testRule.processorModule)
            )
            testRule.runner().withArguments(":processor:assemble", ":app:assembleAndroidTest", "--stacktrace").build()
        } else {
            testRule.appModule.dependencies.add(
                module("kspTest", testRule.processorModule)
            )
            testRule.runner().withArguments(":app:test", "--stacktrace").build()
        }
    }
}
