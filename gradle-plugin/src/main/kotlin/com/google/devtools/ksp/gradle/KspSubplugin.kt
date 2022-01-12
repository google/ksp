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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.compilerArgumentsConfigurationFlags
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinNativeCompilationData
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile.Configurator
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Callable
import javax.inject.Inject
import kotlin.reflect.KProperty1

class KspGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    KotlinCompilerPluginSupportPlugin {
    companion object {
        const val KSP_MAIN_CONFIGURATION_NAME = "ksp"
        const val KSP_ARTIFACT_NAME = "symbol-processing"
        const val KSP_ARTIFACT_NAME_NATIVE = "symbol-processing-cmdline"
        const val KSP_PLUGIN_ID = "com.google.devtools.ksp.symbol-processing"

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
        ): List<SubpluginOption> {
            val options = mutableListOf<SubpluginOption>()
            options += SubpluginOption("classOutputDir", getKspClassOutputDir(project, sourceSetName, target).path)
            options += SubpluginOption("javaOutputDir", getKspJavaOutputDir(project, sourceSetName, target).path)
            options += SubpluginOption("kotlinOutputDir", getKspKotlinOutputDir(project, sourceSetName, target).path)
            options += SubpluginOption(
                "resourceOutputDir",
                getKspResourceOutputDir(project, sourceSetName, target).path
            )
            options += SubpluginOption("cachesDir", getKspCachesDir(project, sourceSetName, target).path)
            options += SubpluginOption("kspOutputDir", getKspOutputDir(project, sourceSetName, target).path)
            options += SubpluginOption("incremental", isIncremental.toString())
            options += SubpluginOption(
                "incrementalLog",
                project.findProperty("ksp.incremental.log")?.toString() ?: "false"
            )
            options += SubpluginOption("projectBaseDir", project.project.projectDir.canonicalPath)
            options += SubpluginOption("allWarningsAsErrors", allWarningsAsErrors.toString())
            options += FilesSubpluginOption("apclasspath", classpath.toList())

            kspExtension.apOptions.forEach {
                options += SubpluginOption("apoption", "${it.key}=${it.value}")
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
        val kotlinCompileProvider: TaskProvider<AbstractCompile> =
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
        val sourceSetName = kotlinCompilation.defaultSourceSetName
        val classOutputDir = getKspClassOutputDir(project, sourceSetName, target)
        val javaOutputDir = getKspJavaOutputDir(project, sourceSetName, target)
        val kotlinOutputDir = getKspKotlinOutputDir(project, sourceSetName, target)
        val resourceOutputDir = getKspResourceOutputDir(project, sourceSetName, target)
        val kspOutputDir = getKspOutputDir(project, sourceSetName, target)

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
            kspTask.destination = kspOutputDir
            kspTask.blockOtherCompilerPlugins = kspExtension.blockOtherCompilerPlugins
            kspTask.apOptions.value(kspExtension.arguments).disallowChanges()
            kspTask.kspCacheDir.fileValue(getKspCachesDir(project, sourceSetName, target)).disallowChanges()

            if (kspExtension.blockOtherCompilerPlugins) {
                // FIXME: ask upstream to provide an API to make this not implementation-dependent.
                val cfg = project.configurations.getByName(kotlinCompilation.pluginConfigurationName)
                kspTask.overridePluginClasspath.value(
                    kspTask.project.provider {
                        val dep = cfg.dependencies.single { it.name == KSP_ARTIFACT_NAME }
                        cfg.fileCollection(dep)
                    }
                )
            }
            kspTask.isKspIncremental = isIncremental
        }

        fun configureAsAbstractCompile(kspTask: AbstractCompile) {
            kspTask.getDestinationDirectory().set(kspOutputDir)
            kspTask.outputs.dirs(
                kotlinOutputDir,
                javaOutputDir,
                classOutputDir,
                resourceOutputDir
            )
            if (kspExtension.allowSourcesFromOtherPlugins) {
                kspTask.source(kotlinCompileTask.source)
                if (kotlinCompileTask is AbstractKotlinCompile<*>) {
                    val sourceRoots = kotlinCompileTask.getSourceRoots()
                    if (sourceRoots is SourceRoots.ForJvm) {
                        kspTask.source(sourceRoots.javaSourceRoots)
                    }
                }
            } else {
                kotlinCompilation.allKotlinSourceSets.forEach { sourceSet -> kspTask.source(sourceSet.kotlin) }
                if (kotlinCompilation is KotlinCommonCompilation) {
                    kspTask.source(kotlinCompilation.defaultSourceSet.kotlin)
                }
            }
            kspTask.source.filter { !kspOutputDir.isParentOf(it) }

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
                    else -> return project.provider { emptyList() }
                }
                val isIncremental = project.findProperty("ksp.incremental")?.toString()?.toBoolean() ?: true
                project.tasks.register(kspTaskName, kspTaskClass) { kspTask ->
                    configureAsKspTask(kspTask, isIncremental)
                    configureAsAbstractCompile(kspTask)

                    kspTask.classpath = kotlinCompileTask.project.files(Callable { kotlinCompileTask.classpath })
                    kspTask.configureCompilation(
                        kotlinCompilation as KotlinCompilationData<*>,
                        kotlinCompileTask,
                    )
                }
            }
            is KotlinNativeCompile -> {
                val kspTaskClass = KspTaskNative::class.java
                val pluginConfigurationName =
                    (kotlinCompileTask.compilation as AbstractKotlinNativeCompilation).pluginConfigurationName
                project.configurations.getByName(pluginConfigurationName).dependencies.add(
                    project.dependencies.create(apiArtifact)
                )
                project.tasks.register(kspTaskName, kspTaskClass, kotlinCompileTask.compilation).apply {
                    configure { kspTask ->
                        kspTask.onlyIf {
                            kotlinCompileTask.compilation.konanTarget.enabledOnCurrentHost
                        }
                        configureAsKspTask(kspTask, false)
                        configureAsAbstractCompile(kspTask)

                        // KotlinNativeCompile computes -Xplugin=... from compilerPluginClasspath.
                        kspTask.compilerPluginClasspath = project.configurations.getByName(pluginConfigurationName)
                        kspTask.commonSources.from(kotlinCompileTask.commonSources)
                    }
                }
            }
            else -> return project.provider { emptyList() }
        }

        kotlinCompileProvider.configure { kotlinCompile ->
            kotlinCompile.dependsOn(kspTaskProvider)
            kotlinCompile.source(kotlinOutputDir, javaOutputDir)
            when (kotlinCompile) {
                is AbstractKotlinCompile<*> -> kotlinCompile.classpath += project.files(classOutputDir)
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
            AndroidPluginIntegration.registerGeneratedJavaSources(
                project = project,
                kotlinCompilation = kotlinCompilation,
                kspTaskProvider = kspTaskProvider as TaskProvider<KspTaskJvm>,
                javaOutputDir = javaOutputDir,
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
            artifactId = KSP_ARTIFACT_NAME,
            version = KSP_VERSION
        )

    override fun getPluginArtifactForNative(): SubpluginArtifact? =
        SubpluginArtifact(
            groupId = "com.google.devtools.ksp",
            artifactId = KSP_ARTIFACT_NAME_NATIVE,
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
        is KotlinWithJavaCompilation -> compilation.compileJavaTaskProvider
        is KotlinJvmCompilation -> compilation.compileJavaTaskProvider // may be null for Kotlin-only JVM target in MPP
        else -> null
    }

interface KspTask : Task {
    @get:Internal
    val options: ListProperty<SubpluginOption>

    @get:OutputDirectory
    var destination: File

    @get:Optional
    @get:Classpath
    val overridePluginClasspath: Property<FileCollection>

    @get:Input
    var blockOtherCompilerPlugins: Boolean

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

    fun configureCompilation(
        kotlinCompilation: KotlinCompilationData<*>,
        kotlinCompile: AbstractKotlinCompile<*>,
    )
}

@CacheableTask
abstract class KspTaskJvm @Inject constructor(
    workerExecutor: WorkerExecutor
) : KotlinCompile(KotlinJvmOptionsImpl(), workerExecutor), KspTask {
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
        Configurator<KspTaskJvm>(kotlinCompilation).configure(this)
        // Assign moduleName different from kotlin compilation to work around https://github.com/google/ksp/issues/647
        // This will not be necessary once https://youtrack.jetbrains.com/issue/KT-45777 lands
        this.moduleName.set(kotlinCompile.moduleName.map { "$it-ksp" })
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
                project.dependencies.create(project.files(project.provider { kotlinCompile.classpath }))
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
                classpath.files.toList(),
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
            args.blockOtherPlugins(overridePluginClasspath.get())
        }
        args.addPluginOptions(options.get())
        args.destinationAsFile = destination
        args.allowNoSourceFiles = true
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `callCompilerAsync$kotlin_gradle_plugin`(
        args: K2JVMCompilerArguments,
        sourceRoots: SourceRoots,
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
        super.callCompilerAsync(args, sourceRoots, inputChanges, taskOutputsBackup)
    }

    override fun skipCondition(): Boolean = false
}

@CacheableTask
abstract class KspTaskJS @Inject constructor(
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : Kotlin2JsCompile(KotlinJsOptionsImpl(), objectFactory, workerExecutor), KspTask {
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
        Configurator<KspTaskJS>(kotlinCompilation).configure(this)
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
        args.fillDefaultValues()
        compileKotlinArgumentsContributor.get().contributeArguments(
            args,
            compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
            )
        )
        if (blockOtherCompilerPlugins) {
            args.blockOtherPlugins(overridePluginClasspath.get())
        }
        args.addPluginOptions(options.get())
        args.outputFile = File(destination, "dummyOutput.js").canonicalPath
        kotlinOptions.copyFreeCompilerArgsToArgs(args)
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `callCompilerAsync$kotlin_gradle_plugin`(
        args: K2JSCompilerArguments,
        sourceRoots: SourceRoots,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        val changedFiles = getChangedFiles(inputChanges, incrementalProps)
        if (!isKspIncremental || changedFiles.hasNonSourceChange()) {
            clearIncCache()
        } else {
            args.addChangedFiles(changedFiles)
        }
        super.callCompilerAsync(args, sourceRoots, inputChanges, taskOutputsBackup)
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `isIncrementalCompilationEnabled$kotlin_gradle_plugin`(): Boolean = false
}

@CacheableTask
abstract class KspTaskMetadata @Inject constructor(
    workerExecutor: WorkerExecutor
) : KotlinCompileCommon(KotlinMultiplatformCommonOptionsImpl(), workerExecutor), KspTask {
    override fun configureCompilation(
        kotlinCompilation: KotlinCompilationData<*>,
        kotlinCompile: AbstractKotlinCompile<*>,
    ) {
        Configurator<KspTaskMetadata>(kotlinCompilation).configure(this)
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
        args.apply { fillDefaultValues() }
        compileKotlinArgumentsContributor.get().contributeArguments(
            args,
            compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
            )
        )
        if (blockOtherCompilerPlugins) {
            args.blockOtherPlugins(overridePluginClasspath.get())
        }
        args.addPluginOptions(options.get())
        args.destination = destination.canonicalPath
        val classpathList = classpath.files.filter { it.exists() }.toMutableList()
        args.classpath = classpathList.joinToString(File.pathSeparator)
        args.friendPaths = friendPaths.files.map { it.absolutePath }.toTypedArray()
        args.refinesPaths = refinesMetadataPaths.map { it.absolutePath }.toTypedArray()
        args.expectActualLinker = true
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `callCompilerAsync$kotlin_gradle_plugin`(
        args: K2MetadataCompilerArguments,
        sourceRoots: SourceRoots,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        val changedFiles = getChangedFiles(inputChanges, incrementalProps)
        if (!isKspIncremental || changedFiles.hasNonSourceChange()) {
            clearIncCache()
        } else {
            args.addChangedFiles(changedFiles)
        }
        super.callCompilerAsync(args, sourceRoots, inputChanges, taskOutputsBackup)
    }
}

@CacheableTask
abstract class KspTaskNative @Inject constructor(
    injected: KotlinNativeCompilationData<*>,
) : KotlinNativeCompile(injected), KspTask {
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
                field = overridePluginClasspath.get()
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
