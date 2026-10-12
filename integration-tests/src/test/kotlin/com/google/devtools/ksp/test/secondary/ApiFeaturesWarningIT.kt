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

package com.google.devtools.ksp.test.secondary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class ApiFeaturesWarningIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "init-plus-provider",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)

        private const val PROCESSOR_NAME = "TestProcessor"

        private const val LEGACY_OPT_IN_WARNING =
            "Processor '$PROCESSOR_NAME' has not opted in for upcoming features yet. " +
                "It might break in a future version of KSP. " +
                "To fix this, please update the processor to a version that is " +
                "compatible with upcoming features."

        private const val REGISTER_LAMBDA_DEPRECATION_WARNING =
            "Note that 'SymbolProcessorEnvironment.registerProcessorForNewFeatures' is deprecated; " +
                "processors should instead migrate to configuring 'SymbolProcessor.apiFeatures'."

        private fun expectedDisabledFeaturesWarning(vararg features: String): String =
            "Processor '$PROCESSOR_NAME' has not enabled the following API features: " +
                "${features.joinToString(", ")}. " +
                "It might break in a future version of KSP. " +
                "To fix this, please configure the set of 'apiFeatures' in 'SymbolProcessorProvider' " +
                "when creating the 'SymbolProcessor' (e.g., using 'buildApiFeatures'). " +
                "Processors should eventually enable all features in 'ApiFeatures'."
    }

    private fun writeProcessorAndMinimalWorkload(processorContent: String) {
        File(project.root, "provider-processor/src/main/kotlin/TestProcessor.kt")
            .writeText(processorContent.trimIndent())
        File(project.root, "workload/src/main/java/com/example/A.kt").writeText(
            """
            package com.example

            fun main() {}
            """.trimIndent()
        )
    }

    private fun configureProcessorApiFeatures(
        enableBackingFields: Boolean,
        enableContextParameters: Boolean
    ) {
        writeProcessorAndMinimalWorkload(
            """
            import com.google.devtools.ksp.ApiFeatures
            import com.google.devtools.ksp.buildApiFeatures
            import com.google.devtools.ksp.processing.*
            import com.google.devtools.ksp.symbol.*

            class TestProcessor(
                override val apiFeatures: ApiFeatures
            ) : SymbolProcessor {

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    return emptyList()
                }

                class Provider : SymbolProcessorProvider {
                    override fun create(
                        environment: SymbolProcessorEnvironment
                    ): SymbolProcessor {
                        val apiFeatures = buildApiFeatures {
                            enableBackingFields = $enableBackingFields
                            enableContextParameters = $enableContextParameters
                        }
                        return TestProcessor(apiFeatures)
                    }
                }
            }
            """
        )
    }

    private fun configureProcessorWithRegisterLambda() {
        writeProcessorAndMinimalWorkload(
            """
            import com.google.devtools.ksp.processing.*
            import com.google.devtools.ksp.symbol.*

            class TestProcessor : SymbolProcessor {

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    return emptyList()
                }

                class Provider : SymbolProcessorProvider {
                    override fun create(
                        environment: SymbolProcessorEnvironment
                    ): SymbolProcessor {
                        val processor = TestProcessor()
                        environment.registerProcessorForNewFeatures(processor)
                        return processor
                    }
                }
            }
            """
        )
    }

    private fun configureProcessorWithoutAnyOptIn() {
        writeProcessorAndMinimalWorkload(
            """
            import com.google.devtools.ksp.processing.*
            import com.google.devtools.ksp.symbol.*

            class TestProcessor : SymbolProcessor {

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    return emptyList()
                }

                class Provider : SymbolProcessorProvider {
                    override fun create(
                        environment: SymbolProcessorEnvironment
                    ): SymbolProcessor = TestProcessor()
                }
            }
            """
        )
    }

    private fun runBuildAndAssertSuccess(): BuildResult {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments("--info", "clean", "build").build()
        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)
        return result
    }

    @Test
    fun testLegacyProcessorWithoutAnyOptInEmitsExactMainWarning() {
        configureProcessorWithoutAnyOptIn()
        val result = runBuildAndAssertSuccess()

        Assert.assertTrue(
            "Expected exact standalone warning line from main when neither registerProcessorForNewFeatures nor ApiFeatures is used",
            result.output.lines().contains("i: [ksp] $LEGACY_OPT_IN_WARNING")
        )
    }

    @Test
    fun testNoFeaturesEnabled() {
        configureProcessorApiFeatures(
            enableBackingFields = false,
            enableContextParameters = false
        )
        val result = runBuildAndAssertSuccess()

        Assert.assertTrue(
            "Expected existing warning about not opting in for upcoming features",
            result.output.lines().contains("i: [ksp] $LEGACY_OPT_IN_WARNING")
        )
        Assert.assertTrue(
            "Expected deprecation warning for registerProcessorForNewFeatures advising migration to apiFeatures",
            result.output.contains(REGISTER_LAMBDA_DEPRECATION_WARNING)
        )
        Assert.assertTrue(
            "Expected warning listing both enableBackingFields and enableContextParameters as disabled",
            result.output.contains(
                expectedDisabledFeaturesWarning("enableBackingFields", "enableContextParameters")
            )
        )
    }

    @Test
    fun testRegisterProcessorForNewFeatures() {
        configureProcessorWithRegisterLambda()
        val result = runBuildAndAssertSuccess()

        Assert.assertFalse(
            "Expected no warning about not opting in via registerProcessorForNewFeatures",
            result.output.contains("has not opted in for upcoming features yet")
        )
        Assert.assertTrue(
            "Expected deprecation warning for registerProcessorForNewFeatures advising migration to apiFeatures",
            result.output.contains(REGISTER_LAMBDA_DEPRECATION_WARNING)
        )
        Assert.assertTrue(
            "Expected warning listing both enableBackingFields and enableContextParameters as disabled",
            result.output.contains(
                expectedDisabledFeaturesWarning("enableBackingFields", "enableContextParameters")
            )
        )
    }

    @Test
    fun testBackingFieldsEnabledOnly() {
        configureProcessorApiFeatures(
            enableBackingFields = true,
            enableContextParameters = false
        )
        val result = runBuildAndAssertSuccess()

        Assert.assertTrue(
            "Expected warning that enableContextParameters is not enabled",
            result.output.contains(expectedDisabledFeaturesWarning("enableContextParameters"))
        )
    }

    @Test
    fun testContextParametersEnabledOnly() {
        configureProcessorApiFeatures(
            enableBackingFields = false,
            enableContextParameters = true
        )
        val result = runBuildAndAssertSuccess()

        Assert.assertTrue(
            "Expected warning that enableBackingFields is not enabled",
            result.output.contains(expectedDisabledFeaturesWarning("enableBackingFields"))
        )
    }

    @Test
    fun testAllFeaturesEnabled() {
        configureProcessorApiFeatures(
            enableBackingFields = true,
            enableContextParameters = true
        )
        val result = runBuildAndAssertSuccess()

        Assert.assertFalse(
            "Expected no warning about upcoming features when all features are enabled",
            result.output.contains("has not opted in for upcoming features yet")
        )
        Assert.assertFalse(
            "Expected no warning about disabled API features when all features are enabled",
            result.output.contains("has not enabled the following API features")
        )
    }
}
