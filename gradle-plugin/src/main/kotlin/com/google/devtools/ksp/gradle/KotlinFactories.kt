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

package com.google.devtools.ksp.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations
import org.gradle.util.GradleVersion
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.CLASS_STRUCTURE_ARTIFACT_TYPE
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.ClasspathSnapshot
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptClasspathChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformAction
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformLegacyAction
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup
import org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlin2JsCompileConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileCommonConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileConfig
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject

/**
 * TODO: Replace with KGP's Kotlin*Factory after:
 * https://youtrack.jetbrains.com/issue/KT-54986/KGP-API-to-toggle-incremental-compilation
 * https://youtrack.jetbrains.com/issue/KT-55031/KGP-API-to-create-compilation-tasks-of-JS-Metadata-and-Native
 */
class KotlinFactories {
    companion object {
        fun registerKotlinJvmCompileTask(
            project: Project,
            taskName: String,
            kotlinCompilation: KotlinCompilation<*>,
        ): TaskProvider<out KspTaskJvm> {
            return project.tasks.register(taskName, KspTaskJvm::class.java).also { kspTaskProvider ->
                KotlinCompileConfig(KotlinCompilationInfo(kotlinCompilation))
                    .execute(kspTaskProvider as TaskProvider<KotlinCompile>)
            }
        }

        fun registerKotlinJSCompileTask(
            project: Project,
            taskName: String,
            kotlinCompilation: KotlinCompilation<*>,
        ): TaskProvider<out KspTaskJS> {
            return project.tasks.register(taskName, KspTaskJS::class.java).also { kspTaskProvider ->
                BaseKotlin2JsCompileConfig<Kotlin2JsCompile>(KotlinCompilationInfo(kotlinCompilation))
                    .execute(kspTaskProvider as TaskProvider<Kotlin2JsCompile>)
            }
        }

        fun registerKotlinMetadataCompileTask(
            project: Project,
            taskName: String,
            kotlinCompilation: KotlinCompilation<*>,
        ): TaskProvider<out KspTaskMetadata> {
            return project.tasks.register(taskName, KspTaskMetadata::class.java).also { kspTaskProvider ->
                KotlinCompileCommonConfig(KotlinCompilationInfo(kotlinCompilation))
                    .execute(kspTaskProvider as TaskProvider<KotlinCompileCommon>)
            }
        }

        fun registerKotlinNativeCompileTask(
            project: Project,
            taskName: String,
            kotlinCompileTask: KotlinNativeCompile
        ): TaskProvider<out KspTaskNative> {
            return project.tasks.register(taskName, KspTaskNative::class.java, kotlinCompileTask.compilation).apply {
                configure { kspTask ->
                    kspTask.onlyIf {
                        kspTask.konanTarget.enabledOnCurrentHost
                    }
                }
            }
        }
    }
}

private val artifactType = Attribute.of("artifactType", String::class.java)

interface KspTask : Task {
    @get:Internal
    val options: ListProperty<SubpluginOption>

    @get:Nested
    val commandLineArgumentProviders: ListProperty<CommandLineArgumentProvider>

    @get:OutputDirectory
    var destination: File

    @get:Classpath
    val processorClasspath: ConfigurableFileCollection

    /**
     * Output directory that contains caches necessary to support incremental annotation processing.
     */
    @get:LocalState
    val kspCacheDir: DirectoryProperty

    @get:Input
    var isKspIncremental: Boolean
}

@CacheableTask
abstract class KspTaskJvm @Inject constructor(
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory
) : KotlinCompile(
    objectFactory.newInstance(KotlinJvmCompilerOptionsDefault::class.java),
    workerExecutor,
    objectFactory
),
    KspTask {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    @get:InputFiles
    @get:Incremental
    abstract val classpathStructure: ConfigurableFileCollection

    @get:Input
    var isIntermoduleIncremental: Boolean = false

    fun configureClasspathSnapshot() {
        isIntermoduleIncremental =
            (project.findProperty("ksp.incremental.intermodule")?.toString()?.toBoolean() ?: true) &&
                isKspIncremental
        if (isIntermoduleIncremental) {
            val classStructureIfIncremental = project.configurations.detachedConfiguration(
                project.dependencies.create(project.files(project.provider { libraries }))
            )
            maybeRegisterTransform(project)

            classpathStructure.from(
                classStructureIfIncremental.incoming.artifactView { viewConfig ->
                    viewConfig.attributes.attribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
                }.files
            ).disallowChanges()
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

    init {
        // Mute a warning from ScriptingGradleSubplugin, which tries to get `sourceSetName` before this task is
        // configured.
        sourceSetName.set("main")
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
        args.allowNoSourceFiles = true
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
    override val javaSources: FileCollection = super.javaSources.filter {
        !destination.isParentOf(it)
    }
}

@CacheableTask
abstract class KspTaskJS @Inject constructor(
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : Kotlin2JsCompile(
    objectFactory.newInstance(KotlinJsCompilerOptionsDefault::class.java),
    objectFactory,
    workerExecutor
),
    KspTask {

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
}

@CacheableTask
abstract class KspTaskMetadata @Inject constructor(
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory
) : KotlinCompileCommon(
    objectFactory.newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java),
    workerExecutor,
    objectFactory
),
    KspTask {

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
        args.expectActualLinker = true
        super.callCompilerAsync(args, kotlinSources, inputChanges, taskOutputsBackup)
    }
}

@CacheableTask
abstract class KspTaskNative @Inject internal constructor(
    compilation: KotlinCompilationInfo,
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory,
    execOperations: ExecOperations
) : KotlinNativeCompile(compilation, objectFactory, providerFactory, execOperations), KspTask {

    override val compilerOptions: KotlinCommonCompilerOptions =
        objectFactory.newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java)
}

// This forces rebuild.
private fun KspTask.clearIncCache() {
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

internal fun File.isParentOf(childCandidate: File): Boolean {
    val parentPath = Paths.get(this.absolutePath).normalize()
    val childCandidatePath = Paths.get(childCandidate.absolutePath).normalize()

    return childCandidatePath.startsWith(parentPath)
}
