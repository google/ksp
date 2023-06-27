/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.AnyChanges
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileImpl
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.processing.impl.CodeGeneratorImpl
import com.google.devtools.ksp.processing.impl.JvmPlatformInfoImpl
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.toKotlinVersion
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.JvmFirDeserializedSymbolProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.createSourceFilesFromSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings

class KotlinSymbolProcessing(
    val kspConfig: KSPJvmConfig,
) {
    fun execute() {
        val deferredSymbols = mutableMapOf<SymbolProcessor, List<KSAnnotated>>()
        val providers: List<SymbolProcessorProvider> = kspConfig.processorProviders

        // TODO: CompilerConfiguration is deprecated.
        val compilerConfiguration: CompilerConfiguration = CompilerConfiguration().apply {
            addKotlinSourceRoots(kspConfig.sourceRoots.map { it.path })
            addJavaSourceRoots(kspConfig.javaSourceRoots)
            addJvmClasspathRoots(kspConfig.libraries)
            put(CommonConfigurationKeys.MODULE_NAME, kspConfig.moduleName)
            kspConfig.jdkHome?.let {
                put(JVMConfigurationKeys.JDK_HOME, it)
            }
            val languageVersion = LanguageVersion.fromFullVersionString(kspConfig.languageVersion)!!
            val apiVersion = LanguageVersion.fromFullVersionString(kspConfig.apiVersion)!!
            languageVersionSettings = LanguageVersionSettingsImpl(
                languageVersion,
                ApiVersion.createByLanguageVersion(apiVersion)
            )
        }

        val analysisAPISession = buildStandaloneAnalysisAPISession(withPsiDeclarationFromBinaryModuleProvider = true) {
            CoreApplicationEnvironment.registerExtensionPoint(
                project.extensionArea,
                KtResolveExtensionProvider.EP_NAME.name,
                KtResolveExtensionProvider::class.java
            )
            buildKtModuleProviderByCompilerConfiguration(compilerConfiguration)
        }.apply {
            (project as MockProject).registerService(
                JvmFirDeserializedSymbolProviderFactory::class.java,
                JvmFirDeserializedSymbolProviderFactory::class.java
            )
        }

        val kspCoreEnvironment = KSPCoreEnvironment(analysisAPISession.project as MockProject)

        val ktFiles = createSourceFilesFromSourceRoots(
            compilerConfiguration, analysisAPISession.project, compilerConfiguration.kotlinSourceRoots
        ).toSet().toList()

        // TODO: support no Kotlin source mode.
        ResolverAAImpl.ktModule = ktFiles.first().let {
            analysisAPISession.project.getService(ProjectStructureProvider::class.java)
                .getModule(it, null)
        }
        val ksFiles = ktFiles.map { file ->
            analyze { KSFileImpl.getCached(file.getFileSymbol()) }
        }
        val anyChangesWildcard = AnyChanges(kspConfig.projectBaseDir)
        val codeGenerator = CodeGeneratorImpl(
            kspConfig.classOutputDir,
            { kspConfig.javaOutputDir },
            kspConfig.kotlinOutputDir,
            kspConfig.resourceOutputDir,
            kspConfig.projectBaseDir,
            anyChangesWildcard,
            ksFiles,
            kspConfig.incremental
        )
        val processors = providers.mapNotNull { provider ->
            var processor: SymbolProcessor? = null
            processor = provider.create(
                SymbolProcessorEnvironment(
                    kspConfig.processorOptions,
                    kspConfig.languageVersion.toKotlinVersion(),
                    codeGenerator,
                    kspConfig.logger,
                    kspConfig.apiVersion.toKotlinVersion(),
                    // TODO: compilerVersion
                    KotlinVersion.CURRENT,
                    // TODO: fix platform info
                    listOf(JvmPlatformInfoImpl("JVM", "1.8", "disable"))
                )
            )
            processor.also { deferredSymbols[it] = mutableListOf() }
        }
        // TODO: support no kotlin source input.
        val resolver = ResolverAAImpl(
            ktFiles.map {
                analyze { it.getFileSymbol() }
            },
            kspConfig,
            analysisAPISession.project
        )
        ResolverAAImpl.instance = resolver

        // TODO: multiple rounds
        processors.forEach { it.process(resolver) }
    }
}
