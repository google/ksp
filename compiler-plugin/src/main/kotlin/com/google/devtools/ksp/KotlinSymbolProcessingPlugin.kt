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

package com.google.devtools.ksp

import com.google.devtools.ksp.processing.impl.MessageCollectorBasedKSPLogger
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeListener
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension

private val KSP_OPTIONS = CompilerConfigurationKey.create<KspOptions.Builder>("Ksp options")

@ExperimentalCompilerApi
class KotlinSymbolProcessingCommandLineProcessor : CommandLineProcessor {
    override val pluginId = "com.google.devtools.ksp.symbol-processing"

    override val pluginOptions: Collection<AbstractCliOption> = KspCliOption.values().asList()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        if (option !is KspCliOption) {
            throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }

        val kspOptions = configuration[KSP_OPTIONS]
            ?: KspOptions.Builder().also { configuration.put(KSP_OPTIONS, it) }
        kspOptions.processOption(option, value)
    }
}

// Changes here may break some third party libraries like Kotlin Compile Testing, where the compiler is invoked in
// another way. Do our best to notify them when changing this.
//
// Third party libraries:
//   https://github.com/tschuchortdev/kotlin-compile-testing
@Suppress("DEPRECATION")
@ExperimentalCompilerApi
class KotlinSymbolProcessingComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        // KSP 1.x don't and will not support K2. Do not register if language version >= 2.
        if (configuration.languageVersionSettings.languageVersion >= LanguageVersion.KOTLIN_2_0)
            return

        val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
        val options = configuration[KSP_OPTIONS]?.apply {
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
            languageVersionSettings = configuration.languageVersionSettings
            compilerVersion = KotlinCompilerVersion.getVersion().toKotlinVersion()
        }?.build() ?: return
        val messageCollector = configuration.get(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY)
            ?: throw IllegalStateException("ksp: message collector not found!")
        val wrappedMessageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: throw IllegalStateException("ksp: message collector not found!")
        val logger = MessageCollectorBasedKSPLogger(
            messageCollector, wrappedMessageCollector, options.allWarningsAsErrors
        )
        if (options.processingClasspath.isNotEmpty()) {
            if (options.withCompilation && options.incremental) {
                throw IllegalStateException("ksp: `incremental` is incompatible with `withCompilation`.")
            }
            val kotlinSymbolProcessingHandlerExtension = KotlinSymbolProcessingExtension(options, logger)
            AnalysisHandlerExtension.registerExtension(project, kotlinSymbolProcessingHandlerExtension)
            configuration.put(CommonConfigurationKeys.LOOKUP_TRACKER, DualLookupTracker())

            // Dummy extension point; Required by dropPsiCaches().
            CoreApplicationEnvironment.registerExtensionPoint(
                project.extensionArea, PsiTreeChangeListener.EP.name, PsiTreeChangeAdapter::class.java
            )
        }
    }

    // FirKotlinToJvmBytecodeCompiler throws an error when it sees an incompatible plugin.
    override val supportsK2: Boolean
        get() = true
}
