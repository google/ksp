/*
 * Copyright 2022 Google LLC
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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.google.devtools.ksp.gradle.tasks.inherited

import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.KspGradleSubplugin
import com.google.devtools.ksp.gradle.KspGradleSubplugin.Companion.getKspCachesDir
import com.google.devtools.ksp.gradle.KspTask
import com.google.devtools.ksp.gradle.tasks.KspTaskCreator
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import org.gradle.util.GradleVersion
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.CompilerJsOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.CompilerMultiplatformCommonOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.copyFreeCompilerArgsToArgs
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.compilerArgumentsConfigurationFlags
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.CLASS_STRUCTURE_ARTIFACT_TYPE
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.ClasspathSnapshot
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptClasspathChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformAction
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformLegacyAction
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinNativeCompilationData
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup
import org.jetbrains.kotlin.gradle.tasks.configuration.AbstractKotlinCompileConfig
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Callable
import javax.inject.Inject
import kotlin.reflect.KProperty1
import org.jetbrains.kotlin.gradle.dsl.CompilerJsOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerMultiplatformCommonOptions

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
internal class Configurator : AbstractKotlinCompileConfig<AbstractKotlinCompile<*>> {
    constructor(compilation: KotlinCompilationData<*>, kotlinCompile: AbstractKotlinCompile<*>) : super(compilation) {
        configureTask { task ->
            if (task is KspTaskJvm) {
                // Assign ownModuleName different from kotlin compilation to
                // work around https://github.com/google/ksp/issues/647
                // This will not be necessary once https://youtrack.jetbrains.com/issue/KT-45777 lands
                task.ownModuleName.value(kotlinCompile.ownModuleName.map { "$it-ksp" })
            }
            if (task is KspTaskJS) {
                val libraryCacheService = project.rootProject.gradle.sharedServices.registerIfAbsent(
                    Kotlin2JsCompile.LibraryFilterCachingService::class.java.canonicalName +
                        "_${Kotlin2JsCompile.LibraryFilterCachingService::class.java.classLoader.hashCode()}",
                    Kotlin2JsCompile.LibraryFilterCachingService::class.java
                ) {}
                task.libraryCache.set(libraryCacheService).also { task.libraryCache.disallowChanges() }
                task.pluginClasspath.setFrom(objectFactory.fileCollection())
            }
        }
    }
}

object InheritedTasks : KspTaskCreator {
    override fun createKspTask(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        kotlinCompileTask: AbstractKotlinCompileTool<*>,
        kspExtension: KspExtension,
        kspConfigurations: List<Configuration>,
        kspTaskName: String,
        target: String,
        sourceSetName: String,
        classOutputDir: File,
        javaOutputDir: File,
        kotlinOutputDir: File,
        resourceOutputDir: File,
        kspOutputDir: File,
    ): TaskProvider<out Task> {

        val kspClasspathConfiguration = project.configurations.getByName(
            KspGradleSubplugin.KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME
        )

        fun configureAsKspTask(kspTask: KspTaskInherited, isIncremental: Boolean) {
            // depends on the processor; if the processor changes, it needs to be reprocessed.
            val processorClasspath = project.configurations.maybeCreate("${kspTaskName}ProcessorClasspath")
                .extendsFrom(*kspConfigurations.toTypedArray())
            kspTask.processorClasspath.from(processorClasspath)
            kspTask.dependsOn(processorClasspath.buildDependencies)

            kspTask.options.addAll(
                kspTask.project.provider {
                    KspGradleSubplugin.getSubpluginOptions(
                        project,
                        kspExtension,
                        processorClasspath,
                        sourceSetName,
                        target,
                        isIncremental,
                        kspExtension.allWarningsAsErrors
                    )
                }
            )
            kspTask.commandLineArgumentProviders.addAll(kspExtension.commandLineArgumentProviders)
            kspTask.destination = kspOutputDir
            kspTask.blockOtherCompilerPlugins = kspExtension.blockOtherCompilerPlugins
            kspTask.apOptions.value(kspExtension.arguments).disallowChanges()
            kspTask.kspCacheDir.fileValue(getKspCachesDir(project, sourceSetName, target)).disallowChanges()

            if (kspExtension.blockOtherCompilerPlugins) {
                kspTask.overridePluginClasspath.from(kspClasspathConfiguration)
            }
            kspTask.isKspIncremental = isIncremental
        }

        fun configureAsAbstractKotlinCompileTool(kspTask: AbstractKotlinCompileTool<*>) {
            kspTask.destinationDirectory.set(kspOutputDir)
            kspTask.outputs.dirs(
                kotlinOutputDir,
                javaOutputDir,
                classOutputDir,
                resourceOutputDir
            )

            if (kspExtension.allowSourcesFromOtherPlugins) {
                val deps = kotlinCompileTask.dependsOn.filterNot {
                    it.safeAs<TaskProvider<*>>()?.name == kspTaskName ||
                        it.safeAs<Task>()?.name == kspTaskName
                }
                kspTask.dependsOn(deps)
                kspTask.setSource(kotlinCompileTask.sources)
                if (kotlinCompileTask is KotlinCompile) {
                    kspTask.setSource(kotlinCompileTask.javaSources)
                }
            } else {
                kotlinCompilation.allKotlinSourceSets.forEach { sourceSet ->
                    kspTask.setSource(sourceSet.kotlin)
                }
                if (kotlinCompilation is KotlinCommonCompilation) {
                    kspTask.setSource(kotlinCompilation.defaultSourceSet.kotlin)
                }
            }

            // Don't support binary generation for non-JVM platforms yet.
            // FIXME: figure out how to add user generated libraries.
            if (kspTask is KspTaskJvm) {
                kotlinCompilation.output.classesDirs.from(classOutputDir)
            }
        }
        val kspTaskProvider = when (kotlinCompileTask) {
            is AbstractKotlinCompile<*> -> {
                val kspTaskClass = when (kotlinCompileTask) {
                    is KotlinCompile -> KspTaskJvm::class.java
                    is Kotlin2JsCompile -> KspTaskJS::class.java
                    is KotlinCompileCommon -> KspTaskMetadata::class.java
                    else -> throw IllegalArgumentException("Unknown Kotlin compilation task $kotlinCompileTask")
                }
                val isIncremental = project.findProperty("ksp.incremental")?.toString()?.toBoolean() ?: true
                project.tasks.register(kspTaskName, kspTaskClass) { kspTask ->
                    configureAsKspTask(kspTask, isIncremental)
                    configureAsAbstractKotlinCompileTool(kspTask)

                    kspTask.libraries.setFrom(kotlinCompileTask.project.files(Callable { kotlinCompileTask.libraries }))
                    kspTask.configureCompilation(
                        kotlinCompilation as KotlinCompilationData<*>,
                        kotlinCompileTask,
                    )
                }
            }
            is KotlinNativeCompile -> {
                val kspTaskClass = KspTaskNative::class.java
                project.tasks.register(kspTaskName, kspTaskClass, kotlinCompileTask.compilation).apply {
                    configure { kspTask ->
                        kspTask.onlyIf {
                            kotlinCompileTask.compilation.konanTarget.enabledOnCurrentHost
                        }
                        configureAsKspTask(kspTask, false)
                        configureAsAbstractKotlinCompileTool(kspTask)

                        // KotlinNativeCompile computes -Xplugin=... from compilerPluginClasspath.
                        kspTask.compilerPluginClasspath = kspClasspathConfiguration
                        kspTask.commonSources.from(kotlinCompileTask.commonSources)
                        kspTask.compilerPluginOptions.addPluginArgument(kotlinCompileTask.compilerPluginOptions)
                    }
                }
            }
            else -> throw IllegalArgumentException("Unknown Kotlin compilation task $kotlinCompileTask")
        }

        kotlinCompileTask.safeAs<AbstractKotlinCompile<*>>()?.let {
            Configurator(kotlinCompilation as KotlinCompilationData<*>, kotlinCompileTask as AbstractKotlinCompile<*>)
                .execute(kspTaskProvider as TaskProvider<AbstractKotlinCompile<*>>)
        }

        return kspTaskProvider
    }
}

private val artifactType = Attribute.of("artifactType", String::class.java)

interface KspTaskInherited : KspTask {
    fun configureCompilation(
        kotlinCompilation: KotlinCompilationData<*>,
        kotlinCompile: AbstractKotlinCompile<*>,
    )
}

@CacheableTask
abstract class KspTaskJvm @Inject constructor(
    compilerOptions: CompilerJvmOptions,
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory
) : KotlinCompile(compilerOptions, workerExecutor, objectFactory), KspTaskInherited {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    @get:InputFiles
    @get:Incremental
    abstract val classpathStructure: ConfigurableFileCollection

    @get:Input
    var isIntermoduleIncremental: Boolean = false

    override fun configureCompilation(
        kotlinCompilation: KotlinCompilationData<*>,
        kotlinCompile: AbstractKotlinCompile<*>,
    ) {
        kotlinCompile as KotlinCompile
        val providerFactory = kotlinCompile.project.providers
        compileKotlinArgumentsContributor.set(
            providerFactory.provider {
                kotlinCompile.compilerArgumentsContributor
            }
        )

        isIntermoduleIncremental =
            (project.findProperty("ksp.incremental.intermodule")?.toString()?.toBoolean() ?: true) &&
            isKspIncremental
        if (isIntermoduleIncremental) {
            val classStructureIfIncremental = project.configurations.detachedConfiguration(
                project.dependencies.create(project.files(project.provider { kotlinCompile.libraries }))
            )
            maybeRegisterTransform(project)

            classpathStructure.from(
                classStructureIfIncremental.incoming.artifactView { viewConfig ->
                    viewConfig.attributes.attribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
                }.files
            ).disallowChanges()
            classpathSnapshotProperties.useClasspathSnapshot.value(true).disallowChanges()
        } else {
            classpathSnapshotProperties.useClasspathSnapshot.value(false).disallowChanges()
        }

        // Used only in incremental compilation and is not applicable to KSP.
        useKotlinAbiSnapshot.value(false)
    }

    private fun maybeRegisterTransform(project: Project) {
        // Use the same flag with KAPT, so as to share the same transformation in case KAPT and KSP are both enabled.
        if (!project.extensions.extraProperties.has("KaptStructureTransformAdded")) {
            val transformActionClass =
                if (GradleVersion.current() >= GradleVersion.version("5.4"))
                    StructureTransformAction::class.java
                else

                    StructureTransformLegacyAction::class.java
            project.dependencies.registerTransform(transformActionClass) { transformSpec ->
                transformSpec.from.attribute(artifactType, "jar")
                transformSpec.to.attribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
            }

            project.dependencies.registerTransform(transformActionClass) { transformSpec ->
                transformSpec.from.attribute(artifactType, "directory")
                transformSpec.to.attribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
            }

            project.extensions.extraProperties["KaptStructureTransformAdded"] = true
        }
    }

    // Reuse Kapt's infrastructure to compute affected names in classpath.
    // This is adapted from KaptTask.findClasspathChanges.
    private fun findClasspathChanges(
        changes: ChangedFiles,
    ): KaptClasspathChanges {
        val cacheDir = kspCacheDir.asFile.get()
        cacheDir.mkdirs()

        val allDataFiles = classpathStructure.files
        val changedFiles = (changes as? ChangedFiles.Known)?.let { it.modified + it.removed }?.toSet() ?: allDataFiles

        val loadedPrevious = ClasspathSnapshot.ClasspathSnapshotFactory.loadFrom(cacheDir)
        val previousAndCurrentDataFiles = lazy { loadedPrevious.getAllDataFiles() + allDataFiles }
        val allChangesRecognized = changedFiles.all {
            val extension = it.extension
            if (extension.isEmpty() || extension == "kt" || extension == "java" || extension == "jar" ||
                extension == "class"
            ) {
                return@all true
            }
            // if not a directory, Java source file, jar, or class, it has to be a structure file, in order to understand changes
            it in previousAndCurrentDataFiles.value
        }
        val previousSnapshot = if (allChangesRecognized) {
            loadedPrevious
        } else {
            ClasspathSnapshot.ClasspathSnapshotFactory.getEmptySnapshot()
        }

        val currentSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(
                cacheDir,
                libraries.files.toList(),
                processorClasspath.files.toList(),
                allDataFiles
            )

        val classpathChanges = currentSnapshot.diff(previousSnapshot, changedFiles)
        if (classpathChanges is KaptClasspathChanges.Unknown || changes is ChangedFiles.Unknown) {
            clearIncCache()
            cacheDir.mkdirs()
        }
        currentSnapshot.writeToCache()

        return classpathChanges
    }

    @get:Internal
    internal abstract val compileKotlinArgumentsContributor:
        Property<CompilerArgumentsContributor<K2JVMCompilerArguments>>

    init {
        // kotlinc's incremental compilation isn't compatible with symbol processing in a few ways:
        // * It doesn't consider private / internal changes when computing dirty sets.
        // * It compiles iteratively; Sources can be compiled in different rounds.
        incremental = false

        // Mute a warning from ScriptingGradleSubplugin, which tries to get `sourceSetName` before this task is
        // configured.
        sourceSetName.set("main")
    }

    override fun setupCompilerArgs(
        args: K2JVMCompilerArguments,
        defaultsOnly: Boolean,
        ignoreClasspathResolutionErrors: Boolean,
    ) {
        // Start with / copy from kotlinCompile.
        compileKotlinArgumentsContributor.get().contributeArguments(
            args,
            compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
            )
        )
        if (blockOtherCompilerPlugins) {
            args.blockOtherPlugins(overridePluginClasspath)
        }
        args.addPluginOptions(options.get())
        args.destinationAsFile = destination
        args.allowNoSourceFiles = true
        args.useK2 = false
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `callCompilerAsync$kotlin_gradle_plugin_common`(
        args: K2JVMCompilerArguments,
        kotlinSources: Set<File>,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        val changedFiles = getChangedFiles(inputChanges, incrementalProps)
        if (isKspIncremental) {
            if (isIntermoduleIncremental) {
                // findClasspathChanges may clear caches, if there are
                // 1. unknown changes, or
                // 2. changes in annotation processors.
                val classpathChanges = findClasspathChanges(changedFiles)
                args.addChangedClasses(classpathChanges)
            } else {
                if (changedFiles.hasNonSourceChange()) {
                    clearIncCache()
                }
            }
        } else {
            clearIncCache()
        }
        args.addChangedFiles(changedFiles)
        super.callCompilerAsync(args, kotlinSources, inputChanges, taskOutputsBackup)
    }

    override fun skipCondition(): Boolean = sources.isEmpty && javaSources.isEmpty

    override val incrementalProps: List<FileCollection>
        get() = listOf(
            sources,
            javaSources,
            commonSourceSet,
            classpathSnapshotProperties.classpath,
            classpathSnapshotProperties.classpathSnapshot
        )

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override val sources: FileCollection = super.sources.filter {
        !destination.isParentOf(it)
    }

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override val javaSources: FileCollection = super.javaSources.filter {
        !destination.isParentOf(it)
    }
}

@CacheableTask
abstract class KspTaskJS @Inject constructor(
    compilerOptions: CompilerJsOptions,
    @get:Internal val objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : Kotlin2JsCompile(compilerOptions, objectFactory, workerExecutor), KspTaskInherited {
    private val backendSelectionArgs = listOf(
        "-Xir-only",
        "-Xir-produce-js",
        "-Xir-produce-klib-dir",
        "-Xir-produce-klib-file"
    )

    override fun configureCompilation(
        kotlinCompilation: KotlinCompilationData<*>,
        kotlinCompile: AbstractKotlinCompile<*>,
    ) {
        kotlinCompile as Kotlin2JsCompile
        kotlinOptions.freeCompilerArgs = kotlinCompile.kotlinOptions.freeCompilerArgs.filter {
            it in backendSelectionArgs
        }
        val providerFactory = kotlinCompile.project.providers
        compileKotlinArgumentsContributor.set(
            providerFactory.provider {
                kotlinCompile.abstractKotlinCompileArgumentsContributor
            }
        )
    }

    @get:Internal
    internal abstract val compileKotlinArgumentsContributor:
        Property<CompilerArgumentsContributor<K2JSCompilerArguments>>

    init {
        // kotlinc's incremental compilation isn't compatible with symbol processing in a few ways:
        // * It doesn't consider private / internal changes when computing dirty sets.
        // * It compiles iteratively; Sources can be compiled in different rounds.
        incremental = false
    }

    override fun setupCompilerArgs(
        args: K2JSCompilerArguments,
        defaultsOnly: Boolean,
        ignoreClasspathResolutionErrors: Boolean,
    ) {
        // Start with / copy from kotlinCompile.
        (compilerOptions as CompilerJsOptionsDefault).fillDefaultValues(args)
        compileKotlinArgumentsContributor.get().contributeArguments(
            args,
            compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
            )
        )
        if (blockOtherCompilerPlugins) {
            args.blockOtherPlugins(overridePluginClasspath)
        }
        args.addPluginOptions(options.get())
        args.outputFile = File(destination, "dummyOutput.js").canonicalPath
        kotlinOptions.copyFreeCompilerArgsToArgs(args)
        args.useK2 = false
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `callCompilerAsync$kotlin_gradle_plugin_common`(
        args: K2JSCompilerArguments,
        kotlinSources: Set<File>,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        val changedFiles = getChangedFiles(inputChanges, incrementalProps)
        if (!isKspIncremental || changedFiles.hasNonSourceChange()) {
            clearIncCache()
        } else {
            args.addChangedFiles(changedFiles)
        }
        super.callCompilerAsync(args, kotlinSources, inputChanges, taskOutputsBackup)
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `isIncrementalCompilationEnabled$kotlin_gradle_plugin_common`(): Boolean = false

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override val sources: FileCollection = super.sources.filter {
        !destination.isParentOf(it)
    }
}

@CacheableTask
abstract class KspTaskMetadata @Inject constructor(
    compilerOptions: CompilerMultiplatformCommonOptions,
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory
) : KotlinCompileCommon(compilerOptions, workerExecutor, objectFactory), KspTaskInherited {
    override fun configureCompilation(
        kotlinCompilation: KotlinCompilationData<*>,
        kotlinCompile: AbstractKotlinCompile<*>,
    ) {
        kotlinCompile as KotlinCompileCommon
        val providerFactory = kotlinCompile.project.providers
        compileKotlinArgumentsContributor.set(
            providerFactory.provider {
                kotlinCompile.abstractKotlinCompileArgumentsContributor
            }
        )
    }

    @get:Internal
    internal abstract val compileKotlinArgumentsContributor:
        Property<CompilerArgumentsContributor<K2MetadataCompilerArguments>>

    init {
        // kotlinc's incremental compilation isn't compatible with symbol processing in a few ways:
        // * It doesn't consider private / internal changes when computing dirty sets.
        // * It compiles iteratively; Sources can be compiled in different rounds.
        incremental = false
    }

    override fun setupCompilerArgs(
        args: K2MetadataCompilerArguments,
        defaultsOnly: Boolean,
        ignoreClasspathResolutionErrors: Boolean,
    ) {
        // Start with / copy from kotlinCompile.
        (compilerOptions as CompilerJsOptionsDefault).fillDefaultValues(args)
        compileKotlinArgumentsContributor.get().contributeArguments(
            args,
            compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
            )
        )
        if (blockOtherCompilerPlugins) {
            args.blockOtherPlugins(overridePluginClasspath)
        }
        args.addPluginOptions(options.get())
        args.destination = destination.canonicalPath
        val classpathList = libraries.files.filter { it.exists() }.toMutableList()
        args.classpath = classpathList.joinToString(File.pathSeparator)
        args.friendPaths = friendPaths.files.map { it.absolutePath }.toTypedArray()
        args.refinesPaths = refinesMetadataPaths.map { it.absolutePath }.toTypedArray()
        args.expectActualLinker = true
        args.useK2 = false
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `callCompilerAsync$kotlin_gradle_plugin_common`(
        args: K2MetadataCompilerArguments,
        kotlinSources: Set<File>,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        val changedFiles = getChangedFiles(inputChanges, incrementalProps)
        if (!isKspIncremental || changedFiles.hasNonSourceChange()) {
            clearIncCache()
        } else {
            args.addChangedFiles(changedFiles)
        }
        super.callCompilerAsync(args, kotlinSources, inputChanges, taskOutputsBackup)
    }

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override val sources: FileCollection = super.sources.filter {
        !destination.isParentOf(it)
    }
}

@CacheableTask
abstract class KspTaskNative @Inject constructor(
    compilation: KotlinNativeCompilationData<*>,
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory,
    execOperations: ExecOperations
) : KotlinNativeCompile(compilation, objectFactory, providerFactory, execOperations), KspTaskInherited {
    override val additionalCompilerOptions: Provider<Collection<String>>
        get() {
            return project.provider {
                val kspOptions = options.get().flatMap { listOf("-P", it.toArg()) }
                super.additionalCompilerOptions.get() + kspOptions
            }
        }

    override var compilerPluginClasspath: FileCollection? = null
        get() {
            if (blockOtherCompilerPlugins) {
                field = overridePluginClasspath
            }
            return field
        }

    override fun configureCompilation(
        kotlinCompilation: KotlinCompilationData<*>,
        kotlinCompile: AbstractKotlinCompile<*>,
    ) = Unit

    // KotlinNativeCompile doesn't support Gradle incremental compilation. Therefore, there is no information about
    // new / changed / removed files.
    // Long term solution: contribute to upstream to support incremental compilation.
    // Short term workaround: declare a @TaskAction function and call super.compile().
    // Use a name that gets sorted in the front just in case. `_$` is lower, but it might be too hacky.
    @TaskAction
    fun _0() {
        options.get().single { it.key == "kspOutputDir" }.value.let {
            File(it).deleteRecursively()
        }
        super.compile()
    }

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    override val sources: FileCollection = super.sources.filter {
        !destination.isParentOf(it)
    }
}

// This forces rebuild.
private fun KspTaskInherited.clearIncCache() {
    kspCacheDir.get().asFile.deleteRecursively()
}

private fun ChangedFiles.hasNonSourceChange(): Boolean {
    if (this !is ChangedFiles.Known)
        return true

    return !(this.modified + this.removed).all {
        it.isKotlinFile(listOf("kt")) || it.isJavaFile()
    }
}

fun CommonCompilerArguments.addChangedClasses(changed: KaptClasspathChanges) {
    if (changed is KaptClasspathChanges.Known) {
        changed.names.map { it.replace('/', '.').replace('$', '.') }.ifNotEmpty {
            addPluginOptions(listOf(SubpluginOption("changedClasses", joinToString(":"))))
        }
    }
}

fun SubpluginOption.toArg() = "plugin:${KspGradleSubplugin.KSP_PLUGIN_ID}:$key=$value"

fun CommonCompilerArguments.addPluginOptions(options: List<SubpluginOption>) {
    pluginOptions = (options.map { it.toArg() } + pluginOptions!!).toTypedArray()
}

fun CommonCompilerArguments.addChangedFiles(changedFiles: ChangedFiles) {
    if (changedFiles is ChangedFiles.Known) {
        val options = mutableListOf<SubpluginOption>()
        changedFiles.modified.filter { it.isKotlinFile(listOf("kt")) || it.isJavaFile() }.ifNotEmpty {
            options += SubpluginOption("knownModified", map { it.path }.joinToString(File.pathSeparator))
        }
        changedFiles.removed.filter { it.isKotlinFile(listOf("kt")) || it.isJavaFile() }.ifNotEmpty {
            options += SubpluginOption("knownRemoved", map { it.path }.joinToString(File.pathSeparator))
        }
        options.ifNotEmpty { addPluginOptions(this) }
    }
}

private fun CommonCompilerArguments.blockOtherPlugins(kspPluginClasspath: FileCollection) {
    pluginClasspaths = kspPluginClasspath.map { it.canonicalPath }.toTypedArray()
    pluginOptions = arrayOf()
}

// TODO: Move into dumpArgs after the compiler supports local function in inline functions.
private inline fun <reified T : CommonCompilerArguments> T.toPair(property: KProperty1<T, *>): Pair<String, String> {
    @Suppress("UNCHECKED_CAST")
    val value = (property as KProperty1<T, *>).get(this)
    return property.name to if (value is Array<*>)
        value.asList().toString()
    else
        value.toString()
}

@Suppress("unused")
internal inline fun <reified T : CommonCompilerArguments> dumpArgs(args: T): Map<String, String> {
    @Suppress("UNCHECKED_CAST")
    val argumentProperties =
        args::class.members.mapNotNull { member ->
            (member as? KProperty1<T, *>)?.takeIf { it.annotations.any { ann -> ann is Argument } }
        }

    return argumentProperties.associate(args::toPair).toSortedMap()
}

internal fun File.isParentOf(childCandidate: File): Boolean {
    val parentPath = Paths.get(this.absolutePath).normalize()
    val childCandidatePath = Paths.get(childCandidate.absolutePath).normalize()

    return childCandidatePath.startsWith(parentPath)
}
