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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.standalone.configureApplicationEnvironment
import org.jetbrains.kotlin.analysis.api.standalone.configureProjectEnvironment
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import java.net.URLClassLoader

class KotlinSymbolProcessing(
    val compilerConfiguration: CompilerConfiguration,
    val options: KspOptions,
    val logger: KSPLogger,
    val testProcessor: SymbolProcessorProvider? = null
) {

    val providers: List<SymbolProcessorProvider>
    val env: KotlinCoreEnvironment = KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(), compilerConfiguration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
    val project = env.project as MockProject
    val kspCoreEnvironment = KSPCoreEnvironment(project)

    var finished = false
    val deferredSymbols = mutableMapOf<SymbolProcessor, List<KSAnnotated>>()
    val ktFiles = convertFilesToKtFiles(project, compilerConfiguration.kotlinSourceRoots.map { it.path })
    val codeGenerator: CodeGeneratorImpl
    val processors: List<SymbolProcessor>

    init {
        configureProjectEnvironment(
            project,
            compilerConfiguration,
            env::createPackagePartProvider,
            env.projectEnvironment.environment.jarFileSystem as CoreJarFileSystem
        )

        val ksFiles = ktFiles.map { KSFileImpl(it) }
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
        val application = ApplicationManager.getApplication() as MockApplication
        configureApplicationEnvironment(application)
        providers = if (testProcessor != null) {
            listOf(testProcessor)
        } else {
            val processingClasspath = options.processingClasspath
            val classLoader =
                URLClassLoader(processingClasspath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)

            ServiceLoaderLite.loadImplementations(SymbolProcessorProvider::class.java, classLoader)
        }

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
        val resolver = ResolverAAImpl(ktFiles)
        processors.forEach { it.process(resolver) }
    }

    private fun convertFilesToKtFiles(project: Project, filePaths: List<String>): List<KtFile> {
        val fs = StandardFileSystems.local()
        val psiManager = PsiManager.getInstance(project)
        val ktFiles = mutableListOf<KtFile>()
        for (path in filePaths) {
            val vFile = fs.findFileByPath(path) ?: continue
            val ktFile = psiManager.findFile(vFile) as? KtFile ?: continue
            ktFiles.add(ktFile)
        }
        return ktFiles
    }
}

fun main(args: Array<String>) {
    val commandLineProcessor = KSPCommandLineProcessor(args)
    val logger = CommandLineKSPLogger()
    val kotlinSymbolProcessing = KotlinSymbolProcessing(
        commandLineProcessor.compilerConfiguration,
        commandLineProcessor.kspOptions,
        logger
    )
    kotlinSymbolProcessing.execute()
}
