/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.compilerRunner.CompilerExecutionSettings
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers
import org.jetbrains.kotlin.compilerRunner.KotlinToolRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.tasks.DefaultKotlinJavaToolchain
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.propertyWithNewInstance
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

internal inline fun <reified T : Any?> ObjectFactory.property() = property(T::class.java)
internal inline fun <reified T : Any?> ObjectFactory.property(initialValue: T) = property<T>().value(initialValue)

abstract class KotlinCompilerRunnerImpl @Inject constructor(
    task: Task,
    objectFactory: ObjectFactory,
    @get:Internal val workerExecutor: WorkerExecutor
) : KotlinCompilerRunner {
    @get:Internal
    internal val taskProvider: Provider<GradleCompileTaskProvider> = objectFactory.property(
        objectFactory.newInstance<GradleCompileTaskProvider>(task.project.gradle, task, task.project)
    )

    @get:Internal
    internal val defaultKotlinJavaToolchain: Provider<DefaultKotlinJavaToolchain> =
        objectFactory.propertyWithNewInstance({ null })

    @get:Internal
    internal val metrics: Property<BuildMetricsReporter> = objectFactory.property(BuildMetricsReporterImpl())

    @get:Internal
    val normalizedKotlinDaemonJvmArguments: Provider<List<String>>
        get() = kotlinDaemonJvmArguments.map {
            it.map { arg -> arg.trim().removePrefix("-") }
        }

    @get:Internal
    internal val logger = task.logger

    @Internal
    internal fun prepareEnvironment(allWarningsAsErrors: Boolean, outputs: List<File>): GradleCompilerEnvironment {
        val messageCollector = GradlePrintingMessageCollector(GradleKotlinLogger(logger), allWarningsAsErrors)
        val outputItemCollector = OutputItemsCollectorImpl()
        return GradleCompilerEnvironment(
            compilerClasspath.files.toList(), messageCollector, outputItemCollector,
            reportingSettings = ReportingSettings(),
            outputFiles = outputs
        )
    }

    @Internal
    internal fun prepareCompilerRunner(): GradleCompilerRunnerWithWorkers {
        return GradleCompilerRunnerWithWorkers(
            taskProvider.get(),
            defaultKotlinJavaToolchain.get().currentJvmJdkToolsJar.orNull,
            CompilerExecutionSettings(
                normalizedKotlinDaemonJvmArguments.get(),
                compilerExecutionStrategy.get(),
                useDaemonFallbackStrategy.get()
            ),
            metrics.get(),
            workerExecutor
        )
    }
}

abstract class KotlinJvmCompilerRunnerImpl @Inject constructor(
    task: Task,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : KotlinCompilerRunnerImpl(task, objectFactory, workerExecutor), KotlinJvmCompilerRunner {

    override fun runJvmCompilerAsync(
        args: K2JVMCompilerArguments,
        sources: List<File>,
        commonSources: List<File>,
        outputs: List<File>
    ) {
        val environment = prepareEnvironment(args.allWarningsAsErrors, outputs)
        val compilerRunner = prepareCompilerRunner()

        compilerRunner.runJvmCompilerAsync(
            sourcesToCompile = sources,
            commonSources = commonSources,
            javaPackagePrefix = null,
            args = args,
            environment = environment,
            jdkHome = defaultKotlinJavaToolchain.get().providedJvm.get().javaHome,
            null
        )
    }
}

abstract class KotlinJsCompilerRunnerImpl @Inject constructor(
    task: Task,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : KotlinCompilerRunnerImpl(task, objectFactory, workerExecutor), KotlinJsCompilerRunner {

    override fun runJsCompilerAsync(
        args: K2JSCompilerArguments,
        sources: List<File>,
        commonSources: List<File>,
        outputs: List<File>
    ) {
        val environment = prepareEnvironment(args.allWarningsAsErrors, outputs)
        val compilerRunner = prepareCompilerRunner()

        compilerRunner.runJsCompilerAsync(sources, commonSources, args, environment, null)
    }
}

abstract class KotlinMetadataCompilerRunnerImpl @Inject constructor(
    task: Task,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : KotlinCompilerRunnerImpl(task, objectFactory, workerExecutor), KotlinMetadataCompilerRunner {

    override fun runMetadataCompilerAsync(
        args: K2MetadataCompilerArguments,
        sources: List<File>,
        commonSources: List<File>,
        outputs: List<File>
    ) {
        val environment = prepareEnvironment(args.allWarningsAsErrors, outputs)
        val compilerRunner = prepareCompilerRunner()

        compilerRunner.runMetadataCompilerAsync(sources, commonSources, args, environment)
    }
}

abstract class KotlinNativeCompilerRunnerImpl @Inject constructor(
    task: Task,
    private val objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor,
    private val execOperations: ExecOperations
) : KotlinCompilerRunnerImpl(task, objectFactory, workerExecutor), KotlinNativeCompilerRunner {

    private val runnerSettings = org.jetbrains.kotlin.compilerRunner
        .KotlinNativeCompilerRunner.Settings.fromProject(task.project)

    override fun runNativeCompilerAsync(
        args: K2NativeCompilerArguments,
        sources: List<File>,
        commonSources: List<File>,
        outputs: List<File>,
    ) {
        val output = File(outputs.first(), "dummy.out")

        val target = KonanTarget.predefinedTargets.get(args.target!!)!!
        val buildArgs: MutableList<String> = mutableListOf(
            "-o", output.path,
            "-target", target.name,
            "-p", "library",
            "-Xmulti-platform"
        )
        args.libraries?.flatMap { listOf("-l", it) }?.let { buildArgs.addAll(it) }
        args.friendModules?.let {
            buildArgs.add("-friend-modules")
            buildArgs.add(it)
        }

        if (args.verbose)
            buildArgs.add("-verbose")
        if (args.allWarningsAsErrors)
            buildArgs.add("-Werror")

        args.pluginClasspaths?.map { "-Xplugin=$it" }?.let { buildArgs.addAll(it) }
        args.pluginOptions?.flatMap { listOf("-P", it) }?.let { buildArgs.addAll(it) }

        args.languageVersion?.let {
            buildArgs.add("-language-version")
            buildArgs.add(it)
        }
        args.apiVersion?.let {
            buildArgs.add("-api-version")
            buildArgs.add(it)
        }

        buildArgs.addAll(sources.map { it.absolutePath })
        buildArgs.addAll(args.freeArgs)
        buildArgs.addAll(commonSources.map { it.absolutePath })

        org.jetbrains.kotlin.compilerRunner.KotlinNativeCompilerRunner(
            settings = runnerSettings,
            executionContext = KotlinToolRunner.GradleExecutionContext.fromTaskContext(
                objectFactory,
                execOperations,
                logger
            )
        ).run(buildArgs)
    }
}
