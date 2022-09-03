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
import com.google.devtools.ksp.gradle.tasks.KspTaskFactory
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import javax.inject.Inject

class KspGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    KotlinCompilerPluginSupportPlugin {
    companion object {
        const val KSP_MAIN_CONFIGURATION_NAME = "ksp"
        const val KSP_ARTIFACT_NAME = "symbol-processing"
        const val KSP_ARTIFACT_NAME_NATIVE = "symbol-processing-cmdline"
        const val KSP_PLUGIN_ID = "com.google.devtools.ksp.symbol-processing"
        const val KSP_API_ID = "symbol-processing-api"
        const val KSP_COMPILER_PLUGIN_ID = "symbol-processing"
        const val KSP_GROUP_ID = "com.google.devtools.ksp"
        const val KSP_PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kspPluginClasspath"

        val apiArtifact = "com.google.devtools.ksp:symbol-processing-api:$KSP_VERSION"

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
        fun getSubpluginOptions(
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
            // Turn this on by default to work KT-30172 around. It is off by default in the compiler plugin.
            options += SubpluginOption(
                "returnOkOnError",
                project.findProperty("ksp.return.ok.on.error")?.toString() ?: "true"
            )

            kspExtension.apOptions.forEach {
                options += SubpluginOption("apoption", "${it.key}=${it.value}")
            }
            kspExtension.commandLineArgumentProviders.forEach {
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
        val sourceSetName = kotlinCompilation.defaultSourceSetName
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

        val kspTaskProvider = KspTaskFactory.createKspTask(
            project,
            kotlinCompilation,
            kotlinCompileTask,
            kspExtension,
            nonEmptyKspConfigurations,
            kspTaskName,
            target,
            sourceSetName,
            classOutputDir,
            javaOutputDir,
            kotlinOutputDir,
            resourceOutputDir,
            kspOutputDir
        )

        kotlinCompileProvider.configure { kotlinCompile ->
            kotlinCompile.dependsOn(kspTaskProvider)
            kotlinCompile.setSource(kotlinOutputDir, javaOutputDir)
            when (kotlinCompile) {
                is KotlinCompile -> kotlinCompile.libraries.from(project.files(classOutputDir))
                // TODO: support binary generation on other platforms?
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
                kspTaskProvider = kspTaskProvider,
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

    override fun getPluginArtifactForNative(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "com.google.devtools.ksp",
            artifactId = KSP_ARTIFACT_NAME_NATIVE,
            version = KSP_VERSION
        )
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

    @get:Nested
    val commandLineArgumentProviders: ListProperty<CommandLineArgumentProvider>

    @get:OutputDirectory
    var destination: File

    @get:Optional
    @get:Classpath
    val overridePluginClasspath: ConfigurableFileCollection

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
}
