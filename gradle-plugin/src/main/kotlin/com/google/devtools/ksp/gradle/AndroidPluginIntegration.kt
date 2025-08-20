/*
 * Copyright 2021 Google LLC
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

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.Component
import com.android.build.api.variant.Variant
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.SourceKind
import com.google.devtools.ksp.gradle.utils.getAgpVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.util.concurrent.Callable

/**
 * This helper class handles communication with the android plugin.
 * It is isolated in a separate class to avoid adding dependency on the android plugin.
 * Instead, we add a compileOnly dependency to the Android Plugin, which means we can still function
 * without the Android plugin. The downside is that we need to ensure never to access Android
 * plugin APIs directly without checking its existence (we have tests covering that case).
 */
object AndroidPluginIntegration {

    fun forEachAndroidSourceSet(project: Project, onSourceSet: (String) -> Unit) {
        project.pluginManager.withPlugin("com.android.base") {
            // for android modules, we need a configuration per source set
            decorateAndroidExtension(project, onSourceSet)
        }
    }

    private fun decorateAndroidExtension(project: Project, onSourceSet: (String) -> Unit) {
        val sourceSets = when (val androidExt = project.extensions.getByName("android")) {
            is BaseExtension -> androidExt.sourceSets
            is CommonExtension<*, *, *, *, *, *> -> androidExt.sourceSets
            else -> throw RuntimeException("Unsupported Android Gradle plugin version.")
        }
        sourceSets.configureEach {
            onSourceSet(it.name)
        }
    }

    fun getCompilationSourceSets(kotlinCompilation: KotlinJvmAndroidCompilation): List<String> {
        return kotlinCompilation.androidVariant
            .sourceSets
            .map { it.name }
    }

    /**
     * Support KspTaskJvm and KspAATask tasks
     */
    @Suppress("DEPRECATION")
    private fun tryUpdateKspWithAndroidSourceSets(
        project: Project,
        kotlinCompilation: KotlinJvmAndroidCompilation,
        kspTaskProvider: TaskProvider<*>,
        androidVariant: Component?
    ) {
        val kaptProvider: TaskProvider<Task>? =
            project.locateTask(kotlinCompilation.compileTaskProvider.kaptTaskName)

        val useLegacyApi = project.useLegacyVariantApi()
        if (useLegacyApi) {
            val sources = kotlinCompilation.androidVariant.getSourceFolders(SourceKind.JAVA)
            kspTaskProvider.configure { task ->
                // this is workaround for KAPT generator that prevents circular dependency
                val filteredSources = Callable {
                    val destinationProperty = (kaptProvider?.get() as? KaptTask)?.destinationDir
                    val dir = destinationProperty?.get()?.asFile
                    sources.filter { dir?.isParentOf(it.dir) != true }
                }
                when (task) {
                    is KspTaskJvm -> { task.source(filteredSources) }
                    is KspAATask -> { task.kspConfig.javaSourceRoots.from(filteredSources) }
                    else -> Unit
                }
            }
        } else {
            kspTaskProvider.configure { task ->
                val sources = androidVariant?.sources?.java?.static
                // this is workaround for KAPT generator that prevents circular dependency
                val filteredSources = Callable {
                    val destinationProperty = (kaptProvider?.get() as? KaptTask)?.destinationDir
                    val dir = destinationProperty?.get()?.asFile
                    sources?.map { it.filter { dir?.isParentOf(it.asFile) != true } }
                }
                when (task) {
                    is KspTaskJvm -> { task.source(filteredSources) }
                    is KspAATask -> { task.kspConfig.javaSourceRoots.from(filteredSources) }
                    else -> Unit
                }
            }
        }
    }

    // same logic as in Kapt name generation method
    private val TaskProvider<*>.kaptTaskName: String
        get() {
            val prefix = "kapt"
            return if (name.startsWith("compile")) {
                name.replaceFirst("compile", prefix)
            } else {
                "$prefix${name.capitalizeAsciiOnly()}"
            }
        }

    fun TaskProvider<*>.isKspAATask(): Boolean = map { it is KspAATask }.getOrElse(false)

    private fun registerGeneratedSources(
        project: Project,
        kotlinCompilation: KotlinJvmAndroidCompilation,
        kspTaskProvider: TaskProvider<*>,
        javaOutputDir: Provider<Directory>,
        kotlinOutputDir: Provider<Directory>,
        classOutputDir: Provider<Directory>,
        resourcesOutputDir: Provider<Directory>,
        androidVariant: Component?,
    ) {
        println("calling registerGeneratedSources with compilation " + kotlinCompilation)
        println(" and variant " + androidVariant?.name)
        val kspJavaOutput = project.fileTree(javaOutputDir).builtBy(kspTaskProvider)
        val kspKotlinOutput = project.fileTree(kotlinOutputDir).builtBy(kspTaskProvider)
        val kspClassOutput = project.fileTree(classOutputDir).builtBy(kspTaskProvider)
        kspJavaOutput.include("**/*.java")
        kspKotlinOutput.include("**/*.kt")
        kspClassOutput.include("**/*.class")

        if (project.isAgpBuiltInKotlinUsed().not()) {
            // This API leads to circular dependency with AGP + Built in kotlin
            kotlinCompilation.androidVariant.registerPreJavacGeneratedBytecode(kspClassOutput)
        }

        kotlinCompilation.androidVariant.registerPostJavacGeneratedBytecode(project.files(resourcesOutputDir))

        if (androidVariant != null && kspTaskProvider.isKspAATask() && project.useLegacyVariantApi().not()) {
            androidVariant.sources.java?.addGeneratedSourceDirectory(
                taskProvider = kspTaskProvider,
                wiredWith = { task -> (task as KspAATask).kspConfig.javaOutputDir }
            )
            androidVariant.sources.java?.addGeneratedSourceDirectory(
                taskProvider = kspTaskProvider,
                wiredWith = { task -> (task as KspAATask).kspConfig.kotlinOutputDir }
            )
        } else {
            kotlinCompilation.androidVariant.addJavaSourceFoldersToModel(kspKotlinOutput.dir)
            kotlinCompilation.androidVariant.registerExternalAptJavaOutput(kspJavaOutput)
        }
    }

    fun syncSourceSets(
        project: Project,
        kotlinCompilation: KotlinJvmAndroidCompilation,
        kspTaskProvider: TaskProvider<*>,
        javaOutputDir: Provider<Directory>,
        kotlinOutputDir: Provider<Directory>,
        classOutputDir: Provider<Directory>,
        resourcesOutputDir: Provider<Directory>,
        androidVariant: Component?
    ) {
        // Order is important here as we update task with AGP generated sources and
        // then update AGP with source that KSP will generate.
        // Mixing this up will cause circular dependency in Gradle
        tryUpdateKspWithAndroidSourceSets(project, kotlinCompilation, kspTaskProvider, androidVariant)

        registerGeneratedSources(
            project,
            kotlinCompilation,
            kspTaskProvider,
            javaOutputDir,
            kotlinOutputDir,
            classOutputDir,
            resourcesOutputDir,
            androidVariant
        )
    }

    fun Project.isKotlinBaseApiPluginApplied() = plugins.findPlugin(KotlinBaseApiPlugin::class.java) != null

    fun Project.isKotlinAndroidPluginApplied() = pluginManager.hasPlugin("org.jetbrains.kotlin.android")

    fun Project.isAgpBuiltInKotlinUsed() = isKotlinBaseApiPluginApplied() && isKotlinAndroidPluginApplied().not()

    /**
     * Returns false for AGP versions 8.10.0-alpha03 or higher.
     *
     * Returns true for older AGP versions or when AGP version cannot be determined.
     */
    fun Project.useLegacyVariantApi(): Boolean {
        val agpVersion = project.getAgpVersion()

        // Fall back to using the legacy Variant API if the AGP version can't be determined for now.
        if (agpVersion == null) {
            return true
        }
        return agpVersion < AndroidPluginVersion(8, 10, 0).alpha(3)
    }
}
