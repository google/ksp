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
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GradleCompilationTest {
    @Rule
    @JvmField
    val tmpDir = TemporaryFolder()

    @Rule
    @JvmField
    val testRule = KspIntegrationTestRule(tmpDir)

    @Test
    fun errorMessageFailsCompilation() {
        testRule.setupAppAsJvmApp()
        testRule.appModule.dependencies.add(
            module(configuration = "ksp", testRule.processorModule)
        )
        testRule.appModule.addSource(
            "Foo.kt",
            """
            class Foo {
            }
            """.trimIndent()
        )
        class ErrorReporting(private val logger: KSPLogger) : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                logger.error("my processor failure")
                return emptyList()
            }
        }

        class Provider : TestSymbolProcessorProvider({ _, _, _, logger -> ErrorReporting(logger) })

        testRule.addProvider(Provider::class)
        val failure = testRule.runner()
            .withArguments("app:assemble")
            .buildAndFail()
        assertThat(failure.output).contains("my processor failure")
    }

    @Test
    fun applicationCanAccessGeneratedCode() {
        testRule.setupAppAsJvmApp()
        testRule.appModule.dependencies.add(
            module(configuration = "ksp", testRule.processorModule)
        )
        testRule.appModule.addSource(
            "Foo.kt",
            """
            class Foo {
                val x = ToBeGenerated()
            }
            """.trimIndent()
        )
        testRule.appModule.addSource(
            "JavaSrc.java",
            """
            class JavaSrc {
                ToBeGenerated x;
            }
            """.trimIndent()
        )
        class MyProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
            var count = 0
            override fun process(resolver: Resolver): List<KSAnnotated> {
                if(count == 0) {
                    codeGenerator.createNewFile(Dependencies.ALL_FILES, "", "Generated").use {
                        it.writer(Charsets.UTF_8).use {
                            it.write("class ToBeGenerated")
                        }
                    }
                    count += 1
                }
                return emptyList()
            }
        }

        class Provider : TestSymbolProcessorProvider({ _, _, codeGenerator, _ -> MyProcessor(codeGenerator) })

        testRule.addProvider(Provider::class)

        testRule.runner()
            .withDebug(true)
            .withArguments("app:assemble")
            .forwardOutput()
            .build()
    }
}