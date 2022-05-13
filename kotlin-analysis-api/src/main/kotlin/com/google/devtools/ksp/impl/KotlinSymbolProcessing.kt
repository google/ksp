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
import com.google.devtools.ksp.KspOptions
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileImpl
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.processing.impl.CodeGeneratorImpl
import com.google.devtools.ksp.processing.impl.JvmPlatformInfoImpl
import com.google.devtools.ksp.symbol.KSAnnotated
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.analyseWithCustomToken
import org.jetbrains.kotlin.analysis.api.standalone.configureApplicationEnvironment
import org.jetbrains.kotlin.analysis.api.standalone.configureProjectEnvironment
import org.jetbrains.kotlin.analysis.api.tokens.AlwaysAccessibleValidityTokenFactory
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.javaSourceRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.nio.file.Files

class KotlinSymbolProcessing(
    val compilerConfiguration: CompilerConfiguration,
    val options: KspOptions,
    val logger: KSPLogger,
    val env: KotlinCoreEnvironment,
    val providers: List<SymbolProcessorProvider>
) {
    val project = env.project as MockProject
    val kspCoreEnvironment = KSPCoreEnvironment(project)

    var finished = false
    val deferredSymbols = mutableMapOf<SymbolProcessor, List<KSAnnotated>>()
    val ktFiles = env.getSourceFiles()
    val javaFiles = compilerConfiguration.javaSourceRoots
    lateinit var codeGenerator: CodeGeneratorImpl
    lateinit var processors: List<SymbolProcessor>

    fun prepare() {
        val ksFiles = ktFiles.map { file ->
            analyseWithCustomToken(file, AlwaysAccessibleValidityTokenFactory) {
                KSFileImpl.getCached(file.getFileSymbol())
            }
        }
        val anyChangesWildcard = AnyChanges(options.projectBaseDir)
        codeGenerator = CodeGeneratorImpl(
            options.classOutputDir,
            options.javaOutputDir,
            options.kotlinOutputDir,
            options.resourceOutputDir,
            options.projectBaseDir,
            anyChangesWildcard,
            ksFiles,
            options.incremental
        )
        processors = providers.mapNotNull { provider ->
            var processor: SymbolProcessor? = null
            processor = provider.create(
                SymbolProcessorEnvironment(
                    options.processingOptions,
                    options.languageVersion,
                    codeGenerator,
                    logger,
                    options.apiVersion,
                    options.compilerVersion,
                    // TODO: fix platform info
                    listOf(JvmPlatformInfoImpl("JVM", "1.8"))
                )
            )
            processor.also { deferredSymbols[it] = mutableListOf() }
        }
    }

    fun execute() {
        // TODO: support no kotlin source input.
        val psiManager = PsiManager.getInstance(project)
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val javaSourceRoots = options.javaSourceRoots
        val javaFiles = javaSourceRoots.sortedBy { Files.isSymbolicLink(it.toPath()) } // Get non-symbolic paths first
            .flatMap { root -> root.walk().filter { it.isFile && it.extension == "java" }.toList() }
            .sortedBy { java.nio.file.Files.isSymbolicLink(it.toPath()) } // This time is for .java files
            .distinctBy { it.canonicalPath }
            .mapNotNull { localFileSystem.findFileByPath(it.path)?.let { psiManager.findFile(it) } as? PsiJavaFile }
        val resolver = ResolverAAImpl(
            ktFiles.map {
                analyseWithCustomToken(it, AlwaysAccessibleValidityTokenFactory) { it.getFileSymbol() }
            },
            javaFiles
        )
        ResolverAAImpl.instance = resolver
        processors.forEach { it.process(resolver) }
    }
}

fun main(args: Array<String>) {
    val compilerConfiguration = CompilerConfiguration()
    val commandLineProcessor = KSPCommandLineProcessor(compilerConfiguration)
    val logger = CommandLineKSPLogger()

    val application = ApplicationManager.getApplication() as MockApplication
    configureApplicationEnvironment(application)

    commandLineProcessor.processArgs(args)

    val kotlinCoreEnvironment = KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(), commandLineProcessor.compilerConfiguration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )

    configureProjectEnvironment(
        kotlinCoreEnvironment.project as MockProject,
        compilerConfiguration,
        kotlinCoreEnvironment::createPackagePartProvider,
        kotlinCoreEnvironment.projectEnvironment.environment.jarFileSystem as CoreJarFileSystem
    )

    val kotlinSymbolProcessing = KotlinSymbolProcessing(
        commandLineProcessor.compilerConfiguration,
        commandLineProcessor.kspOptions,
        logger,
        kotlinCoreEnvironment,
        commandLineProcessor.providers
    )
    kotlinSymbolProcessing.prepare()
    kotlinSymbolProcessing.execute()
}
