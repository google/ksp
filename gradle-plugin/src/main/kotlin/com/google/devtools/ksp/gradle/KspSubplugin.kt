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

package com.google.devtools.ksp.gradle

import com.google.devtools.ksp.gradle.model.builder.KspModelBuilder
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.CLASS_STRUCTURE_ARTIFACT_TYPE
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.ClasspathSnapshot
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptClasspathChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformAction
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformLegacyAction
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.InternalSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.BaseKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
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
            options +=
                InternalSubpluginOption("classOutputDir", getKspClassOutputDir(project, sourceSetName, target).path)
            options +=
                InternalSubpluginOption("javaOutputDir", getKspJavaOutputDir(project, sourceSetName, target).path)
            options +=
                InternalSubpluginOption("kotlinOutputDir", getKspKotlinOutputDir(project, sourceSetName, target).path)
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
                it.asArguments().forEach { argument ->
                    if (!argument.matches(Regex("\\S+=\\S+"))) {
                        throw IllegalArgumentException("KSP apoption does not match \\S+=\\S+: $argument")
                    }
                    options += SubpluginOption("apoption", argument)
                }
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

        val processorClasspath = project.configurations.maybeCreate("${kspTaskName}ProcessorClasspath")
            .extendsFrom(*nonEmptyKspConfigurations.toTypedArray())
        fun configureAsKspTask(kspTask: KspTask, isIncremental: Boolean) {
            // depends on the processor; if the processor changes, it needs to be reprocessed.
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
            kspTask.inputs.property("apOptions", kspExtension.arguments)
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

        val isIncremental = project.findProperty("ksp.incremental")?.toString()?.toBoolean() ?: true

        // Create and configure KSP tasks.
        val kspTaskProvider = when (kotlinCompileTask) {
            is KotlinCompile -> {
                KotlinFactories.registerKotlinJvmCompileTask(project, kspTaskName, kotlinCompilation).also {
                    it.configure { kspTask ->
                        maybeBlockOtherPlugins(kspTask as BaseKotlinCompile)
                        configureAsKspTask(kspTask, isIncremental)
                        configureAsAbstractKotlinCompileTool(kspTask as AbstractKotlinCompileTool<*>)
                        configurePluginOptions(kspTask)
                        kspTask.libraries.setFrom(
                            kotlinCompileTask.project.files(Callable { kotlinCompileTask.libraries })
                        )
                        kspTask.compilerOptions.noJdk.value(kotlinCompileTask.compilerOptions.noJdk)
                        kspTask.compilerOptions.useK2.value(false)
                        kspTask.compilerOptions.moduleName.convention(kotlinCompileTask.moduleName.map { "$it-ksp" })
                        kspTask.moduleName.value(kotlinCompileTask.moduleName.get())
                        kspTask.destination.value(kspOutputDir)

                        val isIntermoduleIncremental =
                            (project.findProperty("ksp.incremental.intermodule")?.toString()?.toBoolean() ?: true) &&
                                isIncremental
                        val classStructureFiles = getClassStructureFiles(project, kspTask.libraries)
                        kspTask.incrementalChangesTransformers.add(
                            createIncrementalChangesTransformer(
                                isIncremental,
                                isIntermoduleIncremental,
                                getKspCachesDir(project, sourceSetName, target),
                                project.provider { classStructureFiles },
                                project.provider { kspTask.libraries },
                                project.provider { processorClasspath }
                            )
                        )
                    }
                    // Don't support binary generation for non-JVM platforms yet.
                    // FIXME: figure out how to add user generated libraries.
                    kotlinCompilation.output.classesDirs.from(classOutputDir)
                }
            }
            is Kotlin2JsCompile -> {
                KotlinFactories.registerKotlinJSCompileTask(project, kspTaskName, kotlinCompilation).also {
                    it.configure { kspTask ->
                        maybeBlockOtherPlugins(kspTask as BaseKotlinCompile)
                        configureAsKspTask(kspTask, isIncremental)
                        configureAsAbstractKotlinCompileTool(kspTask as AbstractKotlinCompileTool<*>)
                        configurePluginOptions(kspTask)
                        kspTask.libraries.setFrom(
                            kotlinCompileTask.project.files(Callable { kotlinCompileTask.libraries })
                        )
                        kspTask.compilerOptions.freeCompilerArgs
                            .value(kotlinCompileTask.compilerOptions.freeCompilerArgs)
                        kspTask.compilerOptions.useK2.value(false)
                        kspTask.compilerOptions.moduleName.convention(kotlinCompileTask.moduleName.map { "$it-ksp" })

                        kspTask.incrementalChangesTransformers.add(
                            createIncrementalChangesTransformer(
                                isIncremental,
                                false,
                                getKspCachesDir(project, sourceSetName, target),
                                project.provider { project.files() },
                                project.provider { project.files() },
                                project.provider { project.files() },
                            )
                        )
                    }
                }
            }
            is KotlinCompileCommon -> {
                KotlinFactories.registerKotlinMetadataCompileTask(project, kspTaskName, kotlinCompilation).also {
                    it.configure { kspTask ->
                        maybeBlockOtherPlugins(kspTask as BaseKotlinCompile)
                        configureAsKspTask(kspTask, isIncremental)
                        configureAsAbstractKotlinCompileTool(kspTask as AbstractKotlinCompileTool<*>)
                        configurePluginOptions(kspTask)
                        kspTask.libraries.setFrom(
                            kotlinCompileTask.project.files(Callable { kotlinCompileTask.libraries })
                        )
                        kspTask.compilerOptions.useK2.value(false)

                        kspTask.incrementalChangesTransformers.add(
                            createIncrementalChangesTransformer(
                                isIncremental,
                                false,
                                getKspCachesDir(project, sourceSetName, target),
                                project.provider { project.files() },
                                project.provider { project.files() },
                                project.provider { project.files() },
                            )
                        )
                    }
                }
            }
            is KotlinNativeCompile -> {
                KotlinFactories.registerKotlinNativeCompileTask(project, kspTaskName, kotlinCompileTask).also {
                    it.configure { kspTask ->
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
                        // Cannot use lambda; See below for details.
                        // https://docs.gradle.org/7.2/userguide/validation_problems.html#implementation_unknown
                        kspTask.doFirst(object : Action<Task> {
                            override fun execute(t: Task) {
                                kspOutputDir.deleteRecursively()
                            }
                        })
                    }
                }
            }
            else -> return project.provider { emptyList() }
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
}

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

internal val artifactType = Attribute.of("artifactType", String::class.java)

internal fun maybeRegisterTransform(project: Project) {
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

internal fun getClassStructureFiles(
    project: Project,
    libraries: ConfigurableFileCollection,
): FileCollection {
    maybeRegisterTransform(project)

    val classStructureIfIncremental = project.configurations.detachedConfiguration(
        project.dependencies.create(project.files(project.provider { libraries }))
    )

    return classStructureIfIncremental.incoming.artifactView { viewConfig ->
        viewConfig.attributes.attribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
    }.files
}

// Reuse Kapt's infrastructure to compute affected names in classpath.
// This is adapted from KaptTask.findClasspathChanges.
internal fun findClasspathChanges(
    changes: ChangedFiles,
    cacheDir: File,
    allDataFiles: Set<File>,
    libs: List<File>,
    processorCP: List<File>,
): KaptClasspathChanges {
    cacheDir.mkdirs()

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
            libs,
            processorCP,
            allDataFiles
        )

    val classpathChanges = currentSnapshot.diff(previousSnapshot, changedFiles)
    if (classpathChanges is KaptClasspathChanges.Unknown || changes is ChangedFiles.Unknown) {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }
    currentSnapshot.writeToCache()

    return classpathChanges
}

internal fun ChangedFiles.hasNonSourceChange(): Boolean {
    if (this !is ChangedFiles.Known)
        return true

    return !(this.modified + this.removed).all {
        it.isKotlinFile(listOf("kt")) || it.isJavaFile()
    }
}

fun KaptClasspathChanges.toSubpluginOptions(): List<SubpluginOption> {
    return if (this is KaptClasspathChanges.Known) {
        this.names.map { it.replace('/', '.').replace('$', '.') }.ifNotEmpty {
            listOf(SubpluginOption("changedClasses", joinToString(":")))
        } ?: emptyList()
    } else {
        emptyList()
    }
}

fun ChangedFiles.toSubpluginOptions(): List<SubpluginOption> {
    return if (this is ChangedFiles.Known) {
        val options = mutableListOf<SubpluginOption>()
        this.modified.filter { it.isKotlinFile(listOf("kt")) || it.isJavaFile() }.ifNotEmpty {
            options += SubpluginOption("knownModified", map { it.path }.joinToString(File.pathSeparator))
        }
        this.removed.filter { it.isKotlinFile(listOf("kt")) || it.isJavaFile() }.ifNotEmpty {
            options += SubpluginOption("knownRemoved", map { it.path }.joinToString(File.pathSeparator))
        }
        options
    } else {
        emptyList()
    }
}

// Return a closure that captures required arguments only.
internal fun createIncrementalChangesTransformer(
    isKspIncremental: Boolean,
    isIntermoduleIncremental: Boolean,
    cacheDir: File,
    classpathStructure: Provider<FileCollection>,
    libraries: Provider<FileCollection>,
    processorCP: Provider<FileCollection>,
): (ChangedFiles) -> List<SubpluginOption> = { changedFiles ->
    val options = mutableListOf<SubpluginOption>()
    if (isKspIncremental) {
        if (isIntermoduleIncremental) {
            // findClasspathChanges may clear caches, if there are
            // 1. unknown changes, or
            // 2. changes in annotation processors.
            val classpathChanges = findClasspathChanges(
                changedFiles,
                cacheDir,
                classpathStructure.get().files,
                libraries.get().files.toList(),
                processorCP.get().files.toList()
            )
            options += classpathChanges.toSubpluginOptions()
        } else {
            if (changedFiles.hasNonSourceChange()) {
                cacheDir.deleteRecursively()
            }
        }
    } else {
        cacheDir.deleteRecursively()
    }
    options += changedFiles.toSubpluginOptions()

    options
}
