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
import com.google.devtools.ksp.gradle.processor.TestSymbolProcessorProvider
import com.google.devtools.ksp.gradle.testing.DependencyDeclaration.Companion.module
import com.google.devtools.ksp.gradle.testing.KspIntegrationTestRule
import com.google.devtools.ksp.gradle.testing.PluginDeclaration
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
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
    fun configurationsForMultiplatformApp() {
        testRule.setupAppAsMultiplatformApp("""
            kotlin {
                jvm { }
                android(name = "foo") { }
                js { browser() }
                androidNativeX86 { }
                androidNativeX64(name = "bar") { }
            }
        """.trimIndent())
        testRule.appModule.addMultiplatformSource("commonMain", "Foo.kt", "class Foo")
        val result = testRule.runner()
            .withArguments(":app:dependencies")
            .build()

        assertThat(result.output.lines()).containsAtLeast(
            // jvm target:
            "kspJvm",
            "kspJvmTest",
            // android target, named foo:
            "kspFoo",
            "kspFooAndroidTest",
            "kspFooAndroidTestDebug",
            "kspFooAndroidTestRelease",
            "kspFooDebug",
            "kspFooRelease",
            "kspFooTest",
            "kspFooTestDebug",
            "kspFooTestRelease",
            // js target:
            "kspJs",
            "kspJsTest",
            // androidNativeX86 target:
            "kspAndroidNativeX86",
            "kspAndroidNativeX86Test",
            // androidNative64 target, named bar:
            "kspBar",
            "kspBarTest"
        )
    }

    @Test
    fun registerJavaSourcesToAndroid() {
        testRule.setupAppAsAndroidApp()
        testRule.appModule.dependencies.addAll(
            listOf(
                module("ksp", testRule.processorModule),
                module("kspTest", testRule.processorModule),
                module("kspAndroidTest", testRule.processorModule)
            )
        )
        testRule.appModule.buildFileAdditions.add(
            """
            tasks.register("printSources") {
                fun logVariantSources(variants: DomainObjectSet<out com.android.build.gradle.api.BaseVariant>) {
                    variants.all {
                        println("VARIANT:" + this.name)
                        val baseVariant = (this as com.android.build.gradle.internal.api.BaseVariantImpl)
                        val variantData = baseVariant::class.java.getMethod("getVariantData").invoke(baseVariant)
                            as com.android.build.gradle.internal.variant.BaseVariantData
                        variantData.extraGeneratedSourceFolders.forEach {
                            println("SRC:" + it.relativeTo(buildDir).path)
                        }
                        variantData.allPreJavacGeneratedBytecode.forEach {
                            println("BYTE:" + it.relativeTo(buildDir).path)
                        }
                    }
                }
                doLast {
                    logVariantSources(android.applicationVariants)
                    logVariantSources(android.testVariants)
                    logVariantSources(android.unitTestVariants)
                }
            }
            """.trimIndent()
        )
        val result = testRule.runner().withDebug(true).withArguments(":app:printSources").build()

        data class SourceFolder(
            val variantName: String,
            val path: String
        )
        // parse output to get variant names and sources
        // variant name -> list of sources
        val variantSources = mutableListOf<SourceFolder>()
        lateinit var currentVariantName: String
        result.output.lines().forEach { line ->
            when {
                line.startsWith("VARIANT:") -> {
                    currentVariantName = line.substring("VARIANT:".length)
                }
                line.startsWith("SRC:") -> {
                    variantSources.add(
                        SourceFolder(
                            variantName = currentVariantName,
                            path = line
                        )
                    )
                }

                line.startsWith("BYTE:") -> {
                    variantSources.add(
                        SourceFolder(
                            variantName = currentVariantName,
                            path = line
                        )
                    )
                }
            }
        }
        assertThat(
            variantSources.filter {
                // there might be more, we are only interested in ksp
                it.path.contains("ksp")
            }
        ).containsExactly(
            SourceFolder(
                "debug", "SRC:generated/ksp/debug/java"
            ),
            SourceFolder(
                "release", "SRC:generated/ksp/release/java"
            ),
            SourceFolder(
                "debugAndroidTest", "SRC:generated/ksp/debugAndroidTest/java"
            ),
            SourceFolder(
                "debugUnitTest", "SRC:generated/ksp/debugUnitTest/java"
            ),
            SourceFolder(
                "releaseUnitTest", "SRC:generated/ksp/releaseUnitTest/java"
            ),
            // TODO byte sources seems to be overridden by tmp/kotlin-classes/debug
            //  assert them as well once fixed
        )
    }

    @Test
    fun configurationsForAndroidApp_withBuildFlavorsMatchesKapt() {
        testRule.setupAppAsAndroidApp()
        testRule.appModule.buildFileAdditions.add(
            """
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
            """.trimIndent()
        )
        testRule.appModule.plugins.add(PluginDeclaration.kotlin("kapt", testRule.testConfig.kotlinBaseVersion))
        testRule.appModule.addSource("Foo.kt", "class Foo")
        val result = testRule.runner()
            .withArguments(":app:dependencies")
            .build()

        // kaptClasspath_* seem to be intermediate configurations that never run.
        val kaptConfigurations = result.output.lines().filter {
            it.startsWith("kapt") && !it.startsWith("kaptClasspath_")
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

    private fun kspForTests(androidApp: Boolean, useAndroidTest: Boolean) {
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

        testRule.appModule.addSource(
            "App.kt",
            """
            @Suppress("app")
            class InApp {
            }
            """.trimIndent()
        )
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

        class Processor(val codeGenerator: CodeGenerator) : SymbolProcessor {
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

        class Provider : TestSymbolProcessorProvider({ env -> Processor(env.codeGenerator) })

        testRule.addProvider(Provider::class)
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
