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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import com.google.devtools.ksp.gradle.model.builder.KspModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import com.google.devtools.ksp.gradle.KspGradleSubplugin.Companion.KSP_CONFIGURATION_NAME
import com.google.devtools.ksp.gradle.KspGradleSubplugin.Companion.getKspClassOutputDir
import com.google.devtools.ksp.gradle.KspGradleSubplugin.Companion.getKspJavaOutputDir
import com.google.devtools.ksp.gradle.KspGradleSubplugin.Companion.getKspKotlinOutputDir
import com.google.devtools.ksp.gradle.KspGradleSubplugin.Companion.getKspResourceOutputDir
import java.io.File
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class KspGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(KspGradleSubplugin::class.java) != null

        val KSP_CONFIGURATION_NAME = "ksp"

        @JvmStatic
        fun getKspClassOutputDir(project: Project, sourceSetName: String) =
            File(project.project.buildDir, "generated/ksp/classes/$sourceSetName")

        @JvmStatic
        fun getKspJavaOutputDir(project: Project, sourceSetName: String) =
            File(project.project.buildDir, "generated/ksp/src/$sourceSetName/java")

        @JvmStatic
        fun getKspKotlinOutputDir(project: Project, sourceSetName: String) =
            File(project.project.buildDir, "generated/ksp/src/$sourceSetName/kotlin")

        @JvmStatic
        fun getKspResourceOutputDir(project: Project, sourceSetName: String) =
            File(project.project.buildDir, "generated/ksp/src/$sourceSetName/resources")
    }

    override fun apply(project: Project) {
        project.extensions.create("ksp", KspExtension::class.java)
        project.configurations.create(KSP_CONFIGURATION_NAME)

        registry.register(KspModelBuilder())
    }
}

class KspKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val KSP_ARTIFACT_NAME = "symbol-processing"
        const val KSP_PLUGIN_ID = "com.google.devtools.ksp.symbol-processing"

    }

    override fun isApplicable(project: Project, task: AbstractCompile) = KspGradleSubplugin.isEnabled(project)

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {
        if (!KspGradleSubplugin.isEnabled(project)) return emptyList()

        val kspExtension = project.extensions.findByType(KspExtension::class.java) ?: return emptyList()

        val kspConfiguration: Configuration = project.configurations.findByName(KSP_CONFIGURATION_NAME) ?: return emptyList()
        val kspClasspath: FileCollection = project.files(kspConfiguration)

        kotlinCompile.dependsOn(kspConfiguration.buildDependencies)

        val options = mutableListOf<SubpluginOption>()

        options += FilesSubpluginOption("apclasspath", kspConfiguration)

        val sourceSetName = kotlinCompilation?.compilationName ?: "default"
        val classOutputDir = getKspClassOutputDir(project, sourceSetName)
        val javaOutputDir = getKspJavaOutputDir(project, sourceSetName)
        val kotlinOutputDir = getKspKotlinOutputDir(project, sourceSetName)
        val resourceOutputDir = getKspResourceOutputDir(project, sourceSetName)
        options += SubpluginOption("classOutputDir", classOutputDir.path)
        options += SubpluginOption("javaOutputDir", javaOutputDir.path)
        options += SubpluginOption("kotlinOutputDir", kotlinOutputDir.path)
        options += SubpluginOption("resourceOutputDir", resourceOutputDir.path)

        kspExtension.apOptions.forEach {
            options += SubpluginOption("apoption", "${it.key}=${it.value}")
        }

        if (javaCompile != null) {
            val generatedJavaSources = javaCompile.project.fileTree(javaOutputDir)
            generatedJavaSources.include("**/*.java")
            javaCompile.source(generatedJavaSources)
            javaCompile.classpath += project.files(classOutputDir)
        }

        val kspTaskName = kotlinCompile.name.replaceFirst("compile", "ksp")
        InternalTrampoline.KotlinCompileTaskData_register(kspTaskName, kotlinCompilation)

        val kspTaskProvider = project.tasks.register(kspTaskName, KspTask::class.java) { kspTask ->
            kspTask.setDestinationDir(File(project.buildDir, "generated/ksp"))
            kspTask.mapClasspath { kotlinCompile.classpath }
            kspTask.options = options
            kspTask.outputs.dirs(kotlinOutputDir, javaOutputDir, classOutputDir)
        }.apply {
            configure {
                kotlinCompilation?.allKotlinSourceSets?.forEach { sourceSet -> it.source(sourceSet.kotlin) }
                kotlinCompilation?.output?.classesDirs?.from(classOutputDir)
            }
        }

        kotlinCompile.dependsOn(kspTaskProvider)
        kotlinCompile.source(kotlinOutputDir, javaOutputDir)
        kotlinCompile.classpath += project.files(classOutputDir)

        return emptyList()
    }

    override fun getCompilerPluginId() = KSP_PLUGIN_ID
    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(groupId = "com.google.devtools.ksp", artifactId = KSP_ARTIFACT_NAME, version = javaClass.`package`.implementationVersion)
}

open class KspTask : KotlinCompile() {
    lateinit var options: List<SubpluginOption>

    init {
        // kotlinc's incremental compilation isn't compatible with symbol processing in a few ways:
        // * It doesn't consider private / internal changes when computing dirty sets.
        // * It compiles iteratively; Sources can be compiled in different rounds.
        incremental = false
    }

    override fun setupCompilerArgs(
        args: org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments,
        defaultsOnly: Boolean,
        ignoreClasspathResolutionErrors: Boolean
    ) {
        fun SubpluginOption.toArg() = "plugin:${KspKotlinGradleSubplugin.KSP_PLUGIN_ID}:${key}=${value}"
        super.setupCompilerArgs(args, defaultsOnly, ignoreClasspathResolutionErrors)
        args.pluginOptions = (options.map { it.toArg() } + args.pluginOptions!!).toTypedArray()
    }
}