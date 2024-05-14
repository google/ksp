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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package com.google.devtools.ksp.impl

import com.google.devtools.ksp.common.AnyChanges
import com.google.devtools.ksp.common.KSObjectCacheManager
import com.google.devtools.ksp.common.impl.CodeGeneratorImpl
import com.google.devtools.ksp.common.impl.JsPlatformInfoImpl
import com.google.devtools.ksp.common.impl.JvmPlatformInfoImpl
import com.google.devtools.ksp.common.impl.NativePlatformInfoImpl
import com.google.devtools.ksp.common.impl.UnknownPlatformInfoImpl
import com.google.devtools.ksp.impl.symbol.kotlin.Deferrable
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.Restorable
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.standalone.IncrementalKotlinDeclarationProviderFactory
import com.google.devtools.ksp.standalone.IncrementalKotlinPackageProviderFactory
import com.google.devtools.ksp.standalone.KspStandaloneDirectInheritorsProvider
import com.google.devtools.ksp.standalone.buildKspLibraryModule
import com.google.devtools.ksp.standalone.buildKspSourceModule
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Origin
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CorePackageIndex
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.standalone.KotlinStaticPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.FirStandaloneServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.LLFirStandaloneLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLSealedInheritorsProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.impl.getSourceFilePaths
import org.jetbrains.kotlin.analysis.providers.*
import org.jetbrains.kotlin.analysis.providers.impl.*
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.computeDefaultRootModules
import org.jetbrains.kotlin.cli.jvm.compiler.createSourceFilesFromSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.getJavaModuleRoots
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.session.registerResolveComponents
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class KotlinSymbolProcessing(
    val kspConfig: KSPConfig,
    val symbolProcessorProviders: List<SymbolProcessorProvider>,
    val logger: KSPLogger
) {
    enum class ExitCode(code: Int) {
        OK(0),

        // Whenever there are some error messages.
        PROCESSING_ERROR(1),

        // Let exceptions pop through to the caller. Don't catch and convert them to, e.g., INTERNAL_ERROR.
    }

    init {
        // We depend on swing (indirectly through PSI or something), so we want to declare headless mode,
        // to avoid accidentally starting the UI thread. But, don't set it if it was set externally.
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true")
        }
        setupIdeaStandaloneExecution()
    }

    @OptIn(KtAnalysisApiInternals::class)
    private fun createAASession(
        compilerConfiguration: CompilerConfiguration,
        applicationDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.application"),
        projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project"),
    ): Triple<StandaloneAnalysisAPISession, KotlinCoreProjectEnvironment, List<KtModule>> {
        val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment =
            StandaloneProjectFactory.createProjectEnvironment(
                projectDisposable,
                KotlinCoreApplicationEnvironmentMode.Production
            )

        val application: Application = kotlinCoreProjectEnvironment.environment.application
        val project: MockProject = kotlinCoreProjectEnvironment.project
        val configLanguageVersionSettings = compilerConfiguration[CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS]

        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            KtResolveExtensionProvider.EP_NAME.name,
            KtResolveExtensionProvider::class.java
        )

        // replaces buildKtModuleProviderByCompilerConfiguration(compilerConfiguration)
        val projectStructureProvider = KtModuleProviderBuilder(kotlinCoreProjectEnvironment).apply {
            val compilerConfig = compilerConfiguration
            val platform = when (kspConfig) {
                is KSPJvmConfig -> {
                    val jvmTarget = JvmTarget.fromString(kspConfig.jvmTarget) ?: JvmTarget.DEFAULT
                    JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget)
                }
                is KSPJsConfig -> when (kspConfig.backend) {
                    "WASM" -> WasmPlatforms.wasmJs
                    "JS" -> JsPlatforms.defaultJsPlatform
                    else -> throw IllegalArgumentException("Unknown JS backend: ${kspConfig.backend}")
                }
                is KSPNativeConfig -> NativePlatforms.nativePlatformByTargetNames(listOf(kspConfig.targetName))
                is KSPCommonConfig -> CommonPlatforms.defaultCommonPlatform
                else -> throw IllegalArgumentException("Unknown platform for config: $kspConfig")
            }

            fun KtModuleBuilder.addModuleDependencies(moduleName: String) {
                val libraryRoots = compilerConfig.jvmModularRoots + compilerConfig.jvmClasspathRoots
                addRegularDependency(
                    buildKspLibraryModule {
                        this.platform = platform
                        addBinaryRoots(libraryRoots.map { it.toPath() })
                        libraryName = "Library for $moduleName"
                    }
                )
                compilerConfig.get(JVMConfigurationKeys.JDK_HOME)?.let { jdkHome ->
                    addRegularDependency(
                        buildKtSdkModule {
                            this.platform = platform
                            addBinaryRootsFromJdkHome(jdkHome.toPath(), isJre = false)
                            sdkName = "JDK for $moduleName"
                        }
                    )
                }
            }

            buildKspSourceModule {
                configLanguageVersionSettings?.let { this.languageVersionSettings = it }
                this.platform = platform
                this.moduleName = compilerConfig.get(CommonConfigurationKeys.MODULE_NAME) ?: "<no module name provided>"

                addModuleDependencies(moduleName)

                // Single file java source roots are added in reinitJavaFileManager() later.
                val roots = mutableListOf<File>()
                roots.addAll(kspConfig.sourceRoots)
                roots.addAll(kspConfig.commonSourceRoots)
                roots.add(kspConfig.kotlinOutputDir)
                if (kspConfig is KSPJvmConfig) {
                    roots.addAll(kspConfig.javaSourceRoots)
                    roots.add(kspConfig.javaOutputDir)
                }
                roots.forEach {
                    it.mkdirs()
                }
                addSourceRoots(roots.map { it.toPath() })
            }.apply(::addModule)

            this.platform = platform
        }.build()

        // register services and build session
        val ktModuleProviderImpl = projectStructureProvider as KtStaticProjectStructureProvider
        val modules = ktModuleProviderImpl.allKtModules
        val allSourceFiles = ktModuleProviderImpl.allSourceFiles
        StandaloneProjectFactory.registerServicesForProjectEnvironment(
            kotlinCoreProjectEnvironment,
            projectStructureProvider,
        )
        val ktFiles = allSourceFiles.filterIsInstance<KtFile>()
        val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(modules, kotlinCoreProjectEnvironment)
        val createPackagePartProvider =
            StandaloneProjectFactory.createPackagePartsProvider(
                libraryRoots,
            )
        registerProjectServices(
            kotlinCoreProjectEnvironment,
            ktFiles,
            createPackagePartProvider,
        )

        kotlinCoreProjectEnvironment.project.apply {
            registerService(
                KotlinPsiDeclarationProviderFactory::class.java,
                KotlinStaticPsiDeclarationProviderFactory(this)
            )
        }

        project.registerService(
            LLFirLibrarySymbolProviderFactory::class.java,
            LLFirStandaloneLibrarySymbolProviderFactory::class.java
        )
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea, PsiTreeChangeListener.EP.name, PsiTreeChangeAdapter::class.java
        )
        return Triple(
            StandaloneAnalysisAPISession(kotlinCoreProjectEnvironment, createPackagePartProvider) {
                // This is only used by kapt4, which should query a provider, instead of have it passed here IMHO.
                // kapt4's implementation is static, which may or may not work for us depending on future use cases.
                // Let's implement it later if necessary.
                TODO("Not implemented yet.")
            },
            kotlinCoreProjectEnvironment,
            modules
        )
    }

    // TODO: org.jetbrains.kotlin.analysis.providers.impl.KotlinStatic*
    @OptIn(KtAnalysisApiInternals::class)
    private fun registerProjectServices(
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
        ktFiles: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    ) {
        val project = kotlinCoreProjectEnvironment.project
        project.apply {
            registerService(
                KotlinMessageBusProvider::class.java,
                KotlinProjectMessageBusProvider::class.java
            )
            FirStandaloneServiceRegistrar.registerProjectServices(project)
            FirStandaloneServiceRegistrar.registerProjectExtensionPoints(project)
            FirStandaloneServiceRegistrar.registerProjectModelServices(
                project,
                kotlinCoreProjectEnvironment.parentDisposable
            )

            registerService(
                KotlinModificationTrackerFactory::class.java,
                KotlinStaticModificationTrackerFactory::class.java
            )
            registerService(
                KotlinGlobalModificationService::class.java,
                KotlinStaticGlobalModificationService::class.java
            )
            registerService(
                KtLifetimeTokenProvider::class.java,
                KtAlwaysAccessibleLifeTimeTokenProvider::class.java
            )

            // Despite being a static implementation, this is only used by IDE tests
            registerService(
                KotlinAnnotationsResolverFactory::class.java,
                KotlinStaticAnnotationsResolverFactory(project, ktFiles)
            )
            registerService(
                KotlinResolutionScopeProvider::class.java,
                KotlinByModulesResolutionScopeProvider::class.java
            )
            registerService(
                KotlinDeclarationProviderFactory::class.java,
                IncrementalKotlinDeclarationProviderFactory(this)
            )
            registerService(
                KotlinDirectInheritorsProvider::class.java,
                KspStandaloneDirectInheritorsProvider::class.java
            )
            registerService(
                KotlinDeclarationProviderMerger::class.java,
                KotlinStaticDeclarationProviderMerger(this)
            )
            registerService(KotlinPackageProviderFactory::class.java, IncrementalKotlinPackageProviderFactory(project))

            registerService(
                SealedClassInheritorsProvider::class.java,
                LLSealedInheritorsProvider::class.java,
            )

            registerService(
                PackagePartProviderFactory::class.java,
                KotlinStaticPackagePartProviderFactory(packagePartProvider)
            )
        }
    }

    private fun prepareAllKSFiles(
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
        modules: List<KtModule>,
        compilerConfiguration: CompilerConfiguration
    ): List<KSFile> {
        val project = kotlinCoreProjectEnvironment.project
        val psiManager = PsiManager.getInstance(project)
        val ktFiles = createSourceFilesFromSourceRoots(
            compilerConfiguration, project, compilerConfiguration.kotlinSourceRoots
        ).toSet().toList()
        val psiFiles = getPsiFilesFromPaths<PsiFileSystemItem>(
            project,
            getSourceFilePaths(compilerConfiguration, includeDirectoryRoot = true)
        )

        // Update Kotlin providers for newly generated source files.
        (
            project.getService(
                KotlinDeclarationProviderFactory::class.java
            ) as IncrementalKotlinDeclarationProviderFactory
            ).update(ktFiles)
        (
            project.getService(
                KotlinPackageProviderFactory::class.java
            ) as IncrementalKotlinPackageProviderFactory
            ).update(ktFiles)

        // Update Java providers for newly generated source files.
        reinitJavaFileManager(kotlinCoreProjectEnvironment, modules, psiFiles)

        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val javaFiles = if (kspConfig is KSPJvmConfig) {
            val javaRoots = kspConfig.javaSourceRoots + kspConfig.javaOutputDir
            // Get non-symbolic paths first
            javaRoots.sortedBy { Files.isSymbolicLink(it.toPath()) }
                .flatMap { root -> root.walk().filter { it.isFile && it.extension == "java" }.toList() }
                // This time is for .java files
                .sortedBy { Files.isSymbolicLink(it.toPath()) }
                .distinctBy { it.canonicalPath }
                .mapNotNull { localFileSystem.findFileByPath(it.path)?.let { psiManager.findFile(it) } as? PsiJavaFile }
        } else {
            emptyList()
        }

        return ktFiles.map { analyze { KSFileImpl.getCached(it.getFileSymbol()) } } +
            javaFiles.map { KSFileJavaImpl.getCached(it) }
    }

    // TODO: performance
    @OptIn(KtAnalysisApiInternals::class)
    fun execute(): ExitCode {
        // TODO: CompilerConfiguration is deprecated.
        val compilerConfiguration: CompilerConfiguration = CompilerConfiguration().apply {
            addKotlinSourceRoots(kspConfig.sourceRoots.map { it.path })
            addKotlinSourceRoot(kspConfig.kotlinOutputDir.path)
            if (kspConfig is KSPJvmConfig) {
                addJavaSourceRoots(kspConfig.javaSourceRoots)
                addJavaSourceRoot(kspConfig.javaOutputDir)
                kspConfig.jdkHome?.let {
                    put(JVMConfigurationKeys.JDK_HOME, it)
                }
            }
            val languageVersion = LanguageVersion.fromFullVersionString(kspConfig.languageVersion)!!
            val apiVersion = LanguageVersion.fromFullVersionString(kspConfig.apiVersion)!!
            languageVersionSettings = LanguageVersionSettingsImpl(
                languageVersion,
                ApiVersion.createByLanguageVersion(apiVersion)
            )
            addJvmClasspathRoots(kspConfig.libraries)
            put(CommonConfigurationKeys.MODULE_NAME, kspConfig.moduleName)
        }

        val (analysisAPISession, kotlinCoreProjectEnvironment, modules) = createAASession(compilerConfiguration)
        val project = analysisAPISession.project
        val kspCoreEnvironment = KSPCoreEnvironment(project as MockProject)

        val logger = object : KSPLogger by logger {
            var hasError: Boolean = false

            override fun error(message: String, symbol: KSNode?) {
                hasError = true
                logger.error(message, symbol)
            }

            override fun warn(message: String, symbol: KSNode?) {
                if (kspConfig.allWarningsAsErrors)
                    hasError = true
                logger.warn(message, symbol)
            }
        }

        val psiManager = PsiManager.getInstance(project)
        val providers: List<SymbolProcessorProvider> = symbolProcessorProviders
        // KspModuleBuilder ensures this is always a KtSourceModule
        ResolverAAImpl.ktModule = modules.single() as KtSourceModule

        // Initializing environments
        val allKSFiles = prepareAllKSFiles(kotlinCoreProjectEnvironment, modules, compilerConfiguration)
        val anyChangesWildcard = AnyChanges(kspConfig.projectBaseDir)
        val codeGenerator = CodeGeneratorImpl(
            kspConfig.classOutputDir,
            { if (kspConfig is KSPJvmConfig) kspConfig.javaOutputDir else kspConfig.kotlinOutputDir },
            kspConfig.kotlinOutputDir,
            kspConfig.resourceOutputDir,
            kspConfig.projectBaseDir,
            anyChangesWildcard,
            allKSFiles,
            kspConfig.incremental
        )

        val dualLookupTracker = DualLookupTracker()
        val incrementalContext = IncrementalContextAA(
            kspConfig.incremental,
            dualLookupTracker,
            File(anyChangesWildcard.filePath).relativeTo(kspConfig.projectBaseDir),
            kspConfig.incrementalLog,
            kspConfig.projectBaseDir,
            kspConfig.cachesDir,
            kspConfig.outputBaseDir,
            kspConfig.modifiedSources,
            kspConfig.removedSources,
            kspConfig.changedClasses,
        )
        var allDirtyKSFiles = incrementalContext.calcDirtyFiles(allKSFiles).toList()
        var newKSFiles = allDirtyKSFiles
        val initialDirtySet = allDirtyKSFiles.toSet()
        val allCleanFilePaths = allKSFiles.filterNot { it in initialDirtySet }.map { it.filePath }.toSet()

        val targetPlatform = ResolverAAImpl.ktModule.platform
        val symbolProcessorEnvironment = SymbolProcessorEnvironment(
            kspConfig.processorOptions,
            kspConfig.languageVersion.toKotlinVersion(),
            codeGenerator,
            logger,
            kspConfig.apiVersion.toKotlinVersion(),
            KotlinCompilerVersion.getVersion().toKotlinVersion(),
            targetPlatform.getPlatformInfo(kspConfig),
            KotlinVersion(2, 0)
        )

        // Load and instantiate processsors
        val deferredSymbols = mutableMapOf<SymbolProcessor, List<Restorable>>()
        val processors = providers.map { provider ->
            provider.create(symbolProcessorEnvironment).also { deferredSymbols[it] = mutableListOf() }
        }

        var rounds = 0
        // Run processors until either
        // 1) there is an error
        // 2) there is no more new files.
        while (!logger.hasError) {
            logger.logging("round ${++rounds} of processing")
            // FirSession in AA is created lazily. Getting it instantiates module providers, which requires source roots
            // to be resolved. Therefore, due to the implementation, it has to be registered repeatedly after the files
            // are created.
            val firSession = ResolverAAImpl.ktModule.getFirResolveSession(project)
            firSession.useSiteFirSession.registerResolveComponents(dualLookupTracker)

            val resolver = ResolverAAImpl(
                allDirtyKSFiles,
                newKSFiles,
                deferredSymbols,
                project,
                incrementalContext,
            )
            ResolverAAImpl.instance = resolver
            ResolverAAImpl.instance.functionAsMemberOfCache = mutableMapOf()
            ResolverAAImpl.instance.propertyAsMemberOfCache = mutableMapOf()

            processors.forEach {
                deferredSymbols[it] =
                    it.process(resolver).filter { it.origin == Origin.KOTLIN || it.origin == Origin.JAVA }
                        .filterIsInstance<Deferrable>().mapNotNull(Deferrable::defer)
                if (!deferredSymbols.containsKey(it) || deferredSymbols[it]!!.isEmpty()) {
                    deferredSymbols.remove(it)
                }
            }

            if (logger.hasError || codeGenerator.generatedFile.isEmpty()) {
                break
            }

            // Drop caches
            KotlinGlobalModificationService.getInstance(project).publishGlobalModuleStateModification()
            KtAnalysisSessionProvider.getInstance(project).clearCaches()
            psiManager.dropResolveCaches()
            psiManager.dropPsiCaches()

            KSObjectCacheManager.clear()

            val newFilePaths = codeGenerator.generatedFile.filter { it.extension == "kt" || it.extension == "java" }
                .map { it.canonicalPath }.toSet()
            allDirtyKSFiles = prepareAllKSFiles(kotlinCoreProjectEnvironment, modules, compilerConfiguration).filter {
                it.filePath !in allCleanFilePaths
            }
            newKSFiles = allDirtyKSFiles.filter { it.filePath in newFilePaths }
            incrementalContext.registerGeneratedFiles(newKSFiles)
            codeGenerator.closeFiles()
        }

        // Call onError() or finish()
        if (logger.hasError) {
            processors.forEach(SymbolProcessor::onError)
        } else {
            processors.forEach(SymbolProcessor::finish)
        }

        if (!logger.hasError) {
            incrementalContext.updateCachesAndOutputs(
                initialDirtySet,
                codeGenerator.outputs,
                codeGenerator.sourceToOutputs
            )
        }

        codeGenerator.closeFiles()

        return if (logger.hasError) ExitCode.PROCESSING_ERROR else ExitCode.OK
    }
}

private inline fun <reified T : PsiFileSystemItem> getPsiFilesFromPaths(
    project: Project,
    paths: Set<Path>,
): List<T> {
    val fs = StandardFileSystems.local()
    val psiManager = PsiManager.getInstance(project)
    return buildList {
        for (path in paths) {
            val vFile = fs.findFileByPath(path.toString()) ?: continue
            val psiFileSystemItem =
                if (vFile.isDirectory)
                    psiManager.findDirectory(vFile) as? T
                else
                    psiManager.findFile(vFile) as? T
            psiFileSystemItem?.let { add(it) }
        }
    }
}

class DirectoriesScope(
    project: Project,
    private val directories: Set<VirtualFile>
) : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
    private val fileSystems = directories.mapTo(hashSetOf(), VirtualFile::getFileSystem)

    override fun contains(file: VirtualFile): Boolean {
        if (file.fileSystem !in fileSystems) return false

        var parent: VirtualFile = file
        while (true) {
            if (parent in directories) return true
            parent = parent.parent ?: return false
        }
    }

    override fun toString() = "All files under: $directories"
}

private fun reinitJavaFileManager(
    environment: KotlinCoreProjectEnvironment,
    modules: List<KtModule>,
    sourceFiles: List<PsiFileSystemItem>,
) {
    val project = environment.project
    val javaFileManager = project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl
    val javaModuleFinder = CliJavaModuleFinder(null, null, javaFileManager, project, null)
    val javaModuleGraph = JavaModuleGraph(javaModuleFinder)
    val allSourceFileRoots = sourceFiles.map { JavaRoot(it.virtualFile, JavaRoot.RootType.SOURCE) }
    val jdkRoots = getDefaultJdkModuleRoots(javaModuleFinder, javaModuleGraph)
    val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(modules, environment)

    val rootsWithSingleJavaFileRoots = buildList {
        addAll(libraryRoots)
        addAll(allSourceFileRoots)
        addAll(jdkRoots)
    }

    val (roots, singleJavaFileRoots) = rootsWithSingleJavaFileRoots.partition { (file) ->
        file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION
    }

    val corePackageIndex = project.getService(PackageIndex::class.java) as CorePackageIndex
    val rootsIndex = JvmDependenciesDynamicCompoundIndex().apply {
        addIndex(JvmDependenciesIndexImpl(roots))
        indexedRoots.forEach { javaRoot ->
            if (javaRoot.file.isDirectory) {
                if (javaRoot.type == JavaRoot.RootType.SOURCE) {
                    // NB: [JavaCoreProjectEnvironment#addSourcesToClasspath] calls:
                    //   1) [CoreJavaFileManager#addToClasspath], which is used to look up Java roots;
                    //   2) [CorePackageIndex#addToClasspath], which populates [PackageIndex]; and
                    //   3) [FileIndexFacade#addLibraryRoot], which conflicts with this SOURCE root when generating a library scope.
                    // Thus, here we manually call first two, which are used to:
                    //   1) create [PsiPackage] as a package resolution result; and
                    //   2) find directories by package name.
                    // With both supports, annotations defined in package-info.java can be properly propagated.
                    javaFileManager.addToClasspath(javaRoot.file)
                    corePackageIndex.addToClasspath(javaRoot.file)
                } else {
                    environment.addSourcesToClasspath(javaRoot.file)
                }
            }
        }
    }

    javaFileManager.initialize(
        rootsIndex,
        listOf(
            StandaloneProjectFactory.createPackagePartsProvider(
                libraryRoots + jdkRoots,
                LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)
            ).invoke(ProjectScope.getLibrariesScope(project))
        ),
        SingleJavaFileRootsIndex(singleJavaFileRoots),
        true
    )
}

private fun getDefaultJdkModuleRoots(
    javaModuleFinder: CliJavaModuleFinder,
    javaModuleGraph: JavaModuleGraph
): List<JavaRoot> {
    // In contrast to `ClasspathRootsResolver.addModularRoots`, we do not need to handle automatic Java modules because JDK modules
    // aren't automatic.
    return javaModuleGraph.getAllDependencies(javaModuleFinder.computeDefaultRootModules()).flatMap { moduleName ->
        val module = javaModuleFinder.findModule(moduleName) ?: return@flatMap emptyList<JavaRoot>()
        val result = module.getJavaModuleRoots()
        result
    }
}

fun String?.toKotlinVersion(): KotlinVersion {
    if (this == null)
        return KotlinVersion.CURRENT

    return split('-').first().split('.').map { it.toInt() }.let {
        when (it.size) {
            1 -> KotlinVersion(it[0], 0, 0)
            2 -> KotlinVersion(it[0], it[1], 0)
            3 -> KotlinVersion(it[0], it[1], it[2])
            else -> KotlinVersion.CURRENT
        }
    }
}

// Workaround for ShadowJar's minimize, whose configuration isn't very flexible.
internal val DEAR_SHADOW_JAR_PLEASE_DO_NOT_REMOVE_THESE = listOf(
    org.jetbrains.kotlin.analysis.api.impl.base.java.source.JavaElementSourceWithSmartPointerFactory::class.java,
    org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService::class.java,
    org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSessionProvider::class.java,
    org.jetbrains.kotlin.analysis.api.fir.references.ReadWriteAccessCheckerFirImpl::class.java,
    org.jetbrains.kotlin.analysis.api.standalone.base.providers.KotlinStandaloneDirectInheritorsProvider::class.java,
    org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLRealFirElementByPsiElementChooser::class.java,
    org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationService::class.java,
    org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased
        .deserialization.LLStubBasedLibrarySymbolProviderFactory::class.java,
    org.jetbrains.kotlin.analysis.providers.impl.KotlinProjectMessageBusProvider::class.java,
    org.jetbrains.kotlin.idea.references.KotlinFirReferenceContributor::class.java,
    org.jetbrains.kotlin.light.classes.symbol.SymbolKotlinAsJavaSupport::class.java,
    org.jetbrains.kotlin.load.java.ErasedOverridabilityCondition::class.java,
    org.jetbrains.kotlin.load.java.FieldOverridabilityCondition::class.java,
    org.jetbrains.kotlin.plugin.references.SimpleNameReferenceExtension::class.java,
    org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsLoaderImpl::class.java,
    com.fasterxml.aalto.AaltoInputProperties::class.java,
    com.google.errorprone.annotations.CheckReturnValue::class.java,
    com.intellij.openapi.application.JetBrainsProtocolHandler::class.java,
    com.intellij.openapi.editor.impl.EditorDocumentPriorities::class.java,
    com.intellij.psi.tree.ChildRoleBase::class.java,
    com.intellij.util.xmlb.Constants::class.java,
    com.intellij.xml.CommonXmlStrings::class.java,
    org.codehaus.stax2.XMLInputFactory2::class.java,
    org.codehaus.stax2.XMLStreamProperties::class.java,
)

fun TargetPlatform.getPlatformInfo(kspConfig: KSPConfig): List<PlatformInfo> =
    componentPlatforms.map { platform ->
        when (platform) {
            is JdkPlatform -> JvmPlatformInfoImpl(
                platformName = platform.platformName,
                jvmTarget = platform.targetVersion.toString(),
                jvmDefaultMode = (kspConfig as? KSPJvmConfig)?.jvmDefaultMode ?: "disable"
            )
            is JsPlatform -> JsPlatformInfoImpl(
                platformName = platform.platformName
            )
            is NativePlatform -> NativePlatformInfoImpl(
                platformName = platform.platformName,
                targetName = platform.targetName
            )
            // Unknown platform; toString() may be more informative than platformName
            else -> UnknownPlatformInfoImpl(platform.toString())
        }
    }
