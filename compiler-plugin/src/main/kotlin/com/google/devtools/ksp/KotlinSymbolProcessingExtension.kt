/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.impl.CodeGeneratorImpl
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.processor.AbstractTestProcessor
import com.google.devtools.ksp.symbol.impl.KSObjectCacheManager
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.net.URLClassLoader
import java.nio.file.Files

class KotlinSymbolProcessingExtension(
    options: KspOptions,
    logger: KSPLogger,
    val testProcessor: AbstractTestProcessor? = null
) : AbstractKotlinSymbolProcessingExtension(options, logger, testProcessor != null) {
    override fun loadProcessors(): List<SymbolProcessor> {
        return if (testProcessor != null) {
            listOf(testProcessor)
        } else {
            val processingClasspath = options.processingClasspath
            val classLoader = URLClassLoader(processingClasspath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)
            ServiceLoaderLite.loadImplementations(SymbolProcessor::class.java, classLoader)
        }
    }
}

abstract class AbstractKotlinSymbolProcessingExtension(val options: KspOptions, val logger: KSPLogger, val testMode: Boolean) :
    AnalysisHandlerExtension {
    private var completed = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (completed)
            return null

        val psiManager = PsiManager.getInstance(project)
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val javaFiles = options.javaSourceRoots
            .sortedBy { Files.isSymbolicLink(it.toPath()) } // Get non-symbolic paths first
            .flatMap { root -> root.walk().filter { it.isFile && it.extension == "java" }.toList() }
            .sortedBy { Files.isSymbolicLink(it.toPath()) } // This time is for .java files
            .distinctBy { it.canonicalPath }
            .mapNotNull { localFileSystem.findFileByPath(it.path)?.let { psiManager.findFile(it) } as? PsiJavaFile }
        val resolver = ResolverImpl(module, files, javaFiles, bindingTrace, project, componentProvider)
        val codeGen = CodeGeneratorImpl(
            options.classOutputDir,
            options.javaOutputDir,
            options.kotlinOutputDir,
            options.resourceOutputDir
        )

        val processors = loadProcessors()
        processors.forEach {
            it.init(options.processingOptions, KotlinVersion.CURRENT, codeGen, logger)
        }
        processors.forEach {
            it.process(resolver)
        }
        processors.forEach {
            it.finish()
        }

        KSObjectCacheManager.clear()

        return AnalysisResult.success(BindingContext.EMPTY, module, shouldGenerateCode = false)
    }

    abstract fun loadProcessors(): List<SymbolProcessor>

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (completed)
            return null

        completed = true

        if (testMode)
            return null

        return AnalysisResult.RetryWithAdditionalRoots(
            BindingContext.EMPTY,
            module,
            listOf(options.javaOutputDir),
            listOf(options.kotlinOutputDir),
            addToEnvironment = true
        )
    }

    private var annotationProcessingComplete = false

    private fun setAnnotationProcessingComplete(): Boolean {
        if (annotationProcessingComplete) return true

        annotationProcessingComplete = true
        return false
    }
}
