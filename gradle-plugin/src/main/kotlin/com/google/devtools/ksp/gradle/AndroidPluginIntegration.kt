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

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BaseExtension
import com.google.devtools.ksp.gradle.KspGradleSubplugin.Companion.KSP_MAIN_CONFIGURATION_NAME
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.io.File
import java.util.Locale

/**
 * This helper class handles communication with the android plugin.
 * It is isolated in a separate class to avoid adding dependency on the android plugin.
 * Instead, we add a compileOnly dependency to the Android Plugin, which means we can still function
 * without the Android plugin. The downside is that we need to ensure never to access Android
 * plugin APIs directly without checking its existence (we have tests covering that case).
 */
@Suppress("UnstableApiUsage") // some android APIs are unsable.
class AndroidPluginIntegration(
    private val kspGradleSubplugin: KspGradleSubplugin
) {

    private val agpPluginIds = listOf("com.android.application", "com.android.library", "com.android.dynamic-feature")

    fun applyIfAndroidProject(project: Project) {
        agpPluginIds.forEach { agpPluginId ->
            project.pluginManager.withPlugin(agpPluginId) {
                // for android apps, we need a configuration per source set
                decorateAndroidExtension(project)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val AndroidSourceSet.kspConfigurationName: String
        get() {
            return if (name == SourceSet.MAIN_SOURCE_SET_NAME) {
                KSP_MAIN_CONFIGURATION_NAME
            } else {
                "$KSP_MAIN_CONFIGURATION_NAME${name.capitalize(Locale.US)}"
            }
        }

    private fun decorateAndroidExtension(project: Project) {
        val sourceSets = when (val androidExt = project.extensions.getByName("android")) {
            is BaseExtension -> androidExt.sourceSets
            is CommonExtension<*, *, *, *, *, *, *, *> -> androidExt.sourceSets
            else -> throw RuntimeException("Unsupported Android Gradle plugin version.")
        }

        @Suppress("UnstableApiUsage")
        kspGradleSubplugin.run {
            sourceSets.createKspConfigurations(project) { androidSourceSet ->
                listOf(androidSourceSet.kspConfigurationName)
            }
        }
    }

    fun registerGeneratedJavaSources(
        project: Project,
        kotlinCompilation: KotlinJvmAndroidCompilation,
        kspTaskProvider: TaskProvider<KspTaskJvm>,
        javaOutputDir: File,
        classOutputDir: File,
        resourcesOutputDir: FileCollection,
    ) {
        val kspJavaOutput = project.fileTree(javaOutputDir).builtBy(kspTaskProvider)
        val kspClassOutput = project.fileTree(classOutputDir).builtBy(kspTaskProvider)
        kspJavaOutput.include("**/*.java")
        kspClassOutput.include("**/*.class")
        kotlinCompilation.androidVariant.registerExternalAptJavaOutput(kspJavaOutput)
        kotlinCompilation.androidVariant.registerPreJavacGeneratedBytecode(kspClassOutput)
        kotlinCompilation.androidVariant.registerPostJavacGeneratedBytecode(resourcesOutputDir)
    }
}
