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

package com.google.devtools.ksp.gradle

import com.google.devtools.ksp.gradle.model.builder.KspModelBuilder
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlin2JsCompileConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileCommonConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileConfig
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Callable
import javax.inject.Inject

class KspGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    KotlinCompilerPluginSupportPlugin {
    companion object {
        const val KSP_PLUGIN_ID = "com.google.devtools.ksp.symbol-processing"
        const val KSP_API_ID = "symbol-processing-api"
        const val KSP_COMPILER_PLUGIN_ID = "symbol-processing"
        const val KSP_GROUP_ID = "com.google.devtools.ksp"
        const val KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kspPluginClasspath"

        @JvmStatic
        fun getKspOutputDir(project: Project, sourceSetName: String, target: String) =
            File(project.project.buildDir, "generated/ksp/$target/$sourceSetName")

        @JvmStatic
        fun getKspClassOutputDir(project: Project, sourceSetName: String, target: String) =
            File(getKspOutputDir(project, sourceSetName, target), "classes")

        @JvmStatic
        fun getKspJavaOutputDir(project: Project, sourceSetName: String, target: String) =
            File(getKspOutputDir(project, sourceSetName, target), "java")

        @JvmStatic
        fun getKspKotlinOutputDir(project: Project, sourceSetName: String, target: String) =
            File(getKspOutputDir(project, sourceSetName, target), "kotlin")

        @JvmStatic
        fun getKspResourceOutputDir(project: Project, sourceSetName: String, target: String) =
            File(getKspOutputDir(project, sourceSetName, target), "resources")

        @JvmStatic
        fun getKspCachesDir(project: Project, sourceSetName: String, target: String) =
            File(project.project.buildDir, "kspCaches/$target/$sourceSetName")

        @JvmStatic
        private fun getSubpluginOptions(
            project: Project,
            kspExtension: KspExtension,
            classpath: Configuration,
            sourceSetName: String,
            target: String,
            isIncremental: Boolean,
            allWarningsAsErrors: Boolean,
            commandLineArgumentProviders: ListProperty<CommandLineArgumentProvider>,
        ): List<SubpluginOption> {
            val options = mutableListOf<SubpluginOption>()
            options += InternalSubpluginOption("classOutputDir", getKspClassOutputDir(project, sourceSetName, target).path)
            options += InternalSubpluginOption("javaOutputDir", getKspJavaOutputDir(project, sourceSetName, target).path)
            options += InternalSubpluginOption("kotlinOutputDir", getKspKotlinOutputDir(project, sourceSetName, target).path)
            options += InternalSubpluginOption(
                "resourceOutputDir",
                getKspResourceOutputDir(project, sourceSetName, target).path
            )
            options += InternalSubpluginOption("cachesDir", getKspCachesDir(project, sourceSetName, target).path)
            options += InternalSubpluginOption("kspOutputDir", getKspOutputDir(project, sourceSetName, target).path)
            options += SubpluginOption("incremental", isIncremental.toString())
            options += SubpluginOption(
                "incrementalLog",
                project.findProperty("ksp.incremental.log")?.toString() ?: "false"
            )
            options += InternalSubpluginOption("projectBaseDir", project.project.projectDir.canonicalPath)
            options += SubpluginOption("allWarningsAsErrors", allWarningsAsErrors.toString())
            options += FilesSubpluginOption("apclasspath", classpath.toList())
            // Turn this on by default to work KT-30172 around. It is off by default in the compiler plugin.
            options += SubpluginOption(
                "returnOkOnError",
                project.findProperty("ksp.return.ok.on.error")?.toString() ?: "true"
            )

            kspExtension.apOptions.forEach {
                options += SubpluginOption("apoption", "${it.key}=${it.value}")
            }
            commandLineArgumentProviders.get().forEach {
                val argument = it.asArguments().joinToString("")
                if (!argument.matches(Regex("\\S+=\\S+"))) {
                    throw IllegalArgumentException("KSP apoption does not match \\S+=\\S+: $argument")
                }
                options += SubpluginOption("apoption", argument)
            }
            return options
        }
    }

    private lateinit var kspConfigurations: KspConfigurations

    override fun apply(target: Project) {
        target.extensions.create("ksp", KspExtension::class.java)
        kspConfigurations = KspConfigurations(target)
        registry.register(KspModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        val kspVersion = ApiVersion.parse(KSP_KOTLIN_BASE_VERSION)!!
        val kotlinVersion = ApiVersion.parse(project.getKotlinPluginVersion())!!

        // Check version and show warning by default.
        val noVersionCheck = project.findProperty("ksp.version.check")?.toString()?.toBoolean() == false
        if (!noVersionCheck) {
            if (kspVersion < kotlinVersion) {
                project.logger.warn(
                    "ksp-$KSP_VERSION is too old for kotlin-$kotlinVersion. " +
                        "Please upgrade ksp or downgrade kotlin-gradle-plugin to $KSP_KOTLIN_BASE_VERSION."
                )
            }
            if (kspVersion > kotlinVersion) {
                project.logger.warn(
                    "ksp-$KSP_VERSION is too new for kotlin-$kotlinVersion. " +
                        "Please upgrade kotlin-gradle-plugin to $KSP_KOTLIN_BASE_VERSION."
                )
            }
        }

        return true
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val kotlinCompileProvider: TaskProvider<AbstractKotlinCompileTool<*>> =
            project.locateTask(kotlinCompilation.compileKotlinTaskName) ?: return project.provider { emptyList() }
        val javaCompile = findJavaTaskForKotlinCompilation(kotlinCompilation)?.get()
        val kspExtension = project.extensions.getByType(KspExtension::class.java)
        val kspConfigurations = kspConfigurations.find(kotlinCompilation)
        val nonEmptyKspConfigurations = kspConfigurations.filter { it.allDependencies.isNotEmpty() }
        if (nonEmptyKspConfigurations.isEmpty()) {
            return project.provider { emptyList() }
        }
        if (kotlinCompileProvider.name == "compileKotlinMetadata") {
            return project.provider { emptyList() }
        }

        val target = kotlinCompilation.target.name
        val sourceSetName = kotlinCompilation.defaultSourceSet.name
        val classOutputDir = getKspClassOutputDir(project, sourceSetName, target)
        val javaOutputDir = getKspJavaOutputDir(project, sourceSetName, target)
        val kotlinOutputDir = getKspKotlinOutputDir(project, sourceSetName, target)
        val resourceOutputDir = getKspResourceOutputDir(project, sourceSetName, target)
        val kspOutputDir = getKspOutputDir(project, sourceSetName, target)

        val kspClasspathCfg = project.configurations.maybeCreate(KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME)
        project.dependencies.add(
            KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME,
            "$KSP_GROUP_ID:$KSP_API_ID:$KSP_VERSION"
        )
        project.dependencies.add(
            KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME,

            "$KSP_GROUP_ID:$KSP_COMPILER_PLUGIN_ID:$KSP_VERSION"
        )

        if (javaCompile != null) {
            val generatedJavaSources = javaCompile.project.fileTree(javaOutputDir)
            generatedJavaSources.include("**/*.java")
            javaCompile.source(generatedJavaSources)
            javaCompile.classpath += project.files(classOutputDir)
        }

        assert(kotlinCompileProvider.name.startsWith("compile"))
        val kspTaskName = kotlinCompileProvider.name.replaceFirst("compile", "ksp")

        val kotlinCompileTask = kotlinCompileProvider.get()

        fun configureAsKspTask(kspTask: KspTask, isIncremental: Boolean) {
            // depends on the processor; if the processor changes, it needs to be reprocessed.
            val processorClasspath = project.configurations.maybeCreate("${kspTaskName}ProcessorClasspath")
                .extendsFrom(*nonEmptyKspConfigurations.toTypedArray())
            kspTask.processorClasspath.from(processorClasspath)
            kspTask.dependsOn(processorClasspath.buildDependencies)
            kspTask.commandLineArgumentProviders.addAll(kspExtension.commandLineArgumentProviders)

            kspTask.options.addAll(
                kspTask.project.provider {
                    getSubpluginOptions(
                        project,
                        kspExtension,
                        processorClasspath,
                        sourceSetName,
                        target,
                        isIncremental,
                        kspExtension.allWarningsAsErrors,
                        kspTask.commandLineArgumentProviders
                    )
                }
            )
            kspTask.destination = kspOutputDir
            kspTask.apOptions.value(kspExtension.arguments).disallowChanges()
            kspTask.kspCacheDir.fileValue(getKspCachesDir(project, sourceSetName, target)).disallowChanges()

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
                    (it as? TaskProvider<*>)?.name == kspTaskName ||
                        (it as? Task)?.name == kspTaskName
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
            kspTask.exclude { kspOutputDir.isParentOf(it.file) }

            // kotlinc's incremental compilation isn't compatible with symbol processing in a few ways:
            // * It doesn't consider private / internal changes when computing dirty sets.
            // * It compiles iteratively; Sources can be compiled in different rounds.
            (kspTask as? AbstractKotlinCompile<*>)?.incremental = false
        }

        fun maybeBlockOtherPlugins(kspTask: BaseKotlinCompile) {
            if (kspExtension.blockOtherCompilerPlugins) {
                kspTask.pluginClasspath.setFrom(kspClasspathCfg)
                kspTask.pluginOptions.set(emptyList())
            }
        }

        fun configurePluginOptions(kspTask: BaseKotlinCompile) {
            kspTask.pluginOptions.add(
                project.provider {
                    CompilerPluginConfig().apply {
                        (kspTask as KspTask).options.get().forEach {
                            addPluginArgument(KSP_PLUGIN_ID, it)
                        }
                    }
                }
            )
        }

        // Create KSP tasks and configure later.
        val kspTaskProvider = when (kotlinCompileTask) {
            is KotlinCompile ->
                project.tasks.register(kspTaskName, KspTaskJvm::class.java)
            is Kotlin2JsCompile ->
                project.tasks.register(kspTaskName, KspTaskJS::class.java)
            is KotlinCompileCommon ->
                project.tasks.register(kspTaskName, KspTaskMetadata::class.java)
            is KotlinNativeCompile ->
                project.tasks.register(kspTaskName, KspTaskNative::class.java, kotlinCompileTask.compilation)
            else -> return project.provider { emptyList() }
        }

        val isIncremental = project.findProperty("ksp.incremental")?.toString()?.toBoolean() ?: true

        // Configure KSP tasks
        when (kotlinCompileTask) {
            is KotlinCompile -> {
                kspTaskProvider.configure { kspTask ->
                    kspTask.libraries.setFrom(kotlinCompileTask.project.files(Callable { kotlinCompileTask.libraries }))
                }
                KotlinCompileConfig(KotlinCompilationInfo(kotlinCompilation))
                    .execute(kspTaskProvider as TaskProvider<KotlinCompile>)
                kspTaskProvider.configure { kspTask ->
                    kspTask as KspTaskJvm
                    maybeBlockOtherPlugins(kspTask as BaseKotlinCompile)
                    configureAsKspTask(kspTask as KspTask, isIncremental)
                    configureAsAbstractKotlinCompileTool(kspTask as AbstractKotlinCompileTool<*>)
                    configurePluginOptions(kspTask)
                    kspTask.configureClasspathSnapshot()
                    kspTask.compilerOptions.noJdk.value(kotlinCompileTask.compilerOptions.noJdk)
                    kspTask.compilerOptions.useK2.value(false)
                    kspTask.compilerOptions.moduleName.convention(kotlinCompileTask.moduleName.map { "$it-ksp" })
                    kspTask.moduleName.value(kotlinCompileTask.moduleName.get())
                }
                // Don't support binary generation for non-JVM platforms yet.
                // FIXME: figure out how to add user generated libraries.
                kotlinCompilation.output.classesDirs.from(classOutputDir)
            }
            is Kotlin2JsCompile -> {
                kspTaskProvider.configure { kspTask ->
                    kspTask.libraries.setFrom(kotlinCompileTask.project.files(Callable { kotlinCompileTask.libraries }))
                    kspTask.compilerOptions.freeCompilerArgs.value(kotlinCompileTask.compilerOptions.freeCompilerArgs)
                }
                BaseKotlin2JsCompileConfig<Kotlin2JsCompile>(KotlinCompilationInfo(kotlinCompilation))
                    .execute(kspTaskProvider as TaskProvider<Kotlin2JsCompile>)
                kspTaskProvider.configure { kspTask ->
                    kspTask as KspTaskJS
                    maybeBlockOtherPlugins(kspTask as BaseKotlinCompile)
                    configureAsKspTask(kspTask as KspTask, isIncremental)
                    configureAsAbstractKotlinCompileTool(kspTask as AbstractKotlinCompileTool<*>)
                    configurePluginOptions(kspTask)
                    kspTask.compilerOptions.useK2.value(false)
                    kspTask.compilerOptions.moduleName.convention(kotlinCompileTask.moduleName.map { "$it-ksp" })
                    (kspTask as KspTaskJS).incrementalJsKlib = false
                }
            }
            is KotlinCompileCommon -> {
                kspTaskProvider.configure { kspTask ->
                    kspTask.libraries.setFrom(kotlinCompileTask.project.files(Callable { kotlinCompileTask.libraries }))
                }
                KotlinCompileCommonConfig(KotlinCompilationInfo(kotlinCompilation))
                    .execute(kspTaskProvider as TaskProvider<KotlinCompileCommon>)
                kspTaskProvider.configure { kspTask ->
                    maybeBlockOtherPlugins(kspTask as BaseKotlinCompile)
                    configureAsKspTask(kspTask as KspTask, isIncremental)
                    configureAsAbstractKotlinCompileTool(kspTask as AbstractKotlinCompileTool<*>)
                    configurePluginOptions(kspTask)
                    kspTask.compilerOptions.useK2.value(false)
                }
            }
            is KotlinNativeCompile -> {
                kspTaskProvider.configure { kspTask ->
                    kspTask as KspTaskNative
                    kspTask.onlyIf {
                        kspTask.konanTarget.enabledOnCurrentHost
                    }
                    configureAsKspTask(kspTask, false)
                    configureAsAbstractKotlinCompileTool(kspTask)

                    // KotlinNativeCompile computes -Xplugin=... from compilerPluginClasspath.
                    if (kspExtension.blockOtherCompilerPlugins) {
                        kspTask.compilerPluginClasspath = kspClasspathCfg
                    } else {
                        kspTask.compilerPluginClasspath =
                            kspClasspathCfg + kotlinCompileTask.compilerPluginClasspath!!
                        kspTask.compilerPluginOptions.addPluginArgument(kotlinCompileTask.compilerPluginOptions)
                    }
                    kspTask.commonSources.from(kotlinCompileTask.commonSources)
                    val kspOptions = kspTask.options.get().flatMap { listOf("-P", it.toArg()) }
                    kspTask.compilerOptions.freeCompilerArgs.value(
                        kspOptions + kotlinCompileTask.compilerOptions.freeCompilerArgs.get()
                    )
                    kspTask.doFirst {
                        kspOutputDir.deleteRecursively()
                    }
                }
            }
        }

        kotlinCompileProvider.configure { kotlinCompile ->
            kotlinCompile.dependsOn(kspTaskProvider)
            kotlinCompile.setSource(kotlinOutputDir, javaOutputDir)
            when (kotlinCompile) {
                is AbstractKotlinCompile<*> -> kotlinCompile.libraries.from(project.files(classOutputDir))
                // is KotlinNativeCompile -> TODO: support binary generation?
            }
        }

        val processResourcesTaskName =
            (kotlinCompilation as? KotlinCompilationWithResources)?.processResourcesTaskName ?: "processResources"
        project.locateTask<ProcessResources>(processResourcesTaskName)?.let { provider ->
            provider.configure { resourcesTask ->
                resourcesTask.dependsOn(kspTaskProvider)
                resourcesTask.from(resourceOutputDir)
            }
        }
        if (kotlinCompilation is KotlinJvmAndroidCompilation) {
            AndroidPluginIntegration.registerGeneratedSources(
                project = project,
                kotlinCompilation = kotlinCompilation,
                kspTaskProvider = kspTaskProvider as TaskProvider<KspTaskJvm>,
                javaOutputDir = javaOutputDir,
                kotlinOutputDir = kotlinOutputDir,
                classOutputDir = classOutputDir,
                resourcesOutputDir = project.files(resourceOutputDir)
            )
        }

        return project.provider { emptyList() }
    }

    override fun getCompilerPluginId() = KSP_PLUGIN_ID
    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "com.google.devtools.ksp",
            artifactId = KSP_COMPILER_PLUGIN_ID,
            version = KSP_VERSION
        )

    override fun getPluginArtifactForNative(): SubpluginArtifact? =
        SubpluginArtifact(
            groupId = "com.google.devtools.ksp",
            artifactId = KSP_COMPILER_PLUGIN_ID,
            version = KSP_VERSION
        )

    val apiArtifact = "com.google.devtools.ksp:symbol-processing-api:$KSP_VERSION"
}

private val artifactType = Attribute.of("artifactType", String::class.java)

// Copied from kotlin-gradle-plugin, because they are internal.
internal inline fun <reified T : Task> Project.locateTask(name: String): TaskProvider<T>? =
    try {
        tasks.withType(T::class.java).named(name)
    } catch (e: UnknownTaskException) {
        null
    }

// Copied from kotlin-gradle-plugin, because they are internal.
internal fun findJavaTaskForKotlinCompilation(compilation: KotlinCompilation<*>): TaskProvider<out JavaCompile>? =
    when (compilation) {
        is KotlinJvmAndroidCompilation -> compilation.compileJavaTaskProvider
        is KotlinWithJavaCompilation<*, *> -> compilation.compileJavaTaskProvider
        is KotlinJvmCompilation -> compilation.compileJavaTaskProvider // may be null for Kotlin-only JVM target in MPP
        else -> null
    }

interface KspTask : Task {
    @get:Internal
    val options: ListProperty<SubpluginOption>

    @get:Nested
    val commandLineArgumentProviders: ListProperty<CommandLineArgumentProvider>

    @get:OutputDirectory
    var destination: File

    @get:Input
    val apOptions: MapProperty<String, String>

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
