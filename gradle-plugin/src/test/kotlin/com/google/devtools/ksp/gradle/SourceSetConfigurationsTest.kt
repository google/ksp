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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class SourceSetConfigurationsTest(val useKSP2: Boolean) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }

    @Rule
    @JvmField
    val tmpDir = TemporaryFolder()

    @Rule
    @JvmField
    val testRule = KspIntegrationTestRule(tmpDir, useKSP2)

    @Test
    fun configurationsForJvmApp() {
        testRule.setupAppAsJvmApp()
        testRule.appModule.addSource("Foo.kt", "class Foo")
        val result = testRule.runner()
            .withArguments(":app:dependencies")
            .build()
        val configurations = result.output.lines().map { it.split(' ').first() }

        assertThat(configurations).containsAtLeast("ksp", "kspTest")
    }

    @Test
    fun configurationsForAndroidApp() {
        testRule.setupAppAsAndroidApp()
        testRule.appModule.addSource("Foo.kt", "class Foo")
        val result = testRule.runner()
            .withArguments(":app:dependencies")
            .build()
        val configurations = result.output.lines().map { it.split(' ').first() }

        assertThat(configurations).containsAtLeast(
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
        testRule.setupAppAsMultiplatformApp(
            """
                kotlin {
                    jvm { }
                    androidTarget(name = "foo") { }
                    js(IR) { browser() }
                    androidNativeX86 { }
                    androidNativeX64(name = "bar") { }
                }
                
            """.trimIndent()
        )
        testRule.appModule.addMultiplatformSource("commonMain", "Foo.kt", "class Foo")
        val result = testRule.runner()
            .withArguments(":app:dependencies")
            .build()
        val configurations = result.output.lines().map { it.split(' ').first() }

        assertThat(configurations).containsAtLeast(
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
    fun configurationsForMultiplatformApp_doesNotCrossCompilationBoundaries() {
        // Adding a ksp dependency on jvmParent should not leak into jvmChild compilation,
        // even if the source sets depend on each other. This works because we use
        // KotlinCompilation.kotlinSourceSets instead of KotlinCompilation.allKotlinSourceSets
        testRule.setupAppAsMultiplatformApp(
            """
                kotlin {
                    jvm("jvmParent") { }
                    jvm("jvmChild") { }
                }
            """.trimIndent()
        )
        testRule.appModule.addMultiplatformSource("commonMain", "Foo.kt", "class Foo")
        testRule.appModule.buildFileAdditions.add(
            """
                kotlin {
                    sourceSets {
                        this["jvmChildMain"].dependsOn(this["jvmParentMain"])
                    }
                }
                dependencies {
                    add("kspJvmParent", "androidx.room:room-compiler:2.4.2")
                }
                tasks.register("checkConfigurations") {
                    doLast {
                        // child has no dependencies, so task is not created.
                        val parent = tasks.findByName("kspKotlinJvmParent")
                        val child = tasks.findByName("kspKotlinJvmChild")
                        require(parent != null)
                        require(child == null)
                    }
                }
            """.trimIndent()
        )
        testRule.runner()
            .withArguments(":app:checkConfigurations")
            .build()
    }

    @Test
    fun registerGeneratedSourcesToAndroid() {
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
                        variantData.extraGeneratedSourceFoldersOnlyInModel.forEach {
                            println("SRC:" + it.relativeTo(layout.buildDirectory.get().asFile).path)
                        }
                        variantData.allPreJavacGeneratedBytecode.forEach {
                            println("BYTE:" + it.relativeTo(layout.buildDirectory.get().asFile).path)
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
        val result = testRule.runner().withArguments(":app:printSources").build()

        data class SourceFolder(
            val variantName: String,
            val path: String
        )

        fun String.normalizePath() = replace(File.separatorChar, '/')
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
                            path = line.normalizePath()
                        )
                    )
                }

                line.startsWith("BYTE:") -> {
                    variantSources.add(
                        SourceFolder(
                            variantName = currentVariantName,
                            path = line.normalizePath()
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
                "debug", "SRC:generated/ksp/debug/kotlin"
            ),
            SourceFolder(
                "release", "SRC:generated/ksp/release/kotlin"
            ),
            SourceFolder(
                "debugAndroidTest", "SRC:generated/ksp/debugAndroidTest/kotlin"
            ),
            SourceFolder(
                "debugUnitTest", "SRC:generated/ksp/debugUnitTest/kotlin"
            ),
            SourceFolder(
                "releaseUnitTest", "SRC:generated/ksp/releaseUnitTest/kotlin"
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
        val configurations = result.output.lines().map { it.split(' ').first() }
        val kaptConfigurations = configurations.filter {
            it.startsWith("kapt") && !it.startsWith("kaptClasspath_")
        }
        val kspConfigurations = configurations.filter {
            it.startsWith("ksp") && !it.endsWith("KotlinProcessorClasspath")
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
