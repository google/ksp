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

import com.android.build.api.variant.Component
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.gradle.AndroidPluginIntegration.useLegacyVariantApi
import com.google.devtools.ksp.gradle.model.builder.KspModelBuilder
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.CLASS_STRUCTURE_ARTIFACT_TYPE
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.ClasspathSnapshot
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptClasspathChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformAction
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformLegacyAction
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@OptIn(KspExperimental::class)
class KspGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    KotlinCompilerPluginSupportPlugin {
    companion object {
        const val KSP_PLUGIN_ID = "com.google.devtools.ksp.symbol-processing"
        const val KSP_API_ID = "symbol-processing-api"
        const val KSP_COMPILER_PLUGIN_ID = "symbol-processing"
        const val KSP_COMPILER_PLUGIN_ID_NON_EMBEDDABLE = "symbol-processing-cmdline"
        const val KSP_GROUP_ID = "com.google.devtools.ksp"
        const val KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kspPluginClasspath"
        const val KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME_NON_EMBEDDABLE = "kspPluginClasspathNonEmbeddable"

        @JvmStatic
        fun getKspOutputDir(project: Project, sourceSetName: String, target: String): Provider<Directory> =
            project.layout.buildDirectory.dir("generated/ksp/$target/$sourceSetName")

        @JvmStatic
        fun getKspClassOutputDir(project: Project, sourceSetName: String, target: String): Provider<Directory> =
            getKspOutputDir(project, sourceSetName, target).map { it.dir("classes") }

        @JvmStatic
        fun getKspJavaOutputDir(project: Project, sourceSetName: String, target: String): Provider<Directory> =
            getKspOutputDir(project, sourceSetName, target).map { it.dir("java") }

        @JvmStatic
        fun getKspKotlinOutputDir(project: Project, sourceSetName: String, target: String): Provider<Directory> =
            getKspOutputDir(project, sourceSetName, target).map { it.dir("kotlin") }

        @JvmStatic
        fun getKspResourceOutputDir(project: Project, sourceSetName: String, target: String): Provider<Directory> =
            getKspOutputDir(project, sourceSetName, target).map { it.dir("resources") }

        @JvmStatic
        fun getKspCachesDir(project: Project, sourceSetName: String, target: String): Provider<Directory> =
            project.layout.buildDirectory.dir("kspCaches/$target/$sourceSetName")
    }

    private lateinit var kspConfigurations: KspConfigurations

    private val androidComponentCache = ConcurrentHashMap<String, Component>()

    override fun apply(target: Project) {
        val ksp = target.extensions.create("ksp", KspExtension::class.java)
        ksp.useKsp2.convention(
            target.providers
                .gradleProperty("ksp.useKSP2")
                .map { it.toBoolean() }
                .orElse(true)
        )
        kspConfigurations = KspConfigurations(target)
        registry.register(KspModelBuilder())

        target.plugins.withId("com.android.base") {
            val androidComponents =
                target.extensions.findByType(com.android.build.api.variant.AndroidComponentsExtension::class.java)!!

            val selector = androidComponents.selector().all()
            androidComponents.onVariants(selector) { variant ->
                for (component in variant.components) {
                    androidComponentCache.computeIfAbsent(component.name) {
                        component
                    }
                }
            }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        val useKsp2 = project.extensions.getByType(KspExtension::class.java).useKsp2.get()

        if (useKsp2.not()) {
            project.logger.error(
                "KSP1 is no longer available. Please use KSP2 instead and do not explicitly set ksp.useKsp2 to false " +
                    "via the DSL or the Gradle property please"
            )
        }
        return true
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val component = androidComponentCache.get(kotlinCompilation.name)
        val kotlinCompileProvider: TaskProvider<AbstractKotlinCompileTool<*>> =
            project.locateTask(kotlinCompilation.compileKotlinTaskName) ?: return project.provider { emptyList() }
        val kspExtension = project.extensions.getByType(KspExtension::class.java)
        if (kotlinCompileProvider.name == "compileKotlinMetadata") {
            return project.provider { emptyList() }
        }
        if ((kotlinCompilation as? KotlinSharedNativeCompilation)?.platformType == KotlinPlatformType.common) {
            return project.provider { emptyList() }
        }
        assert(kotlinCompileProvider.name.startsWith("compile"))
        val kspTaskName = kotlinCompileProvider.name.replaceFirst("compile", "ksp")
        val processorClasspath =
            project.configurations.maybeCreate("${kspTaskName}ProcessorClasspath").markResolvable()
        if (kotlinCompilation.platformType != KotlinPlatformType.androidJvm ||
            project.useLegacyVariantApi() ||
            project.pluginManager.hasPlugin("kotlin-multiplatform")
        ) {
            val nonEmptyKspConfigurations =
                kspConfigurations.find(kotlinCompilation)
                    .filter { it.allDependencies.isNotEmpty() }
            processorClasspath.extendsFrom(*nonEmptyKspConfigurations.toTypedArray())
        }

        val target = kotlinCompilation.target.name
        val sourceSetName = kotlinCompilation.defaultSourceSet.name
        val classOutputDir = getKspClassOutputDir(project, sourceSetName, target)
        val javaOutputDir = getKspJavaOutputDir(project, sourceSetName, target)
        val kotlinOutputDir = getKspKotlinOutputDir(project, sourceSetName, target)
        val resourceOutputDir = getKspResourceOutputDir(project, sourceSetName, target)

        val kspClasspathCfg = project.configurations.maybeCreate(
            KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME
        ).markResolvable()
        project.dependencies.add(
            kspClasspathCfg.name,
            "$KSP_GROUP_ID:$KSP_API_ID:$KSP_VERSION"
        )
        project.dependencies.add(
            kspClasspathCfg.name,
            "$KSP_GROUP_ID:$KSP_COMPILER_PLUGIN_ID:$KSP_VERSION"
        )

        val kspClasspathCfgNonEmbeddable = project.configurations.maybeCreate(
            KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME_NON_EMBEDDABLE
        ).markResolvable()
        project.dependencies.add(
            kspClasspathCfgNonEmbeddable.name,
            "$KSP_GROUP_ID:$KSP_API_ID:$KSP_VERSION"
        )
        project.dependencies.add(
            kspClasspathCfgNonEmbeddable.name,
            "$KSP_GROUP_ID:$KSP_COMPILER_PLUGIN_ID_NON_EMBEDDABLE:$KSP_VERSION"
        )

        // Create and configure KSP tasks.
        val kspTaskProvider = KspAATask.registerKspAATask(
            kotlinCompilation,
            kotlinCompileProvider,
            processorClasspath,
            kspExtension
        )

        val generatedSources = arrayOf(
            project.files(kotlinOutputDir).builtBy(kspTaskProvider),
            project.files(javaOutputDir).builtBy(kspTaskProvider),
        )
        if (kotlinCompilation is KotlinCommonCompilation) {
            // Do not add generated sources to common source sets.
            // They will be observed by downstreams and violate current build scheme.
            kotlinCompileProvider.configure { it.source(*generatedSources) }
        } else {
            kotlinCompilation.defaultSourceSet.kotlin.srcDirs(*generatedSources)
        }

        kotlinCompileProvider.configure { kotlinCompile ->
            when (kotlinCompile) {
                is AbstractKotlinCompile<*> -> kotlinCompile.libraries.from(project.files(classOutputDir))
                // is KotlinNativeCompile -> TODO: support binary generation?
            }
        }

        findJavaTaskForKotlinCompilation(kotlinCompilation)?.configure { javaCompile ->
            val generatedJavaSources = javaCompile.project.fileTree(javaOutputDir).builtBy(kspTaskProvider)
            generatedJavaSources.include("**/*.java")
            javaCompile.source(generatedJavaSources)
            javaCompile.classpath += project.files(classOutputDir)
        }

        val processResourcesTaskName =
            (kotlinCompilation as? KotlinCompilationWithResources)?.processResourcesTaskName ?: "processResources"
        project.locateTask<ProcessResources>(processResourcesTaskName)?.let { provider ->
            provider.configure { resourcesTask ->
                resourcesTask.from(project.files(resourceOutputDir).builtBy(kspTaskProvider))
            }
        }
        if (kotlinCompilation is KotlinJvmAndroidCompilation) {
            AndroidPluginIntegration.syncSourceSets(
                project = project,
                kotlinCompilation = kotlinCompilation,
                kspTaskProvider = kspTaskProvider,
                javaOutputDir = javaOutputDir,
                kotlinOutputDir = kotlinOutputDir,
                classOutputDir = classOutputDir,
                resourcesOutputDir = resourceOutputDir,
                androidComponent = component,
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

    override fun getPluginArtifactForNative(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "com.google.devtools.ksp",
            artifactId = KSP_COMPILER_PLUGIN_ID_NON_EMBEDDABLE,
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
    ).markResolvable()

    return classStructureIfIncremental.incoming.artifactView { viewConfig ->
        viewConfig.attributes.attribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
    }.files
}

// Reuse Kapt's infrastructure to compute affected names in classpath.
// This is adapted from KaptTask.findClasspathChanges.
internal fun findClasspathChanges(
    changes: SourcesChanges,
    cacheDir: File,
    allDataFiles: Set<File>,
    libs: List<File>,
    processorCP: List<File>,
): KaptClasspathChanges {
    cacheDir.mkdirs()

    val changedFiles =
        (changes as? SourcesChanges.Known)?.let { it.modifiedFiles + it.removedFiles }?.toSet() ?: allDataFiles

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
    if (classpathChanges is KaptClasspathChanges.Unknown || changes is SourcesChanges.Unknown) {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }
    currentSnapshot.writeToCache()

    return classpathChanges
}

internal fun getCPChanges(
    inputChanges: InputChanges,
    incrementalProps: List<FileCollection>,
    cacheDir: File,
    classpathStructure: FileCollection,
    libraries: FileCollection,
    processorCP: FileCollection,
): List<String> {
    val apClasspath = processorCP.files.toList()
    val changedFiles = if (!inputChanges.isIncremental) {
        SourcesChanges.Unknown
    } else {
        incrementalProps.fold(mutableListOf<File>() to mutableListOf<File>()) { (modified, removed), prop ->
            inputChanges.getFileChanges(prop).forEach {
                when (it.changeType) {
                    ChangeType.ADDED, ChangeType.MODIFIED -> modified.add(it.file)
                    ChangeType.REMOVED -> removed.add(it.file)
                    else -> Unit
                }
            }
            modified to removed
        }.run {
            SourcesChanges.Known(first, second)
        }
    }
    val classpathChanges = findClasspathChanges(
        changedFiles,
        cacheDir,
        classpathStructure.files,
        libraries.files.toList(),
        apClasspath
    )
    return if (classpathChanges is KaptClasspathChanges.Known) {
        classpathChanges.names.map {
            it.replace('/', '.').replace('$', '.')
        }
    } else {
        emptyList()
    }
}

internal fun Configuration.markResolvable(): Configuration = apply {
    isCanBeResolved = true
    isCanBeConsumed = false
    isVisible = false
}

/**
 * A [SubpluginOption] that returns the joined path for files in the given [fileCollection].
 */
internal class FileCollectionSubpluginOption(
    key: String,
    val fileCollection: FileCollection
) : SubpluginOption(
    key = key,
    lazyValue = lazy {
        val files = fileCollection.files
        files.joinToString(File.pathSeparator) { it.normalize().absolutePath }
    }
) {
    companion object {
        fun create(
            name: String,
            classpath: Configuration
        ): FileCollectionSubpluginOption {
            return FileCollectionSubpluginOption(
                key = name,
                fileCollection = classpath.incoming.artifactView { }.files
            )
        }
    }
}

internal fun FileCollection.nonSelfDeps(selfTaskName: String): List<Task> =
    buildDependencies.getDependencies(null).filterNot {
        it.name == selfTaskName
    }

internal fun getKaptGeneratedClassesDir(project: Project, sourceSetName: String) =
    Kapt3GradleSubplugin.getKaptGeneratedClassesDir(project, sourceSetName)
