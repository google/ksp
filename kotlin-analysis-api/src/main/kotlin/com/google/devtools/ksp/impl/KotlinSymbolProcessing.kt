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
import com.google.devtools.ksp.standalone.IncrementalGlobalSearchScope
import com.google.devtools.ksp.standalone.IncrementalJavaFileManager
import com.google.devtools.ksp.standalone.IncrementalKotlinDeclarationProviderFactory
import com.google.devtools.ksp.standalone.IncrementalKotlinPackageProviderFactory
import com.google.devtools.ksp.standalone.KspStandaloneDirectInheritorsProvider
import com.google.devtools.ksp.standalone.buildKspLibraryModule
import com.google.devtools.ksp.standalone.buildKspSdkModule
import com.google.devtools.ksp.standalone.buildKspSourceModule
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Origin
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ui.EDT
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.KotlinMessageBusProvider
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.KotlinProjectMessageBusProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModificationService
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinByModulesResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import org.jetbrains.kotlin.analysis.api.standalone.KotlinStaticPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.base.KotlinStandalonePlatformSettings
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneGlobalModificationService
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.permissions.KotlinStandaloneAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.FirStandaloneServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLSealedInheritorsProvider
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
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
import java.nio.file.Path

@Suppress("UnstableApiUsage")
class KotlinSymbolProcessing(
    val kspConfig: KSPConfig,
    val symbolProcessorProviders: List<SymbolProcessorProvider>,
    val logger: KSPLogger
) {
    enum class ExitCode(
        val code: Int
    ) {
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

    @OptIn(KaExperimentalApi::class, KaImplementationDetail::class)
    private fun createAASession(
        compilerConfiguration: CompilerConfiguration,
        projectDisposable: Disposable,
    ): Triple<StandaloneAnalysisAPISession, KotlinCoreProjectEnvironment, List<KaModule>> {
        val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment =
            StandaloneProjectFactory.createProjectEnvironment(
                projectDisposable,
                KotlinCoreApplicationEnvironmentMode.Production
            )

        val project: MockProject = kotlinCoreProjectEnvironment.project
        val configLanguageVersionSettings = compilerConfiguration[CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS]

        @Suppress("UnstableApiUsage")
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            KaResolveExtensionProvider.EP_NAME.name,
            KaResolveExtensionProvider::class.java
        )

        // replaces buildKtModuleProviderByCompilerConfiguration(compilerConfiguration)
        val projectStructureProvider = KtModuleProviderBuilder(
            kotlinCoreProjectEnvironment.environment, project
        ).apply {
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
                        buildKspSdkModule {
                            this.platform = platform
                            addBinaryRootsFromJdkHome(jdkHome.toPath(), isJre = false)
                            libraryName = "JDK for $moduleName"
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
                if (kspConfig is KSPJvmConfig) {
                    roots.addAll(kspConfig.javaSourceRoots)
                }
                addSourceRoots(roots.map { it.toPath() })
            }.apply(::addModule)

            this.platform = platform
        }.build()

        // register services and build session
        val ktModuleProviderImpl = projectStructureProvider
        val modules = ktModuleProviderImpl.allModules
        val allSourceFiles = ktModuleProviderImpl.allSourceFiles
        StandaloneProjectFactory.registerServicesForProjectEnvironment(
            kotlinCoreProjectEnvironment,
            projectStructureProvider,
        )
        val ktFiles = allSourceFiles.filterIsInstance<KtFile>()
        val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(modules, kotlinCoreProjectEnvironment.environment)
        val createPackagePartProvider =
            StandaloneProjectFactory.createPackagePartsProvider(
                libraryRoots,
            )

        kotlinCoreProjectEnvironment.registerApplicationServices(
            org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry::class.java,
            org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionRegistry::class.java
        )
        kotlinCoreProjectEnvironment.registerApplicationServices(
            KotlinAnalysisPermissionOptions::class.java,
            KotlinStandaloneAnalysisPermissionOptions::class.java
        )

        registerProjectServices(
            kotlinCoreProjectEnvironment,
            ktFiles,
            createPackagePartProvider,
            projectDisposable
        )

        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea, PsiTreeChangeListener.EP.name, PsiTreeChangeAdapter::class.java
        )
        return Triple(
            StandaloneAnalysisAPISession(kotlinCoreProjectEnvironment) {
                // This is only used by kapt4, which should query a provider, instead of have it passed here IMHO.
                // kapt4's implementation is static, which may or may not work for us depending on future use cases.
                // Let's implement it later if necessary.
                TODO("Not implemented yet.")
            },
            kotlinCoreProjectEnvironment,
            modules
        )
    }

    private fun <T> KotlinCoreProjectEnvironment.registerApplicationServices(
        serviceInterface: Class<T>,
        serviceImplementation: Class<out T>
    ) {
        val application = environment.application
        if (application.getServiceIfCreated(serviceInterface) == null) {
            KotlinCoreEnvironment.underApplicationLock {
                if (application.getServiceIfCreated(serviceInterface) == null) {
                    application.registerService(serviceInterface, serviceImplementation)
                }
            }
        }
    }

    // TODO: org.jetbrains.kotlin.analysis.providers.impl.KotlinStatic*
    private fun registerProjectServices(
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
        ktFiles: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        projectDisposable: Disposable,
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
                KotlinStandaloneModificationTrackerFactory::class.java
            )
            registerService(
                KotlinGlobalModificationService::class.java,
                KotlinStandaloneGlobalModificationService::class.java
            )
            registerService(
                KotlinLifetimeTokenFactory::class.java,
                KotlinAlwaysAccessibleLifetimeTokenFactory::class.java
            )

            // Despite being a static implementation, this is only used by IDE tests
            registerService(
                KotlinAnnotationsResolverFactory::class.java,
                KotlinStandaloneAnnotationsResolverFactory(project, ktFiles)
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
                KotlinStandaloneDeclarationProviderMerger(this)
            )
            registerService(
                KotlinPackageProviderFactory::class.java,
                IncrementalKotlinPackageProviderFactory(project, projectDisposable)
            )

            registerService(
                SealedClassInheritorsProvider::class.java,
                LLSealedInheritorsProvider::class.java,
            )

            registerService(
                KotlinPackagePartProviderFactory::class.java,
                KotlinStaticPackagePartProviderFactory(packagePartProvider)
            )

            registerService(
                KotlinPlatformSettings::class.java,
                KotlinStandalonePlatformSettings()
            )
        }
    }

    @OptIn(KaExperimentalApi::class)
    private fun prepareAllKSFiles(
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
        modules: List<KaModule>,
        javaFileManager: IncrementalJavaFileManager?,
    ): List<KSFile> {
        val project = kotlinCoreProjectEnvironment.project
        val ktFiles = mutableSetOf<KtFile>()
        val javaFiles = mutableSetOf<PsiJavaFile>()
        modules.filterIsInstance<KaSourceModule>().forEach {
            it.psiRoots.forEach {
                when (it) {
                    is KtFile -> ktFiles.add(it)
                    is PsiJavaFile -> if (javaFileManager != null) javaFiles.add(it)
                }
            }
        }

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

        javaFileManager?.initialize(modules, javaFiles)

        return ktFiles.map { analyze { KSFileImpl.getCached(it.symbol) } } +
            javaFiles.map { KSFileJavaImpl.getCached(it) }
    }

    private fun prepareNewKSFiles(
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
        javaFileManager: IncrementalJavaFileManager?,
        newKotlinFiles: List<File>,
        newJavaFiles: List<File>,
    ): List<KSFile> {
        val project = kotlinCoreProjectEnvironment.project
        val ktFiles = getPsiFilesFromPaths<KtFile>(
            project,
            newKotlinFiles.map { it.toPath() }.toSet()
        ).toSet()
        val javaFiles = if (javaFileManager != null) {
            getPsiFilesFromPaths<PsiJavaFile>(
                project,
                newJavaFiles.map { it.toPath() }.toSet()
            ).toSet()
        } else emptySet()

        // Add new files to content scope.
        val contentScope = ResolverAAImpl.ktModule.contentScope as IncrementalGlobalSearchScope
        contentScope.addAll(ktFiles.map { it.virtualFile })
        contentScope.addAll(javaFiles.map { it.virtualFile })

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
        javaFileManager?.add(javaFiles)

        return ktFiles.map { analyze { KSFileImpl.getCached(it.symbol) } } +
            javaFiles.map { KSFileJavaImpl.getCached(it) }
    }

    // TODO: performance
    @OptIn(KaImplementationDetail::class)
    fun execute(): ExitCode {
        // TODO: CompilerConfiguration is deprecated.
        val compilerConfiguration: CompilerConfiguration = CompilerConfiguration().apply {
            addKotlinSourceRoots(kspConfig.sourceRoots.map { it.path })
            if (kspConfig is KSPJvmConfig) {
                addJavaSourceRoots(kspConfig.javaSourceRoots)
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

        val projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project")
        try {
            val (analysisAPISession, kotlinCoreProjectEnvironment, modules) =
                createAASession(compilerConfiguration, projectDisposable)
            val project = analysisAPISession.project
            // Initializes it
            KSPCoreEnvironment(project as MockProject)

            val psiManager = PsiManager.getInstance(project)
            val providers: List<SymbolProcessorProvider> = symbolProcessorProviders
            // KspModuleBuilder ensures this is always a KtSourceModule
            ResolverAAImpl.ktModule = modules.single() as KaSourceModule

            // Initializing environments
            val javaFileManager = if (kspConfig is KSPJvmConfig) {
                IncrementalJavaFileManager(kotlinCoreProjectEnvironment)
            } else null

            val allKSFiles =
                prepareAllKSFiles(kotlinCoreProjectEnvironment, modules, javaFileManager)
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

            val targetPlatform = ResolverAAImpl.ktModule.targetPlatform
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

            fun dropCaches() {
                maybeRunInWriteAction {
                    KotlinGlobalModificationService.getInstance(project).publishGlobalSourceModuleStateModification()
                    KaSessionProvider.getInstance(project).clearCaches()
                    psiManager.dropResolveCaches()
                    psiManager.dropPsiCaches()

                    KSObjectCacheManager.clear()
                }
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
                    incrementalContext.closeFilesOnException {
                        deferredSymbols[it] =
                            it.process(resolver).filter { it.origin == Origin.KOTLIN || it.origin == Origin.JAVA }
                                .filterIsInstance<Deferrable>().mapNotNull(Deferrable::defer)
                    }
                    if (!deferredSymbols.containsKey(it) || deferredSymbols[it]!!.isEmpty()) {
                        deferredSymbols.remove(it)
                    }
                }

                val allKSFilesPointers = allDirtyKSFiles.filterIsInstance<Deferrable>().map { it.defer() }

                if (logger.hasError || codeGenerator.generatedFile.isEmpty()) {
                    break
                }

                dropCaches()

                newKSFiles = prepareNewKSFiles(
                    kotlinCoreProjectEnvironment,
                    javaFileManager,
                    codeGenerator.generatedFile.filter { it.extension.lowercase() == "kt" },
                    codeGenerator.generatedFile.filter { it.extension.lowercase() == "java" },
                )
                // Now that caches are dropped, KtSymbols and KS* are invalid. They need to be restored from deferred.
                // Do not replace `!!` with `?.`. Implementations of KSFile in KSP2 must implement Deferrable and
                // return non-null.
                allDirtyKSFiles = allKSFilesPointers.map { it!!.restore() as KSFile } + newKSFiles
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
                    allDirtyKSFiles,
                    codeGenerator.outputs,
                    codeGenerator.sourceToOutputs
                )
            } else {
                incrementalContext.closeFiles()
            }

            dropCaches()
            codeGenerator.closeFiles()
        } finally {
            maybeRunInWriteAction {
                Disposer.dispose(projectDisposable)
            }
        }

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
/* ktlint-disable */
@Suppress("unused")
@OptIn(KaImplementationDetail::class)
internal val DEAR_SHADOW_JAR_PLEASE_DO_NOT_REMOVE_THESE = listOf(
    it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap::class.java,
    it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap::class.java,
    org.jetbrains.kotlin.analysis.api.impl.base.java.source.JavaElementSourceWithSmartPointerFactory::class.java,
    org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBaseModuleProvider::class.java,
    org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService::class.java,
    org.jetbrains.kotlin.analysis.api.fir.KaFirDefaultImportsProvider::class.java,
    org.jetbrains.kotlin.analysis.api.fir.KaFirSessionProvider::class.java,
    org.jetbrains.kotlin.analysis.api.fir.references.ReadWriteAccessCheckerFirImpl::class.java,
    org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneFirDirectInheritorsProvider::class.java,
    org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLRealFirElementByPsiElementChooser::class.java,
    org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationService::class.java,
    org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.factories.LLStubOriginLibrarySymbolProviderFactory::class.java,
    org.jetbrains.kotlin.analysis.api.impl.base.java.KaBaseJavaModuleResolver::class.java,
    org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionChecker::class.java,
    org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionRegistry::class.java,
    org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry::class.java,
    org.jetbrains.kotlin.analysis.api.platform.KotlinProjectMessageBusProvider::class.java,
    org.jetbrains.kotlin.analysis.api.platform.permissions.KaAnalysisPermissionChecker::class.java,
    org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinSimpleGlobalSearchScopeMerger::class.java,
    org.jetbrains.kotlin.analysis.api.fir.modification.KaFirSourceModificationService::class.java,
    org.jetbrains.kotlin.analysis.api.fir.references.KotlinFirReferenceContributor::class.java,
    org.jetbrains.kotlin.analysis.api.fir.statistics.KaFirStatisticsService::class.java,
    org.jetbrains.kotlin.light.classes.symbol.SymbolKotlinAsJavaSupport::class.java,
    org.jetbrains.kotlin.load.java.ErasedOverridabilityCondition::class.java,
    org.jetbrains.kotlin.load.java.FieldOverridabilityCondition::class.java,
    org.jetbrains.kotlin.plugin.references.SimpleNameReferenceExtension::class.java,
    org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsLoaderImpl::class.java,
    com.fasterxml.aalto.AaltoInputProperties::class.java,
    com.google.errorprone.annotations.CheckReturnValue::class.java,
    com.intellij.diagnostic.ActivityCategory::class.java,
    com.intellij.openapi.application.JetBrainsProtocolHandler::class.java,
    com.intellij.openapi.editor.impl.EditorDocumentPriorities::class.java,
    com.intellij.platform.diagnostic.telemetry.TelemetryManager::class.java,
    com.intellij.psi.impl.PsiSubstitutorImpl::class.java,
    com.intellij.psi.tree.ChildRoleBase::class.java,
    com.intellij.util.xmlb.Constants::class.java,
    com.intellij.xml.CommonXmlStrings::class.java,
    org.codehaus.stax2.XMLInputFactory2::class.java,
    org.codehaus.stax2.XMLStreamProperties::class.java,
)
/* ktlint-enable */

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

private fun <R> maybeRunInWriteAction(f: () -> R) {
    synchronized(EDT::class.java) {
        if (!EDT.isCurrentThreadEdt())
            EDT.updateEdt()
        if (ApplicationManager.getApplication() != null) {
            runWriteAction {
                f()
            }
        } else {
            f()
        }
    }
}
