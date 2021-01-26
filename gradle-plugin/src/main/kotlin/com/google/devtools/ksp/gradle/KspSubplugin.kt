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
import java.io.File
import javax.inject.Inject
import kotlin.reflect.KProperty1
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class KspGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
        KotlinCompilerPluginSupportPlugin {
    companion object {
        const val KSP_CONFIGURATION_NAME = "ksp"
        const val KSP_ARTIFACT_NAME = "symbol-processing"
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
    }

    override fun apply(project: Project) {
        project.extensions.create("ksp", KspExtension::class.java)
        project.configurations.create(KSP_CONFIGURATION_NAME)

        registry.register(KspModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val kotlinCompileProvider: TaskProvider<KotlinCompile> = project.locateTask(kotlinCompilation.compileKotlinTaskName)
                ?: return project.provider { emptyList() }
        val javaCompile = findJavaTaskForKotlinCompilation(kotlinCompilation)?.get()
        val kspExtension = project.extensions.getByType(KspExtension::class.java)

        val kspConfiguration: Configuration = project.configurations.findByName(KSP_CONFIGURATION_NAME)
                ?: return project.provider { emptyList() }

        val options = mutableListOf<SubpluginOption>()

        options += FilesSubpluginOption("apclasspath", kspConfiguration)

        val sourceSetName = kotlinCompilation.compilationName ?: "default"
        val classOutputDir = getKspClassOutputDir(project, sourceSetName)
        val javaOutputDir = getKspJavaOutputDir(project, sourceSetName)
        val kotlinOutputDir = getKspKotlinOutputDir(project, sourceSetName)
        val resourceOutputDir = getKspResourceOutputDir(project, sourceSetName)
        val cachesDir = getKspCachesDir(project, sourceSetName)
        val kspOutputDir = getKspOutputDir(project, sourceSetName)
        options += SubpluginOption("classOutputDir", classOutputDir.path)
        options += SubpluginOption("javaOutputDir", javaOutputDir.path)
        options += SubpluginOption("kotlinOutputDir", kotlinOutputDir.path)
        options += SubpluginOption("resourceOutputDir", resourceOutputDir.path)
        options += SubpluginOption("cachesDir", cachesDir.path)
        options += SubpluginOption("incremental", project.findProperty("ksp.incremental")?.toString() ?: "true")
        options += SubpluginOption("incrementalLog", project.findProperty("ksp.incremental.log")?.toString() ?: "false")
        options += SubpluginOption("projectBaseDir", project.project.projectDir.canonicalPath)
        options += SubpluginOption("kspOutputDir", kspOutputDir.path)

        kspExtension.apOptions.forEach {
            options += SubpluginOption("apoption", "${it.key}=${it.value}")
        }

        if (javaCompile != null) {
            val generatedJavaSources = javaCompile.project.fileTree(javaOutputDir)
            generatedJavaSources.include("**/*.java")
            javaCompile.source(generatedJavaSources)
            javaCompile.classpath += project.files(classOutputDir)
        }

        assert(kotlinCompileProvider.name.startsWith("compile"))
        val kspTaskName = kotlinCompileProvider.name.replaceFirst("compile", "ksp")
        val destinationDir = getKspOutputDir(project, sourceSetName)
        InternalTrampoline.KotlinCompileTaskData_register(kspTaskName, kotlinCompilation, project.provider { destinationDir })

        val kspTaskProvider = project.tasks.register(kspTaskName, KspTask::class.java) { kspTask ->
            kspTask.setDestinationDir(destinationDir)
            kspTask.options = options
            kspTask.kotlinCompile = kotlinCompileProvider.get()
            kspTask.destination = destinationDir
            kspTask.outputs.dirs(kotlinOutputDir, javaOutputDir, classOutputDir, resourceOutputDir)
            kspTask.dependsOn(kspConfiguration.buildDependencies)
            // depends on the processor; if the processor changes, it needs to be reprocessed.
            kspTask.source(kspConfiguration)
        }.apply {
            configure {
                kotlinCompilation.allKotlinSourceSets.forEach { sourceSet -> it.source(sourceSet.kotlin) }
                kotlinCompilation.output.classesDirs.from(classOutputDir)
            }
        }

        kotlinCompileProvider.configure { kotlinCompile ->
            kotlinCompile.dependsOn(kspTaskProvider)
            kotlinCompile.source(kotlinOutputDir, javaOutputDir)
            kotlinCompile.classpath += project.files(classOutputDir)
        }

        val processResourcesTaskName = (kotlinCompilation as? KotlinCompilationWithResources)?.processResourcesTaskName ?: "processResources"
        project.locateTask<ProcessResources>(processResourcesTaskName)?.let { provider ->
            provider.configure { resourcesTask ->
                resourcesTask.dependsOn(kspTaskProvider)
                resourcesTask.from(resourceOutputDir)
            }
        }

        return project.provider { emptyList() }
    }

    override fun getCompilerPluginId() = KSP_PLUGIN_ID
    override fun getPluginArtifact(): SubpluginArtifact =
            SubpluginArtifact(groupId = "com.google.devtools.ksp", artifactId = KSP_ARTIFACT_NAME, version = javaClass.`package`.implementationVersion)
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
          is KotlinWithJavaCompilation -> compilation.compileJavaTaskProvider
          is KotlinJvmCompilation -> compilation.compileJavaTaskProvider // may be null for Kotlin-only JVM target in MPP
            else -> null
        }

open class KspTask : KspTaskJ() {
    lateinit var options: List<SubpluginOption>
    lateinit var kotlinCompile: KotlinCompile
    lateinit var destination: File

    @Input
    open fun getApOptions(): Map<String, String> {
        return project.extensions.getByType(KspExtension::class.java).apOptions
    }

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
        kotlinCompile.setupCompilerArgs(args, defaultsOnly, ignoreClasspathResolutionErrors)
        args.addPluginOptions(options)
        args.destinationAsFile = destination
    }

    override fun getClasspath() = kotlinCompile.classpath
}

fun K2JVMCompilerArguments.addPluginOptions(options: List<SubpluginOption>) {
    fun SubpluginOption.toArg() = "plugin:${KspGradleSubplugin.KSP_PLUGIN_ID}:${key}=${value}"
    pluginOptions = (options.map { it.toArg() } + pluginOptions!!).toTypedArray()
}

fun K2JVMCompilerArguments.addChangedFiles(changedFiles: ChangedFiles) {
    if (changedFiles is ChangedFiles.Known) {
        val options = mutableListOf<SubpluginOption>()
        changedFiles.modified.ifNotEmpty { options += SubpluginOption("knownModified", map { it.path }.joinToString(":")) }
        changedFiles.removed.ifNotEmpty { options += SubpluginOption("knownRemoved", map { it.path }.joinToString(":")) }
        options.ifNotEmpty { addPluginOptions(this) }
    }
}

@Suppress("unused")
internal fun dumpArgs(args: K2JVMCompilerArguments): Map<String, String> {
    @Suppress("UNCHECKED_CAST")
    val argumentProperties =
        args::class.members.mapNotNull { member ->
            (member as? KProperty1<K2JVMCompilerArguments, *>)?.takeIf { it.annotations.any { ann -> ann is Argument } }
        }

    fun toPair(property: KProperty1<K2JVMCompilerArguments, *>): Pair<String, String> {
        @Suppress("UNCHECKED_CAST")
        val value = (property as KProperty1<K2JVMCompilerArguments, *>).get(args)
        return property.name to if (value is Array<*>)
            value.asList().toString()
        else
            value.toString()
    }

    return argumentProperties.associate(::toPair).toSortedMap()
}
