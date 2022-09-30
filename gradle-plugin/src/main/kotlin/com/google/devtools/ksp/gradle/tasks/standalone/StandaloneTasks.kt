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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.google.devtools.ksp.gradle.tasks.standalone

import com.google.devtools.ksp.gradle.KotlinCompilerArguments
import com.google.devtools.ksp.gradle.KotlinJsCompilerArguments
import com.google.devtools.ksp.gradle.KotlinJvmCompilerArguments
import com.google.devtools.ksp.gradle.KotlinMetadataCompilerArguments
import com.google.devtools.ksp.gradle.KotlinNativeCompilerArguments
import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.KspGradleSubplugin
import com.google.devtools.ksp.gradle.KspGradleSubplugin.Companion.getKspCachesDir
import com.google.devtools.ksp.gradle.KspGradleSubplugin.Companion.getSubpluginOptions
import com.google.devtools.ksp.gradle.KspTask
import com.google.devtools.ksp.gradle.createKotlinJsCompilerRunner
import com.google.devtools.ksp.gradle.createKotlinJvmCompilerRunner
import com.google.devtools.ksp.gradle.createKotlinMetadataCompilerRunner
import com.google.devtools.ksp.gradle.createKotlinNativeCompilerRunner
import com.google.devtools.ksp.gradle.tasks.KspTaskCreator
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.CLASS_STRUCTURE_ARTIFACT_TYPE
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.ClasspathSnapshot
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptClasspathChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformAction
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformLegacyAction
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject

object StandaloneTasks : KspTaskCreator {
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

        fun configureKspTask(kspTask: KspTaskStandalone, kotlinCompile: KotlinCompileTool, isIncremental: Boolean) {
            // depends on the processor; if the processor changes, it needs to be reprocessed.
            val processorClasspath = project.configurations.maybeCreate("${kspTaskName}ProcessorClasspath")
                .extendsFrom(*kspConfigurations.toTypedArray())
            kspTask.processorClasspath.from(processorClasspath)
            kspTask.dependsOn(processorClasspath.buildDependencies)

            kspTask.options.addAll(
                kspTask.project.provider {
                    getSubpluginOptions(
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
            kspTask.apOptions.value(kspExtension.arguments).disallowChanges()
            kspTask.kspCacheDir.fileValue(getKspCachesDir(project, sourceSetName, target)).disallowChanges()
            kspTask.allWarningAsErrors.value(kspExtension.allWarningsAsErrors)

            kspTask.isKspIncremental = isIncremental

            val providers = project.providers
            kspTask.outputs.dirs(
                kotlinOutputDir,
                javaOutputDir,
                classOutputDir,
                resourceOutputDir
            )

            fun FileCollection.excludeKSP(): FileCollection = filter {
                !kspOutputDir.isParentOf(it)
            }

            if (kspExtension.allowSourcesFromOtherPlugins) {
                val deps = kotlinCompile.dependsOn.filterNot {
                    it.safeAs<TaskProvider<*>>()?.name == kspTaskName ||
                        it.safeAs<Task>()?.name == kspTaskName
                }
                kspTask.dependsOn(deps)
                kspTask.allSources.from(kotlinCompile.sources.excludeKSP())
                if (kotlinCompile is KotlinCompile) {
                    kspTask.allSources.from(kotlinCompile.javaSources.excludeKSP())
                }
            } else {
                kotlinCompilation.allKotlinSourceSets.forEach { sourceSet ->
                    kspTask.allSources.from(sourceSet.kotlin.excludeKSP())
                }
                if (kotlinCompilation is KotlinCommonCompilation) {
                    kspTask.allSources.from(kotlinCompilation.defaultSourceSet.kotlin.excludeKSP())
                }
            }

            if (kotlinCompile is KotlinNativeCompile) {
                kspTask.commonSources.from(providers.provider { kotlinCompile.commonSources })
                kspTask.friendPaths.from(kotlinCompile.compilation.friendPaths)
                kspTask.multiplatform.value(true)
            } else if (kotlinCompile is AbstractKotlinCompile<*>) {
                kspTask.commonSources.from(providers.provider { kotlinCompile.commonSourceSet })
                kspTask.friendPaths.from(kotlinCompile.friendPaths)
                kspTask.multiplatform.set(kotlinCompile.multiPlatformEnabled)
            } else {
                throw IllegalArgumentException("Unknown compilation task type")
            }
            kspTask.pluginClasspath.from(kspClasspathConfiguration)
            kspTask.libraries.from(providers.provider { kotlinCompile.libraries.excludeKSP() })
            kspTask.kotlinApiVersion.value(kotlinCompilation.kotlinOptions.apiVersion)
            kspTask.kotlinLanguageVersion.value(kotlinCompilation.kotlinOptions.languageVersion)
            kspTask.verbose.value(kotlinCompilation.kotlinOptions.verbose || project.logger.isDebugEnabled)

            kspTask.configureCompilation(kotlinCompilation as KotlinCompilation<KotlinCommonOptions>, kotlinCompile)

            // Don't support binary generation for non-JVM platforms yet.
            // FIXME: figure out how to add user generated libraries.
            if (kspTask is KspTaskJvm) {
                kotlinCompilation.output.classesDirs.from(classOutputDir)
            }
        }

        val kspTaskProvider = when (kotlinCompileTask) {
            is KotlinCompileTool -> {
                val kspTaskClass = when (kotlinCompileTask) {
                    is KotlinCompile -> KspTaskJvm::class.java
                    is Kotlin2JsCompile -> KspTaskJs::class.java
                    is KotlinCompileCommon -> KspTaskMetadata::class.java
                    is KotlinNativeCompile -> KspTaskNative::class.java
                    else -> throw IllegalArgumentException("Unknown Kotlin compilation task $kotlinCompileTask")
                }
                val isNative = kspTaskClass == KspTaskNative::class.java
                val isIncremental = project.findProperty("ksp.incremental")?.toString()?.toBoolean() ?: !isNative
                project.tasks.register(kspTaskName, kspTaskClass) { kspTask ->
                    if (isNative) {
                        // KotlinNativeCompile computes -Xplugin=... from compilerPluginClasspath.
                        kspTask.pluginClasspath.from(kspClasspathConfiguration)
                        // The lambda to Task.onlyIf is evaluated at evaluation time.
                        // It will try to serialize the entire closure and break configuration cache.
                        val isEnabled =
                            (kotlinCompilation as AbstractKotlinNativeCompilation).konanTarget.enabledOnCurrentHost
                        kspTask.onlyIf { isEnabled }
                    }
                    configureKspTask(kspTask, kotlinCompileTask, isIncremental)
                }
            }
            else -> throw IllegalArgumentException("Unknown Kotlin compilation task $kotlinCompileTask")
        }

        return kspTaskProvider
    }
}

private val artifactType = Attribute.of("artifactType", String::class.java)

interface KspTaskStandalone : KspTask {
    @get:Classpath
    @get:Incremental
    val libraries: ConfigurableFileCollection

    @get:Classpath
    val pluginClasspath: ConfigurableFileCollection

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val commonSources: ConfigurableFileCollection

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    val allSources: ConfigurableFileCollection

    @get:Input
    @get:Optional
    val kotlinApiVersion: Property<String>

    @get:Input
    @get:Optional
    val kotlinLanguageVersion: Property<String>

    @get:Input
    val verbose: Property<Boolean>

    @get:Internal
    val friendPaths: ConfigurableFileCollection

    @get:Input
    val multiplatform: Property<Boolean>

    @get:Input
    val allWarningAsErrors: Property<Boolean>

    fun configureCompilation(
        kotlinCompilation: KotlinCompilation<*>,
        kotlinCompile: KotlinCompileTool,
    )
}

// This forces rebuild.
private fun KspTaskStandalone.clearIncCache() {
    kspCacheDir.get().asFile.deleteRecursively()
}

private fun ChangedFiles.hasNonSourceChange(): Boolean {
    if (this !is ChangedFiles.Known)
        return true

    return !(this.modified + this.removed).all {
        it.isKotlinFile(listOf("kt")) || it.isJavaFile()
    }
}

fun KotlinCompilerArguments.addChangedClasses(changed: KaptClasspathChanges) {
    if (changed is KaptClasspathChanges.Known) {
        changed.names.map { it.replace('/', '.').replace('$', '.') }.ifNotEmpty {
            addPluginOptions(listOf(SubpluginOption("changedClasses", joinToString(":"))))
        }
    }
}

fun SubpluginOption.toArg() = "plugin:${KspGradleSubplugin.KSP_PLUGIN_ID}:$key=$value"

fun KotlinCompilerArguments.addPluginOptions(options: List<SubpluginOption>) {
    pluginOptions += options.map { it.toArg() }
}

fun KotlinCompilerArguments.addChangedFiles(changedFiles: ChangedFiles) {
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

internal fun File.isParentOf(childCandidate: File): Boolean {
    val parentPath = Paths.get(this.absolutePath).normalize()
    val childCandidatePath = Paths.get(childCandidate.absolutePath).normalize()

    return childCandidatePath.startsWith(parentPath)
}

private fun KotlinCompilerArguments.fromTask(task: KspTaskStandalone) {
    useK2 = false
    incrementalCompilation = false
    pluginClasspaths = task.pluginClasspath.files.toList()
    apiVersion = task.kotlinApiVersion.orNull
    languageVersion = task.kotlinLanguageVersion.orNull
    verbose = task.verbose.get()
    multiPlatform = task.multiplatform.get()
    allWarningsAsErrors = task.allWarningAsErrors.get()

    addPluginOptions(task.options.get())
}

@CacheableTask
abstract class KspTaskJvm @Inject constructor(
    objectFactory: ObjectFactory,
) : KspTaskStandalone, CompileUsingKotlinDaemon, DefaultTask() {
    @get:Nested
    val compilerRunner = createKotlinJvmCompilerRunner(this, objectFactory)

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    var isIntermoduleIncremental: Boolean = false

    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    @get:InputFiles
    @get:Incremental
    abstract val classpathStructure: ConfigurableFileCollection

    @get:Nested
    abstract val classpathSnapshotProperties: ClasspathSnapshotProperties

    @get:Internal
    val incrementalProps: List<FileCollection>
        get() = listOf(
            allSources,
            commonSources,
            classpathSnapshotProperties.classpath,
            classpathSnapshotProperties.classpathSnapshot
        )

    @get:Internal
    abstract val noJdk: Property<Boolean>

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val args = KotlinJvmCompilerArguments().apply {
            // Common
            fromTask(this@KspTaskJvm)

            // JVM
            noJdk = this@KspTaskJvm.noJdk.get()
            allowNoSourceFiles = true
            destination = this@KspTaskJvm.destination
            noReflect = true
            noStdlib = true
            libraries = this@KspTaskJvm.libraries.files.toList()
            friendPaths = this@KspTaskJvm.friendPaths.files.toList()
            moduleName = this@KspTaskJvm.moduleName.get()
        }

        // Clean outputs. Backups will be copied back if incremental.
        // TODO: leave outputs untouched and only restore outputs if build fails.
        outputs.files.forEach {
            it.deleteRecursively()
        }
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

        compilerRunner.runJvmCompilerAsync(
            args,
            allSources.files.filter { !destination.isParentOf(it) },
            commonSources.files.filter { !destination.isParentOf(it) },
            outputs.files.toList()
        )
    }

    override fun configureCompilation(
        kotlinCompilation: KotlinCompilation<*>,
        kotlinCompile: KotlinCompileTool,
    ) {
        val providers = project.providers

        kotlinCompile as AbstractKotlinCompile<*>
        compilerRunner.compilerClasspath.from(providers.provider { kotlinCompile.defaultCompilerClasspath })
        compilerRunner.compilerExecutionStrategy.set(kotlinCompile.compilerExecutionStrategy)
        compilerRunner.useDaemonFallbackStrategy.set(kotlinCompile.useDaemonFallbackStrategy)
        compilerRunner.kotlinDaemonJvmArguments.set(kotlinCompile.kotlinDaemonJvmArguments)

        // Android compilation doesn't include JDK libraries.
        // Copying the settings from KotlinCompilation is complicated and implementation dependent. So let's check
        // for Android explicitly.
        noJdk.value(kotlinCompilation is KotlinJvmAndroidCompilation)

        moduleName.value(kotlinCompilation.moduleName)

        isIntermoduleIncremental =
            (project.findProperty("ksp.incremental.intermodule")?.toString()?.toBoolean() ?: true) && isKspIncremental

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
}

@CacheableTask
abstract class KspTaskJs @Inject constructor(
    objectFactory: ObjectFactory,
) : KspTaskStandalone, CompileUsingKotlinDaemon, DefaultTask() {
    @get:Nested
    val compilerRunner = createKotlinJsCompilerRunner(this, objectFactory)

    @get:Internal
    abstract val freeArgs: ListProperty<String>

    @get:Internal
    val incrementalProps: List<FileCollection>
        get() = listOf(
            allSources,
            commonSources,
        )

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val args = KotlinJsCompilerArguments().apply {
            // Common
            fromTask(this@KspTaskJs)

            // JS
            destination = this@KspTaskJs.destination
            noStdlib = true
            libraries = this@KspTaskJs.libraries.files.toList()
            friendPaths = this@KspTaskJs.friendPaths.files.toList()
            destination = File(destination, "dummyOutput.js")

            this@KspTaskJs.freeArgs.get().forEach {
                when (it) {
                    "-Xir-only" -> irOnly = true
                    "-Xir-produce-js" -> irProduceJs = true
                    "-Xir-produce-klib-file" -> irProduceKlibFile = true
                    "-Xir-produce-klib-dir" -> irProduceKlibDir = true
                    "-Xir-build-cache" -> irBuildCache = true
                    "-Xwasm" -> wasm = true
                }
            }
        }

        // Clean outputs. Backups will be copied back if incremental.
        // TODO: leave outputs untouched and only restore outputs if build fails.
        outputs.files.forEach {
            it.deleteRecursively()
        }
        val changedFiles = getChangedFiles(inputChanges, incrementalProps)
        if (isKspIncremental) {
            if (changedFiles.hasNonSourceChange()) {
                clearIncCache()
            }
        } else {
            clearIncCache()
        }
        args.addChangedFiles(changedFiles)

        compilerRunner.runJsCompilerAsync(
            args,
            allSources.files.toList(),
            commonSources.files.toList(),
            outputs.files.toList()
        )
    }

    override fun configureCompilation(
        kotlinCompilation: KotlinCompilation<*>,
        kotlinCompile: KotlinCompileTool,
    ) {
        val providers = project.providers

        kotlinCompile as AbstractKotlinCompile<*>
        compilerRunner.compilerClasspath.from(providers.provider { kotlinCompile.defaultCompilerClasspath })
        compilerRunner.compilerExecutionStrategy.set(kotlinCompile.compilerExecutionStrategy)
        compilerRunner.useDaemonFallbackStrategy.set(kotlinCompile.useDaemonFallbackStrategy)
        compilerRunner.kotlinDaemonJvmArguments.set(kotlinCompile.kotlinDaemonJvmArguments)

        freeArgs.value(kotlinCompilation.kotlinOptions.freeCompilerArgs)
    }
}

@CacheableTask
abstract class KspTaskMetadata @Inject constructor(
    objectFactory: ObjectFactory,
) : KspTaskStandalone, CompileUsingKotlinDaemon, DefaultTask() {
    @get:Nested
    val compilerRunner = createKotlinMetadataCompilerRunner(this, objectFactory)

    @get:Internal
    val incrementalProps: List<FileCollection>
        get() = listOf(
            allSources,
            commonSources,
        )

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val args = KotlinMetadataCompilerArguments().apply {
            // Common
            fromTask(this@KspTaskMetadata)

            // Metadata
            destination = this@KspTaskMetadata.destination
            libraries = this@KspTaskMetadata.libraries.files.toList()
            friendPaths = this@KspTaskMetadata.friendPaths.files.toList()
            expectActualLinker = true
        }

        // Clean outputs. Backups will be copied back if incremental.
        // TODO: leave outputs untouched and only restore outputs if build fails.
        outputs.files.forEach {
            it.deleteRecursively()
        }
        val changedFiles = getChangedFiles(inputChanges, incrementalProps)
        if (isKspIncremental) {
            if (changedFiles.hasNonSourceChange()) {
                clearIncCache()
            }
        } else {
            clearIncCache()
        }
        args.addChangedFiles(changedFiles)

        compilerRunner.runMetadataCompilerAsync(
            args,
            allSources.files.toList(),
            commonSources.files.toList(),
            outputs.files.toList()
        )
    }

    override fun configureCompilation(
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>,
        kotlinCompile: KotlinCompileTool,
    ) {
        val providers = project.providers

        kotlinCompile as AbstractKotlinCompile<*>
        compilerRunner.compilerClasspath.from(providers.provider { kotlinCompile.defaultCompilerClasspath })
        compilerRunner.compilerExecutionStrategy.set(kotlinCompile.compilerExecutionStrategy)
        compilerRunner.useDaemonFallbackStrategy.set(kotlinCompile.useDaemonFallbackStrategy)
        compilerRunner.kotlinDaemonJvmArguments.set(kotlinCompile.kotlinDaemonJvmArguments)
    }
}

@CacheableTask
abstract class KspTaskNative @Inject constructor(
    objectFactory: ObjectFactory,
) : KspTaskStandalone, DefaultTask() {
    @get:Nested
    val compilerRunner = createKotlinNativeCompilerRunner(this, objectFactory)

    @get:Internal
    val incrementalProps: List<FileCollection>
        get() = listOf(
            allSources,
            commonSources,
        )

    @get:Input
    abstract val target: Property<String>

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val args = KotlinNativeCompilerArguments().apply {
            // Common
            fromTask(this@KspTaskNative)

            // Native
            destination = this@KspTaskNative.destination
            libraries = this@KspTaskNative.libraries.files.toList()
            expectActualLinker = true
            friendPaths = this@KspTaskNative.friendPaths.files.toList()
            target = this@KspTaskNative.target.get()
        }

        // Clean outputs. Backups will be copied back if incremental.
        // TODO: leave outputs untouched and only restore outputs if build fails.
        outputs.files.forEach {
            it.deleteRecursively()
        }
        val changedFiles = getChangedFiles(inputChanges, incrementalProps)
        if (isKspIncremental) {
            if (changedFiles.hasNonSourceChange()) {
                clearIncCache()
            }
        } else {
            clearIncCache()
        }
        args.addChangedFiles(changedFiles)

        compilerRunner.runNativeCompilerAsync(
            args,
            allSources.toList(),
            commonSources.toList(),
            outputs.files.toList(),
        )
    }

    override fun configureCompilation(
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>,
        kotlinCompile: KotlinCompileTool,
    ) {
        target.value((kotlinCompile as KotlinNativeCompile).target)
    }
}

internal fun getChangedFiles(
    inputChanges: InputChanges,
    incrementalProps: List<FileCollection>,
) = if (!inputChanges.isIncremental) {
    ChangedFiles.Unknown()
} else {
    incrementalProps
        .fold(mutableListOf<File>() to mutableListOf<File>()) { (modified, removed), prop ->
            inputChanges.getFileChanges(prop).forEach {
                when (it.changeType) {
                    ChangeType.ADDED, ChangeType.MODIFIED -> modified.add(it.file)
                    ChangeType.REMOVED -> removed.add(it.file)
                    else -> Unit
                }
            }
            modified to removed
        }
        .run {
            ChangedFiles.Known(first, second)
        }
}

/** Properties related to the `kotlin.incremental.useClasspathSnapshot` feature. */
abstract class ClasspathSnapshotProperties {
    @get:Input
    abstract val useClasspathSnapshot: Property<Boolean>

    @get:Classpath
    @get:Incremental
    @get:Optional // Set if useClasspathSnapshot == true
    abstract val classpathSnapshot: ConfigurableFileCollection

    // Optional: Set if useClasspathSnapshot == false
    // (to restore the existing classpath annotations when the feature is disabled)
    @get:Classpath
    @get:Incremental
    @get:Optional
    abstract val classpath: ConfigurableFileCollection

    @get:OutputDirectory
    @get:Optional // Set if useClasspathSnapshot == true
    abstract val classpathSnapshotDir: DirectoryProperty
}
