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
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.fillDefaultValues
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.compilerArgumentsConfigurationFlags
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.CLASS_STRUCTURE_ARTIFACT_TYPE
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.ClasspathSnapshot
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptClasspathChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformAction
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformLegacyAction
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinNativeCompilationData
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile.Configurator
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.SourceRoots
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import javax.inject.Inject
import kotlin.collections.LinkedHashSet
import kotlin.reflect.KProperty1

class KspGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
        KotlinCompilerPluginSupportPlugin {
    companion object {
        const val KSP_MAIN_CONFIGURATION_NAME = "ksp"
        const val KSP_ARTIFACT_NAME = "symbol-processing"
        const val KSP_ARTIFACT_NAME_NATIVE = "symbol-processing-cmdline"
        const val KSP_PLUGIN_ID = "com.google.devtools.ksp.symbol-processing"

        @JvmStatic
        fun getKspOutputDir(project: Project, sourceSetName: String) =
                File(project.project.buildDir, "generated/ksp/$sourceSetName")

        @JvmStatic
        fun getKspClassOutputDir(project: Project, sourceSetName: String) =
                File(getKspOutputDir(project, sourceSetName), "classes")

        @JvmStatic
        fun getKspJavaOutputDir(project: Project, sourceSetName: String) =
                File(getKspOutputDir(project, sourceSetName), "java")

        @JvmStatic
        fun getKspKotlinOutputDir(project: Project, sourceSetName: String) =
                File(getKspOutputDir(project, sourceSetName), "kotlin")

        @JvmStatic
        fun getKspResourceOutputDir(project: Project, sourceSetName: String) =
                File(getKspOutputDir(project, sourceSetName), "resources")

        @JvmStatic
        fun getKspCachesDir(project: Project, sourceSetName: String) =
                File(project.project.buildDir, "kspCaches/$sourceSetName")

        @JvmStatic
        private fun getSubpluginOptions(
            project: Project,
            kspExtension: KspExtension,
            nonEmptyKspConfigurations: List<Configuration>,
            sourceSetName: String,
            isIncremental: Boolean
        ): List<SubpluginOption> {
            val options = mutableListOf<SubpluginOption>()
            options += SubpluginOption("classOutputDir", getKspClassOutputDir(project, sourceSetName).path)
            options += SubpluginOption("javaOutputDir", getKspJavaOutputDir(project, sourceSetName).path)
            options += SubpluginOption("kotlinOutputDir", getKspKotlinOutputDir(project, sourceSetName).path)
            options += SubpluginOption("resourceOutputDir", getKspResourceOutputDir(project, sourceSetName).path)
            options += SubpluginOption("cachesDir", getKspCachesDir(project, sourceSetName).path)
            options += SubpluginOption("kspOutputDir", getKspOutputDir(project, sourceSetName).path)
            options += SubpluginOption("incremental", isIncremental.toString())
            options += SubpluginOption("incrementalLog", project.findProperty("ksp.incremental.log")?.toString() ?: "false")
            options += SubpluginOption("projectBaseDir", project.project.projectDir.canonicalPath)
            options += FilesSubpluginOption("apclasspath", nonEmptyKspConfigurations.flatten())

            kspExtension.apOptions.forEach {
                options += SubpluginOption("apoption", "${it.key}=${it.value}")
            }
            return options
        }
    }

    private val androidIntegration by lazy {
        AndroidPluginIntegration(this)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val KotlinSourceSet.kspConfigurationName: String
        get() {
            return if (name == SourceSet.MAIN_SOURCE_SET_NAME) {
                KSP_MAIN_CONFIGURATION_NAME
            } else {
                "$KSP_MAIN_CONFIGURATION_NAME${name.capitalize(Locale.US)}"
            }
        }

    private fun KotlinSourceSet.kspConfiguration(project: Project): Configuration? {
        return project.configurations.findByName(kspConfigurationName)
    }

    override fun apply(project: Project) {
        project.extensions.create("ksp", KspExtension::class.java)
        // Always include the main `ksp` configuration.
        // TODO: multiplatform
        project.configurations.create(KSP_MAIN_CONFIGURATION_NAME)
        project.plugins.withType(KotlinPluginWrapper::class.java) {
            // kotlin extension has the compilation target that we need to look for to create configurations
            decorateKotlinExtension(project)
        }
        androidIntegration.applyIfAndroidProject(project)
        registry.register(KspModelBuilder())
    }

    private fun decorateKotlinExtension(project:Project) {
        project.extensions.configure(KotlinSingleTargetExtension::class.java) { kotlinExtension ->
            kotlinExtension.target.compilations.createKspConfigurations(project) { kotlinCompilation ->
                kotlinCompilation.kotlinSourceSets.map {
                    it.kspConfigurationName
                }
            }
        }
    }

    /**
     * Creates a KSP configuration for each element in the object container.
     */
    internal fun<T> NamedDomainObjectContainer<T>.createKspConfigurations(
        project: Project,
        getKspConfigurationNames : (T)-> List<String>
    ) {
        val mainConfiguration = project.configurations.maybeCreate(KSP_MAIN_CONFIGURATION_NAME)
        all {
            getKspConfigurationNames(it).forEach { kspConfigurationName ->
                if (kspConfigurationName != KSP_MAIN_CONFIGURATION_NAME) {
                    val existing = project.configurations.findByName(kspConfigurationName)
                    if (existing == null) {
                        project.configurations.create(kspConfigurationName) {
                            it.extendsFrom(mainConfiguration)
                        }
                    }
                }
            }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        val kspVersion = javaClass.`package`.implementationVersion
        val kotlinVersion = project.getKotlinPluginVersion() ?: "N/A"

        // Check version and show warning by default.
        val noVersionCheck = project.findProperty("ksp.version.check")?.toString()?.toBoolean() == false
        if (!noVersionCheck && !kspVersion.startsWith(kotlinVersion))
            project.logger.warn("ksp-$kspVersion might not work with kotlin-$kotlinVersion properly. Please pick the same version of ksp and kotlin plugins.")

        return true
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val kotlinCompileProvider: TaskProvider<AbstractCompile> =
            project.locateTask(kotlinCompilation.compileKotlinTaskName) ?: return project.provider { emptyList() }
        val javaCompile = findJavaTaskForKotlinCompilation(kotlinCompilation)?.get()
        val kspExtension = project.extensions.getByType(KspExtension::class.java)
        val kspConfigurations = LinkedHashSet<Configuration>()
        kotlinCompilation.allKotlinSourceSets.forEach {
            it.kspConfiguration(project)?.let {
                kspConfigurations.add(it)
            }
        }
        // Always include the main `ksp` configuration.
        // TODO: multiplatform
        project.configurations.findByName(KSP_MAIN_CONFIGURATION_NAME)?.let {
            kspConfigurations.add(it)
        }
        val nonEmptyKspConfigurations = kspConfigurations.filter { it.dependencies.isNotEmpty() }
        if (nonEmptyKspConfigurations.isEmpty()) {
            return project.provider { emptyList() }
        }

        val sourceSetName = kotlinCompilation.defaultSourceSetName
        val classOutputDir = getKspClassOutputDir(project, sourceSetName)
        val javaOutputDir = getKspJavaOutputDir(project, sourceSetName)
        val kotlinOutputDir = getKspKotlinOutputDir(project, sourceSetName)
        val resourceOutputDir = getKspResourceOutputDir(project, sourceSetName)
        val kspOutputDir = getKspOutputDir(project, sourceSetName)

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
            kspTask.options.addAll(
                kspTask.project.provider {
                    getSubpluginOptions(
                        project,
                        kspExtension,
                        nonEmptyKspConfigurations,
                        sourceSetName,
                        isIncremental
                    )
                }
            )
            kspTask.destination = kspOutputDir
            kspTask.blockOtherCompilerPlugins = kspExtension.blockOtherCompilerPlugins
            kspTask.pluginConfigurationName = kotlinCompilation.pluginConfigurationName
            kspTask.apOptions.value(kspExtension.arguments).disallowChanges()
            kspTask.kspCacheDir.fileValue(getKspCachesDir(project, sourceSetName)).disallowChanges()

            // depends on the processor; if the processor changes, it needs to be reprocessed.
            val processorClasspath = project.configurations.maybeCreate("${kspTaskName}ProcessorClasspath")
                .extendsFrom(*nonEmptyKspConfigurations.toTypedArray())
            kspTask.processorClasspath.from(processorClasspath)

            nonEmptyKspConfigurations.forEach {
                kspTask.dependsOn(it.buildDependencies)
            }
        }

        fun configureAsAbstractCompile(kspTask: AbstractCompile) {
            kspTask.getDestinationDirectory().set(kspOutputDir)
            kspTask.outputs.dirs(
                kotlinOutputDir,
                javaOutputDir,
                classOutputDir,
                resourceOutputDir
            )
            kotlinCompilation.allKotlinSourceSets.forEach { sourceSet -> kspTask.source(sourceSet.kotlin) }
            kotlinCompilation.output.classesDirs.from(classOutputDir)
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
                        isIncremental
                    )
                }
            }
            is KotlinNativeCompile -> {
                val kspTaskClass = KspTaskNative::class.java
                project.tasks.register(kspTaskName, kspTaskClass, kotlinCompileTask.compilation).apply {
                    configure { kspTask ->
                        configureAsKspTask(kspTask, false)
                        configureAsAbstractCompile(kspTask)

                        // KotlinNativeCompile computes -Xplugin=... from compilerPluginClasspath.
                        val pluginConfigurationName = (kotlinCompileTask.compilation as AbstractKotlinNativeCompilation).pluginConfigurationName
                        val compilerPluginCP = project.configurations.getByName(pluginConfigurationName)
                        val apiDep = project.dependencies.create(apiArtifact)
                        compilerPluginCP.dependencies.add(apiDep)
                        kspTask.compilerPluginClasspath = compilerPluginCP
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

        val processResourcesTaskName = (kotlinCompilation as? KotlinCompilationWithResources)?.processResourcesTaskName ?: "processResources"
        project.locateTask<ProcessResources>(processResourcesTaskName)?.let { provider ->
            provider.configure { resourcesTask ->
                resourcesTask.dependsOn(kspTaskProvider)
                resourcesTask.from(resourceOutputDir)
            }
        }
        if (kotlinCompilation is KotlinJvmAndroidCompilation) {
            androidIntegration.registerGeneratedJavaSources(
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
        SubpluginArtifact(groupId = "com.google.devtools.ksp", artifactId = KSP_ARTIFACT_NAME, version = javaClass.`package`.implementationVersion)

    override fun getPluginArtifactForNative(): SubpluginArtifact? =
        SubpluginArtifact(groupId = "com.google.devtools.ksp", artifactId = KSP_ARTIFACT_NAME_NATIVE, version = javaClass.`package`.implementationVersion)

    val apiArtifact = "com.google.devtools.ksp:symbol-processing-api:${javaClass.`package`.implementationVersion}"
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

    @get:Internal
    var pluginConfigurationName: String

    @get:Input
    var blockOtherCompilerPlugins: Boolean

    @get:Input
    val apOptions: MapProperty<String, String>

    // @PathSensitive and @Classpath doesn't seem working together. Effectively, we are forced to choose between
    // 1. remote cache, or
    // 2. detecting trivial changes in processors.
    // Only processor authors need 2. so let's favor 1. for broader audience.
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val processorClasspath: ConfigurableFileCollection

    /**
     * Output directory that contains caches necessary to support incremental annotation processing.
     */
    @get:LocalState
    val kspCacheDir: DirectoryProperty

    fun configureCompilation(kotlinCompilation: KotlinCompilationData<*>, kotlinCompile: AbstractKotlinCompile<*>, isIncremental: Boolean)
}

@CacheableTask
abstract class KspTaskJvm : KotlinCompile(KotlinJvmOptionsImpl()), KspTask {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    @get:InputFiles
    abstract val classpathStructure: ConfigurableFileCollection

    @get:Internal
    var isIntermoduleIncremental: Boolean = false

    override fun configureCompilation(kotlinCompilation: KotlinCompilationData<*>, kotlinCompile: AbstractKotlinCompile<*>, isIncremental: Boolean) {
        Configurator<KspTaskJvm>(kotlinCompilation).configure(this)
        kotlinCompile as KotlinCompile
        val providerFactory = kotlinCompile.project.providers
        compileKotlinArgumentsContributor.set(
            providerFactory.provider {
                kotlinCompile.compilerArgumentsContributor
            }
        )

        isIntermoduleIncremental = project.findProperty("ksp.incremental.intermodule")?.toString()?.toBoolean() ?: true
        if (isIncremental && isIntermoduleIncremental) {
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
            if (extension.isEmpty() || extension == "kt" || extension == "java" || extension == "jar" || extension == "class") {
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
    internal abstract val compileKotlinArgumentsContributor: Property<CompilerArgumentsContributor<K2JVMCompilerArguments>>

    init {
        // kotlinc's incremental compilation isn't compatible with symbol processing in a few ways:
        // * It doesn't consider private / internal changes when computing dirty sets.
        // * It compiles iteratively; Sources can be compiled in different rounds.
        incremental = false
    }

    override fun setupCompilerArgs(
            args: K2JVMCompilerArguments,
            defaultsOnly: Boolean,
            ignoreClasspathResolutionErrors: Boolean
    ) {
        // Start with / copy from kotlinCompile.
        compileKotlinArgumentsContributor.get().contributeArguments(args, compilerArgumentsConfigurationFlags(
            defaultsOnly,
            ignoreClasspathResolutionErrors
        ))
        if (blockOtherCompilerPlugins) {
            args.blockOtherPlugins(project, pluginConfigurationName)
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
        changedFiles: ChangedFiles
    ) {
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
        args.addChangedFiles(changedFiles)
        super.callCompilerAsync(args, sourceRoots, changedFiles)
    }

    override fun skipCondition(): Boolean = false
}


@CacheableTask
abstract class KspTaskJS @Inject constructor(
    objectFactory: ObjectFactory
) : Kotlin2JsCompile(KotlinJsOptionsImpl(), objectFactory), KspTask {
    override fun configureCompilation(kotlinCompilation: KotlinCompilationData<*>, kotlinCompile: AbstractKotlinCompile<*>, isIncremental: Boolean) {
        Configurator<KspTaskJS>(kotlinCompilation).configure(this)
        kotlinCompile as Kotlin2JsCompile
        val providerFactory = kotlinCompile.project.providers
        compileKotlinArgumentsContributor.set(
            providerFactory.provider {
                kotlinCompile.abstractKotlinCompileArgumentsContributor
            }
        )
    }

    @get:Internal
    internal abstract val compileKotlinArgumentsContributor: Property<CompilerArgumentsContributor<K2JSCompilerArguments>>

    init {
        // kotlinc's incremental compilation isn't compatible with symbol processing in a few ways:
        // * It doesn't consider private / internal changes when computing dirty sets.
        // * It compiles iteratively; Sources can be compiled in different rounds.
        incremental = false
    }

    override fun setupCompilerArgs(
        args: K2JSCompilerArguments,
        defaultsOnly: Boolean,
        ignoreClasspathResolutionErrors: Boolean
    ) {
        // Start with / copy from kotlinCompile.
        args.fillDefaultValues()
        compileKotlinArgumentsContributor.get().contributeArguments(args, compilerArgumentsConfigurationFlags(
            defaultsOnly,
            ignoreClasspathResolutionErrors
        ))
        if (blockOtherCompilerPlugins) {
            args.blockOtherPlugins(project, pluginConfigurationName)
        }
        args.addPluginOptions(options.get())
        args.outputFile = File(destination, "dummyOutput.js").canonicalPath
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `callCompilerAsync$kotlin_gradle_plugin`(
        args: K2JSCompilerArguments,
        sourceRoots: SourceRoots,
        changedFiles: ChangedFiles
    ) {
        if (changedFiles.hasNonSourceChange()) {
            clearIncCache()
        } else {
            args.addChangedFiles(changedFiles)
        }
        super.callCompilerAsync(args, sourceRoots, changedFiles)
    }
}

abstract class KspTaskMetadata : KotlinCompileCommon(KotlinMultiplatformCommonOptionsImpl()), KspTask {
    override fun configureCompilation(kotlinCompilation: KotlinCompilationData<*>, kotlinCompile: AbstractKotlinCompile<*>, isIncremental: Boolean) {
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
    internal abstract val compileKotlinArgumentsContributor: Property<CompilerArgumentsContributor<K2MetadataCompilerArguments>>

    init {
        // kotlinc's incremental compilation isn't compatible with symbol processing in a few ways:
        // * It doesn't consider private / internal changes when computing dirty sets.
        // * It compiles iteratively; Sources can be compiled in different rounds.
        incremental = false
    }

    override fun setupCompilerArgs(
        args: K2MetadataCompilerArguments,
        defaultsOnly: Boolean,
        ignoreClasspathResolutionErrors: Boolean
    ) {
        // Start with / copy from kotlinCompile.
        args.apply { fillDefaultValues() }
        compileKotlinArgumentsContributor.get().contributeArguments(args, compilerArgumentsConfigurationFlags(
            defaultsOnly,
            ignoreClasspathResolutionErrors
        ))
        if (blockOtherCompilerPlugins) {
            args.blockOtherPlugins(project, pluginConfigurationName)
        }
        args.addPluginOptions(options.get())
        args.destination = destination.canonicalPath
    }

    // Overrding an internal function is hacky.
    // TODO: Ask upstream to open it.
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
    fun `callCompilerAsync$kotlin_gradle_plugin`(
        args: K2MetadataCompilerArguments,
        sourceRoots: SourceRoots,
        changedFiles: ChangedFiles
    ) {
        if (changedFiles.hasNonSourceChange()) {
            clearIncCache()
        } else {
            args.addChangedFiles(changedFiles)
        }
        super.callCompilerAsync(args, sourceRoots, changedFiles)
    }
}

@CacheableTask
abstract class KspTaskNative @Inject constructor(
    injected: KotlinNativeCompilationData<*>
) : KotlinNativeCompile(injected), KspTask {
    override fun buildCompilerArgs(): List<String> {
        val kspOptions = options.get().flatMap { listOf("-P", it.toArg()) }
        return super.buildCompilerArgs() + kspOptions
    }

    override fun configureCompilation(
        kotlinCompilation: KotlinCompilationData<*>,
        kotlinCompile: AbstractKotlinCompile<*>,
        isIncremental: Boolean
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

fun SubpluginOption.toArg() = "plugin:${KspGradleSubplugin.KSP_PLUGIN_ID}:${key}=${value}"

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

private fun CommonCompilerArguments.blockOtherPlugins(project: Project, pluginConfigurationName: String) {
    // FIXME: ask upstream to provide an API to make this not implementation-dependent.
    val cfg = project.configurations.getByName(pluginConfigurationName)
    val dep = cfg.dependencies.single { it.name == KspGradleSubplugin.KSP_ARTIFACT_NAME }
    pluginClasspaths = cfg.files(dep).map { it.canonicalPath }.toTypedArray()
    pluginOptions = arrayOf()

}

// TODO: Move into dumpArgs after the compiler supports local function in inline functions.
private inline fun <reified T: CommonCompilerArguments> T.toPair(property: KProperty1<T, *>): Pair<String, String> {
    @Suppress("UNCHECKED_CAST")
    val value = (property as KProperty1<T, *>).get(this)
    return property.name to if (value is Array<*>)
        value.asList().toString()
    else
        value.toString()
}

@Suppress("unused")
internal inline fun <reified T: CommonCompilerArguments> dumpArgs(args: T): Map<String, String> {
    @Suppress("UNCHECKED_CAST")
    val argumentProperties =
        args::class.members.mapNotNull { member ->
            (member as? KProperty1<T, *>)?.takeIf { it.annotations.any { ann -> ann is Argument } }
        }

    return argumentProperties.associate(args::toPair).toSortedMap()
}
