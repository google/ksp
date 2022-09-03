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
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
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
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import org.jetbrains.kotlin.utils.JsLibraryUtils
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.reflect.KProperty1

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
            } else if (kotlinCompile is AbstractKotlinCompile<*>) {
                kspTask.commonSources.from(providers.provider { kotlinCompile.commonSourceSet })
                kspTask.friendPaths.from(kotlinCompile.friendPaths)
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

fun CommonCompilerArguments.addChangedClasses(changed: KaptClasspathChanges) {
    if (changed is KaptClasspathChanges.Known) {
        changed.names.map { it.replace('/', '.').replace('$', '.') }.ifNotEmpty {
            addPluginOptions(listOf(SubpluginOption("changedClasses", joinToString(":"))))
        }
    }
}

fun SubpluginOption.toArg() = "plugin:${KspGradleSubplugin.KSP_PLUGIN_ID}:$key=$value"

fun CommonCompilerArguments.addPluginOptions(options: List<SubpluginOption>) {
    pluginOptions = ((pluginOptions?.toList() ?: emptyList()) + options.map { it.toArg() }).toTypedArray()
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
    val value = property.get(this)
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
        val args = K2JVMCompilerArguments().apply {
            // Common
            useK2 = false
            incrementalCompilation = false
            pluginClasspaths = pluginClasspath.files.map { it.absolutePath }.toTypedArray()
            apiVersion = this@KspTaskJvm.kotlinApiVersion.orNull
            languageVersion = this@KspTaskJvm.kotlinLanguageVersion.orNull
            verbose = this@KspTaskJvm.verbose.get()
            noJdk = this@KspTaskJvm.noJdk.get()

            addPluginOptions(options.get())

            // JVM
            allowNoSourceFiles = true
            destinationAsFile = this@KspTaskJvm.destination
            noReflect = true
            noStdlib = true
            classpathAsList = libraries.files.toList()
            friendPaths = this@KspTaskJvm.friendPaths.map { it.absolutePath }.toTypedArray()
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
    // TODO: setup by checking target info, instead of copying from compile task
    private val backendSelectionArgs = listOf(
        "-Xir-only",
        "-Xir-produce-js",
        "-Xir-produce-klib-dir",
        "-Xir-produce-klib-file",
        "-Xwasm"
    )

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

    private fun isJsLib(file: File): Boolean =
        file.exists() && when (backend.get()) {
            JS_BACKEND.JS_IR -> isKotlinLibrary(file)
            JS_BACKEND.JS_LEGACY -> JsLibraryUtils.isKotlinJavascriptLibrary(file)
            JS_BACKEND.JS_BOTH -> JsLibraryUtils.isKotlinJavascriptLibrary(file) && isKotlinLibrary(file)
        }

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val args = K2JSCompilerArguments().apply {
            // Common
            useK2 = false
            incrementalCompilation = false
            pluginClasspaths = pluginClasspath.files.map { it.absolutePath }.toTypedArray()
            apiVersion = this@KspTaskJs.kotlinApiVersion.orNull
            languageVersion = this@KspTaskJs.kotlinLanguageVersion.orNull
            verbose = this@KspTaskJs.verbose.get()

            addPluginOptions(options.get())

            // JS
            destination = this@KspTaskJs.destination
            noStdlib = true
            libraries = this@KspTaskJs.libraries.filter { isJsLib(it) }.asPath
            freeArgs = this@KspTaskJs.freeArgs.get()
            outputFile = File(destination, "dummyOutput.js").canonicalPath
            friendModules = this@KspTaskJs.friendPaths.filter { isJsLib(it) }.asPath
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

    @get:Internal
    abstract val backend: Property<JS_BACKEND>

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

        val kotlinJsOptions = kotlinCompilation.kotlinOptions as KotlinJsOptions
        backend.value(
            when {
                kotlinJsOptions.isIrBackendEnabled() && kotlinJsOptions.isPreIrBackendDisabled() ->
                    JS_BACKEND.JS_IR
                !kotlinJsOptions.isIrBackendEnabled() && !kotlinJsOptions.isPreIrBackendDisabled() ->
                    JS_BACKEND.JS_LEGACY
                kotlinJsOptions.isIrBackendEnabled() && !kotlinJsOptions.isPreIrBackendDisabled() ->
                    JS_BACKEND.JS_BOTH
                else -> throw IllegalArgumentException("Unknown JS backend")
            }
        )
        freeArgs.value(
            (kotlinCompile as Kotlin2JsCompile).kotlinOptions.freeCompilerArgs.filter {
                it in backendSelectionArgs
            }
        )
    }

    enum class JS_BACKEND {
        JS_IR,
        JS_LEGACY,
        JS_BOTH
    }

    private fun KotlinJsOptions.isPreIrBackendDisabled(): Boolean =
        listOf(
            "-Xir-only",
            "-Xir-produce-js",
            "-Xir-produce-klib-file"
        ).any(freeCompilerArgs::contains)

    // see also isIncrementalCompilationEnabled
    private fun KotlinJsOptions.isIrBackendEnabled(): Boolean =
        listOf(
            "-Xir-produce-klib-dir",
            "-Xir-produce-js",
            "-Xir-produce-klib-file"
        ).any(freeCompilerArgs::contains)
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
        val args = K2MetadataCompilerArguments().apply {
            // Common
            useK2 = false
            incrementalCompilation = false
            pluginClasspaths = pluginClasspath.files.map { it.absolutePath }.toTypedArray()
            apiVersion = this@KspTaskMetadata.kotlinApiVersion.orNull
            languageVersion = this@KspTaskMetadata.kotlinLanguageVersion.orNull
            verbose = this@KspTaskMetadata.verbose.get()

            addPluginOptions(options.get())

            // Metadata
            destination = this@KspTaskMetadata.destination.absolutePath
            classpath = this@KspTaskMetadata.libraries.joinToString(":")
            multiPlatform = true
            expectActualLinker = true
            friendPaths = this@KspTaskMetadata.friendPaths.map { it.absolutePath }.toTypedArray()
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
        val args = K2NativeCompilerArguments().apply {
            // Common
            useK2 = false
            incrementalCompilation = false
            pluginClasspaths = pluginClasspath.files.map { it.absolutePath }.toTypedArray()
            apiVersion = this@KspTaskNative.kotlinApiVersion.orNull
            languageVersion = this@KspTaskNative.kotlinLanguageVersion.orNull
            verbose = this@KspTaskNative.verbose.get()

            addPluginOptions(options.get())

            // Native
            destination = this@KspTaskNative.destination
            libraries = this@KspTaskNative.libraries.map { it.absolutePath }.toTypedArray()
            multiPlatform = true
            expectActualLinker = true
            friendModules = this@KspTaskNative.friendPaths.asPath
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
