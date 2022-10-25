package com.google.devtools.ksp.gradle.tasks

import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.tasks.inherited.InheritedTasks
import com.google.devtools.ksp.gradle.tasks.standalone.StandaloneTasks
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import java.io.File

object KspTaskFactory {
    private fun getKspTaskCreator(project: Project): KspTaskCreator {
        return if (project.findProperty("ksp.compiler.runner")?.toString() == "standalone") {
            StandaloneTasks
        } else {
            InheritedTasks
        }
    }

    fun createKspTask(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        kotlinCompileTask: AbstractKotlinCompileTool<*>,
        kspExtension: KspExtension,
        kspConfigurations: List<Configuration>,
        kspTaskName: String,
        target: String,
        sourceSetName: String,
        classOutputDir: File,
        javaOutputDir: File,
        kotlinOutputDir: File,
        resourceOutputDir: File,
        kspOutputDir: File,
    ): TaskProvider<out Task> = getKspTaskCreator(project).createKspTask(
        project,
        kotlinCompilation,
        kotlinCompileTask,
        kspExtension,
        kspConfigurations,
        kspTaskName,
        target,
        sourceSetName,
        classOutputDir,
        javaOutputDir,
        kotlinOutputDir,
        resourceOutputDir,
        kspOutputDir
    )
}

interface KspTaskCreator {
    fun createKspTask(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        kotlinCompileTask: AbstractKotlinCompileTool<*>,
        kspExtension: KspExtension,
        kspConfigurations: List<Configuration>,
        kspTaskName: String,
        target: String,
        sourceSetName: String,
        classOutputDir: File,
        javaOutputDir: File,
        kotlinOutputDir: File,
        resourceOutputDir: File,
        kspOutputDir: File,
    ): TaskProvider<out Task>
}
