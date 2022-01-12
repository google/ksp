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

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.PlatformInfo
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.processing.impl.CodeGeneratorImpl
import com.google.devtools.ksp.processing.impl.JsPlatformInfoImpl
import com.google.devtools.ksp.processing.impl.JvmPlatformInfoImpl
import com.google.devtools.ksp.processing.impl.KSPCompilationError
import com.google.devtools.ksp.processing.impl.MessageCollectorBasedKSPLogger
import com.google.devtools.ksp.processing.impl.NativePlatformInfoImpl
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.processing.impl.UnknownPlatformInfoImpl
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.java.KSFileJavaImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSFileImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLClassLoader
import java.nio.file.Files

class KotlinSymbolProcessingExtension(
    options: KspOptions,
    logger: KSPLogger,
    val testProcessor: SymbolProcessorProvider? = null,
) : AbstractKotlinSymbolProcessingExtension(options, logger, testProcessor != null) {
    override fun loadProviders(): List<SymbolProcessorProvider> {
        if (!initialized) {
            providers = if (testProcessor != null) {
                listOf(testProcessor)
            } else {
                val processingClasspath = options.processingClasspath
                val classLoader =
                    URLClassLoader(processingClasspath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)

                ServiceLoaderLite.loadImplementations(SymbolProcessorProvider::class.java, classLoader)
            }
        }
        return providers
    }
}

abstract class AbstractKotlinSymbolProcessingExtension(
    val options: KspOptions,
    val logger: KSPLogger,
    val testMode: Boolean,
) :
    AnalysisHandlerExtension {
    var initialized = false
    var finished = false
    val deferredSymbols = mutableMapOf<SymbolProcessor, List<KSAnnotated>>()
    lateinit var providers: List<SymbolProcessorProvider>
    lateinit var processors: List<SymbolProcessor>
    lateinit var incrementalContext: IncrementalContext
    lateinit var dirtyFiles: Set<KSFile>
    lateinit var cleanFilenames: Set<String>
    lateinit var codeGenerator: CodeGeneratorImpl
    var newFileNames: Collection<String> = emptySet()
    var rounds = 0

    companion object {
        private const val KSP_PACKAGE_NAME = "com.google.devtools.ksp"
        private const val KOTLIN_PACKAGE_NAME = "org.jetbrains.kotlin"
        private const val MULTI_ROUND_THRESHOLD = 100
    }

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider,
    ): AnalysisResult? {
        // with `withCompilation == true`:
        // * KSP returns AnalysisResult.RetryWithAdditionalRoots in last round of processing, to notify compiler the generated sources.
        // * This function will be called again, and returning null tells compiler to fall through with normal compilation.
        if (finished) {
            if (!options.withCompilation)
                throw IllegalStateException("KSP is re-entered unexpectedly.")
            return null
        }

        rounds++
        if (rounds > MULTI_ROUND_THRESHOLD) {
            logger.warn("Current processing rounds exceeds 100, check processors for potential infinite rounds")
        }
        val psiManager = PsiManager.getInstance(project)
        if (initialized) {
            psiManager.dropPsiCaches()
        }

        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val javaSourceRoots =
            if (initialized) options.javaSourceRoots + options.javaOutputDir else options.javaSourceRoots
        val javaFiles = javaSourceRoots
            .sortedBy { Files.isSymbolicLink(it.toPath()) } // Get non-symbolic paths first
            .flatMap { root -> root.walk().filter { it.isFile && it.extension == "java" }.toList() }
            .sortedBy { Files.isSymbolicLink(it.toPath()) } // This time is for .java files
            .distinctBy { it.canonicalPath }
            .mapNotNull { localFileSystem.findFileByPath(it.path)?.let { psiManager.findFile(it) } as? PsiJavaFile }

        val anyChangesWildcard = AnyChanges(options.projectBaseDir)
        val ksFiles = files.map { KSFileImpl.getCached(it) } + javaFiles.map { KSFileJavaImpl.getCached(it) }
        lateinit var newFiles: List<KSFile>

        handleException(module, project) {
            if (!initialized) {
                incrementalContext = IncrementalContext(
                    options, componentProvider,
                    File(anyChangesWildcard.filePath).relativeTo(options.projectBaseDir)
                )
                dirtyFiles = incrementalContext.calcDirtyFiles(ksFiles).toSet()
                cleanFilenames = ksFiles.filterNot { it in dirtyFiles }.map { it.filePath }.toSet()
                newFiles = dirtyFiles.toList()
            } else {
                newFiles = ksFiles.filter {
                    when (it) {
                        is KSFileImpl -> it.file
                        is KSFileJavaImpl -> it.psi
                        else -> null
                    }?.virtualFile?.let { virtualFile ->
                        virtualFile.canonicalPath ?: virtualFile.path
                    } in newFileNames
                }
                incrementalContext.registerGeneratedFiles(newFiles)
            }
        }?.let { return@doAnalysis it }

        // dirtyFiles cannot be reused because they are created in the old container.
        val resolver = ResolverImpl(
            module,
            ksFiles.filterNot {
                it.filePath in cleanFilenames
            },
            newFiles, deferredSymbols, bindingTrace, project, componentProvider, incrementalContext, options
        )

        val providers = loadProviders()
        if (!initialized) {
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
                handleException(module, project) {
                    processor = provider.create(
                        SymbolProcessorEnvironment(
                            options.processingOptions,
                            options.languageVersion,
                            codeGenerator,
                            logger,
                            options.apiVersion,
                            options.compilerVersion,
                            findTargetInfos(module)
                        )
                    )
                }?.let { analysisResult -> return@doAnalysis analysisResult }
                if (logger.hasError()) {
                    return@mapNotNull null
                }
                processor?.also { deferredSymbols[it] = mutableListOf() }
            }
            /* Kotlin compiler expects a source dir to exist, but processors might not generate Kotlin source.
               Create it during initialization just in case. */
            options.kotlinOutputDir.mkdirs()
            initialized = true
        }
        if (!logger.hasError()) {
            processors.forEach processing@{ processor ->
                handleException(module, project) {
                    deferredSymbols[processor] =
                        processor.process(resolver).filter { it.origin == Origin.KOTLIN || it.origin == Origin.JAVA }
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
        newFileNames = codeGenerator.generatedFile.filter { it.extension == "kt" || it.extension == "java" }
            .map { it.canonicalPath }.toSet()
        if (codeGenerator.generatedFile.isEmpty()) {
            finished = true
        }
        KSObjectCacheManager.clear()
        codeGenerator.closeFiles()
        if (logger.hasError()) {
            finished = true
            processors.forEach { processor ->
                handleException(module, project) {
                    processor.onError()
                }?.let { return it }
            }
        } else {
            if (finished) {
                processors.forEach { processor ->
                    handleException(module, project) {
                        processor.finish()
                    }?.let { return it }
                }
                if (deferredSymbols.isNotEmpty()) {
                    deferredSymbols.map { entry ->
                        logger.warn(
                            "Unable to process:${entry.key::class.qualifiedName}:   ${
                            entry.value.map { it.toString() }.joinToString(";")
                            }"
                        )
                    }
                }
                if (!logger.hasError()) {
                    incrementalContext.updateCachesAndOutputs(
                        dirtyFiles,
                        codeGenerator.outputs,
                        codeGenerator.sourceToOutputs
                    )
                }
            }
        }
        if (finished) {
            logger.reportAll()
        }
        return if (finished && !options.withCompilation) {
            if (!options.returnOkOnError && logger.hasError()) {
                AnalysisResult.compilationError(BindingContext.EMPTY)
            } else {
                AnalysisResult.success(BindingContext.EMPTY, module, shouldGenerateCode = false)
            }
        } else {
            // Temporary workaround for metadata task class path issue.
            if (module.platform.isCommon()) {
                module.allDependencyModules.filter { it != module.builtIns.builtInsModule }.forEach {
                    it.safeAs<ModuleDescriptorImpl>()?.let {
                        val field = it.javaClass.getDeclaredField("packageFragmentProviderForModuleContent")
                        field.isAccessible = true
                        field.set(it, null)
                    }
                }
            }
            AnalysisResult.RetryWithAdditionalRoots(
                BindingContext.EMPTY,
                module,
                listOf(options.javaOutputDir),
                listOf(options.kotlinOutputDir),
                listOf(options.classOutputDir)
            )
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
        return (this as MessageCollectorBasedKSPLogger).recordedEvents.any {
            it.severity == CompilerMessageSeverity.ERROR || it.severity == CompilerMessageSeverity.EXCEPTION
        }
    }

    private fun handleException(module: ModuleDescriptor, project: Project, call: () -> Unit): AnalysisResult? {
        try {
            call()
        } catch (e: Exception) {
            fun Exception.logToError() {
                val sw = StringWriter()
                printStackTrace(PrintWriter(sw))
                logger.error(sw.toString())
            }

            fun Exception.isNotRecoverable(): Boolean =
                stackTrace.first().className.let {
                    // TODO: convert non-critical exceptions thrown by KSP to recoverable errors.
                    it.startsWith(KSP_PACKAGE_NAME) || it.startsWith(KOTLIN_PACKAGE_NAME)
                }

            // Returning non-null here allows
            // 1. subsequent processing of other processors in current round.
            // 2. processor.onError() be called.
            //
            // In other words, returning non-null let current round finish.
            when {
                e is KSPCompilationError -> {
                    logger.error("${project.findLocationString(e.file, e.offset)}: ${e.message}")
                    logger.reportAll()
                    return if (options.returnOkOnError) {
                        AnalysisResult.success(BindingContext.EMPTY, module, shouldGenerateCode = false)
                    } else {
                        AnalysisResult.compilationError(BindingContext.EMPTY)
                    }
                }

                e.isNotRecoverable() -> {
                    e.logToError()
                    logger.reportAll()
                    return if (options.returnOkOnError) {
                        AnalysisResult.success(BindingContext.EMPTY, module, shouldGenerateCode = false)
                    } else {
                        AnalysisResult.internalError(BindingContext.EMPTY, e)
                    }
                }

                // Let this round finish.
                else -> {
                    e.logToError()
                }
            }
        }
        return null
    }
}

fun findTargetInfos(module: ModuleDescriptor): List<PlatformInfo> =
    module.platform?.componentPlatforms?.map { platform ->
        when (platform) {
            is JdkPlatform -> JvmPlatformInfoImpl(platform.platformName, platform.targetVersion.toString())
            is JsPlatform -> JsPlatformInfoImpl(platform.platformName)
            is NativePlatform -> NativePlatformInfoImpl(platform.platformName, platform.targetName)
            // Unknown platform; toString() may be more informative than platformName
            else -> UnknownPlatformInfoImpl(platform.toString())
        }
    } ?: emptyList()
