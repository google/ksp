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
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.processing.impl.CodeGeneratorImpl
import com.google.devtools.ksp.processing.impl.MessageCollectorBasedKSPLogger
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.processor.AbstractTestProcessor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCacheManager
import com.google.devtools.ksp.symbol.impl.java.KSFileJavaImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSFileImpl
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLClassLoader
import java.nio.file.Files

class KotlinSymbolProcessingExtension(
    options: KspOptions,
    logger: KSPLogger,
    val testProcessor: AbstractTestProcessor? = null
) : AbstractKotlinSymbolProcessingExtension(options, logger, testProcessor != null) {
    override fun loadProviders(): List<SymbolProcessorProvider> {
        if (!initialized) {
            providers = if (testProcessor != null) {
                listOf(testProcessor)
            } else {
                val processingClasspath = options.processingClasspath
                val classLoader = URLClassLoader(processingClasspath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)

                ServiceLoaderLite.loadImplementations(SymbolProcessorProvider::class.java, classLoader)
                    .plus(loadProvidersForLegacySymbolProcessors(classLoader))
            }
        }
        return providers
    }

    private fun loadProvidersForLegacySymbolProcessors(classLoader: URLClassLoader): List<SymbolProcessorProvider> {
        return ServiceLoaderLite
            .loadImplementations(SymbolProcessor::class.java, classLoader)
            .map(::LegacySymbolProcessorAdapter)
    }
}

abstract class AbstractKotlinSymbolProcessingExtension(val options: KspOptions, val logger: KSPLogger, val testMode: Boolean) :
    AnalysisHandlerExtension {
    var initialized = false
    var finished = false
    val deferredSymbols = mutableMapOf<SymbolProcessor, List<KSAnnotated>>()
    lateinit var providers: List<SymbolProcessorProvider>
    lateinit var processors: List<SymbolProcessor>
    lateinit var newFiles: Collection<KSFile>
    lateinit var incrementalContext: IncrementalContext
    lateinit var dirtyFiles: Set<KSFile>
    lateinit var cleanFilenames: Set<String>
    lateinit var codeGenerator: CodeGeneratorImpl
    var rounds = 0

    companion object {
        private const val KSP_PACKAGE_NAME = "com.google.devtools.ksp"
        private const val MULTI_ROUND_THRESHOLD = 100
    }

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        rounds++
        if (rounds > MULTI_ROUND_THRESHOLD) {
            logger.warn("Current processing rounds exceeds 100, check processors for potential infinite rounds")
        }
        val psiManager = PsiManager.getInstance(project)
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val javaFiles = options.javaSourceRoots
            .sortedBy { Files.isSymbolicLink(it.toPath()) } // Get non-symbolic paths first
            .flatMap { root -> root.walk().filter { it.isFile && it.extension == "java" }.toList() }
            .sortedBy { Files.isSymbolicLink(it.toPath()) } // This time is for .java files
            .distinctBy { it.canonicalPath }
            .mapNotNull { localFileSystem.findFileByPath(it.path)?.let { psiManager.findFile(it) } as? PsiJavaFile }

        val anyChangesWildcard = AnyChanges(options.projectBaseDir)
        val ksFiles = files.map { KSFileImpl.getCached(it) } + javaFiles.map { KSFileJavaImpl.getCached(it) }

        if (!initialized) {
            incrementalContext = IncrementalContext(
                    options, componentProvider,
                    File(anyChangesWildcard.filePath).relativeTo(options.projectBaseDir)
            )
            dirtyFiles = incrementalContext.calcDirtyFiles(ksFiles).toSet()
            cleanFilenames = ksFiles.filterNot { it in dirtyFiles }.map { it.filePath }.toSet()
            newFiles = dirtyFiles
        } else {
            incrementalContext.registerGeneratedFiles(newFiles)
        }

        // dirtyFiles cannot be reused because they are created in the old container.
        val resolver = ResolverImpl(module, ksFiles.filterNot { it.filePath in cleanFilenames }, newFiles, deferredSymbols, bindingTrace, project, componentProvider, incrementalContext)

        val providers = loadProviders()
        if (!initialized) {
            codeGenerator = CodeGeneratorImpl(
                    options.classOutputDir,
                    options.javaOutputDir,
                    options.kotlinOutputDir,
                    options.resourceOutputDir,
                    options.projectBaseDir,
                    anyChangesWildcard,
                    ksFiles
            )
            processors = providers.mapNotNull { provider ->
                var processor: SymbolProcessor? = null
                handleException {
                    processor = provider.create(options.processingOptions, KotlinVersion.CURRENT, codeGenerator, logger)
                }?.let { analysisResult -> return@doAnalysis analysisResult }
                if (logger.hasError()) {
                    return@mapNotNull null
                }
                processor?.also { deferredSymbols[it] = mutableListOf() }
            }
            initialized = true
        }
        if (!logger.hasError()) {
            processors.forEach processing@{ processor ->
                handleException {
                    deferredSymbols[processor] = processor.process(resolver).filter { it.origin == Origin.KOTLIN || it.origin == Origin.JAVA }
                }?.let { return it }
                if (logger.hasError()) {
                    return@processing
                }
                if (!deferredSymbols.containsKey(processor) || deferredSymbols[processor]!!.isEmpty()) {
                    deferredSymbols.remove(processor)
                }
            }
        }
        // Post processing.
        val newKtFiles = codeGenerator.generatedFile.filter { it.extension == "kt" }
                .mapNotNull { localFileSystem.findFileByPath(it.canonicalPath)?.let { psiManager.findFile(it) } as? KtFile }
        val newJavaFiles = codeGenerator.generatedFile.filter { it.extension == "java" }
                .mapNotNull { localFileSystem.findFileByPath(it.canonicalPath)?.let { psiManager.findFile(it) } as? PsiJavaFile}
        newFiles = newKtFiles.map { KSFileImpl.getCached(it) } + newJavaFiles.map { KSFileJavaImpl.getCached(it) }
        if (codeGenerator.generatedFile.isEmpty()) {
            finished = true
        }
        KSObjectCacheManager.clear()
        codeGenerator.closeFiles()
        if (logger.hasError()) {
            finished = true
            processors.forEach { processor ->
                handleException {
                    processor.onError()
                }?.let { return it }
            }
        } else {
            if (finished) {
                processors.forEach { processor ->
                    handleException {
                        processor.finish()
                    }?.let { return it }
                }
                if (deferredSymbols.isNotEmpty()) {
                    deferredSymbols.map { entry -> logger.warn("Unable to process:${entry.key::class.qualifiedName}:   ${entry.value.map { it.toString() }.joinToString(";")}") }
                }
                if (!logger.hasError()) {
                    incrementalContext.updateCachesAndOutputs(dirtyFiles, codeGenerator.outputs, codeGenerator.sourceToOutputs)
                }
            }
        }
        if (finished) {
            logger.reportAll()
        }
        return if (finished) {
            if (logger.hasError())
                AnalysisResult.compilationError(BindingContext.EMPTY)
            else
                AnalysisResult.success(BindingContext.EMPTY, module, shouldGenerateCode = false)
        } else {
            AnalysisResult.RetryWithAdditionalRoots(BindingContext.EMPTY, module, listOf(options.javaOutputDir), listOf(options.kotlinOutputDir), listOf(options.classOutputDir))
        }
    }

    abstract fun loadProviders(): List<SymbolProcessorProvider>

    private var annotationProcessingComplete = false

    private fun setAnnotationProcessingComplete(): Boolean {
        if (annotationProcessingComplete) return true

        annotationProcessingComplete = true
        return false
    }

    private fun KSPLogger.reportAll() {
        (this as MessageCollectorBasedKSPLogger).reportAll()
    }

    private fun KSPLogger.hasError(): Boolean {
        return (this as MessageCollectorBasedKSPLogger).recordedEvents.any { it.severity == CompilerMessageSeverity.ERROR || it.severity == CompilerMessageSeverity.EXCEPTION }
    }

    private fun handleException(call: () -> Unit): AnalysisResult? {
        try {
            call()
        } catch (e: Exception) {
            // Throws KSP exceptions
            if (e.stackTrace.first().className.startsWith(KSP_PACKAGE_NAME)) {
                return AnalysisResult.internalError(BindingContext.EMPTY, e)
            } else {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                logger.error(sw.toString())
            }
        }
        return null
    }
}

/**
 * Used when an output potentially depends on new information.
 */
internal class AnyChanges(val baseDir: File) : KSFile {
    override val annotations: Sequence<KSAnnotation>
        get() = throw Exception("AnyChanges should not be used.")

    override val declarations: Sequence<KSDeclaration>
        get() = throw Exception("AnyChanges should not be used.")

    override val fileName: String
        get() = "<AnyChanges is a virtual file; DO NOT USE.>"

    override val filePath: String
        get() = File(baseDir, fileName).path

    override val packageName: KSName
        get() = throw Exception("AnyChanges should not be used.")

    override val origin: Origin
        get() = throw Exception("AnyChanges should not be used.")

    override val location: Location
        get() = throw Exception("AnyChanges should not be used.")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        throw Exception("AnyChanges should not be used.")
    }
}
