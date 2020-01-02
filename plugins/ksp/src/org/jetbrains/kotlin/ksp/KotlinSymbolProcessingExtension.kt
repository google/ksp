/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ksp.processing.SymbolProcessor
import org.jetbrains.kotlin.ksp.processing.impl.CodeGeneratorImpl
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.processor.AbstractTestProcessor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File
import java.net.URLClassLoader

class KotlinSymbolProcessingExtension(
    options: KspOptions,
    val testProcessor: AbstractTestProcessor? = null
) : AbstractKotlinSymbolProcessingExtension(options, testProcessor != null) {
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

abstract class AbstractKotlinSymbolProcessingExtension(val options: KspOptions, val testMode: Boolean) : AnalysisHandlerExtension {
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

        val resolver = ResolverImpl(module, files, bindingTrace, componentProvider)
        val targetDir = options.sourcesOutputDir
        val codeGen = CodeGeneratorImpl(targetDir)

        val processors = loadProcessors()
        processors.forEach {
            it.init(mapOf(), KotlinVersion.CURRENT, codeGen)
        }
        processors.forEach {
            it.process(resolver)
        }
        processors.forEach {
            it.finish()
        }

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
            listOf(options.sourcesOutputDir),
            listOf(options.sourcesOutputDir),
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
