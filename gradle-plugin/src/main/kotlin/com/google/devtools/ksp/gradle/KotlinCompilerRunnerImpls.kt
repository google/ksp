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
import org.jetbrains.kotlin.cli.common.arguments.isIrBackendEnabled
import org.jetbrains.kotlin.cli.common.arguments.isPreIrBackendDisabled
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
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import org.jetbrains.kotlin.utils.JsLibraryUtils
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.CompilerJsOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerJsOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptionsDefault
import org.jetbrains.kotlin.gradle.logging.GradleErrorMessageCollector

internal inline fun <reified T : Any?> ObjectFactory.property() = property(T::class.java)
internal inline fun <reified T : Any?> ObjectFactory.property(initialValue: T) = property<T>().value(initialValue)

// TODO: All the properties should be configured by KGP
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
        val errorMessageCollector = GradleErrorMessageCollector(messageCollector)
        val outputItemCollector = OutputItemsCollectorImpl()
        return GradleCompilerEnvironment(
            compilerClasspath.files.toList(), errorMessageCollector, outputItemCollector,
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
    private val task: Task,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : KotlinCompilerRunnerImpl(task, objectFactory, workerExecutor), KotlinJvmCompilerRunner {

    override fun runJvmCompilerAsync(
        options: CompilerJvmOptions,
        freeArgs: List<String>,
        sources: List<File>,
        commonSources: List<File>,
        friendPaths: List<File>,
        libraries: List<File>,
        destination: File
    ) {
        val environment = prepareEnvironment(options.allWarningsAsErrors.get(), task.outputs.files.toList())
        val compilerRunner = prepareCompilerRunner()
        val compilerArgs = K2JVMCompilerArguments().apply {
            options as CompilerJvmOptionsDefault
            options.fillCompilerArguments(this)

            this@apply.friendPaths = friendPaths.map { it.absolutePath }.toTypedArray()
            this@apply.classpath = libraries.map { it.absolutePath }.joinToString(File.pathSeparator)
            this@apply.freeArgs = options.freeCompilerArgs.get() + freeArgs
            this@apply.destination = destination.absolutePath
        }

        compilerRunner.runJvmCompilerAsync(
            sourcesToCompile = sources,
            commonSources = commonSources,
            javaPackagePrefix = null,
            args = compilerArgs,
            environment = environment,
            jdkHome = defaultKotlinJavaToolchain.get().providedJvm.get().javaHome,
            null
        )
    }
}

abstract class KotlinJsCompilerRunnerImpl @Inject constructor(
    private val task: Task,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : KotlinCompilerRunnerImpl(task, objectFactory, workerExecutor), KotlinJsCompilerRunner {

    private fun libFilter(args: K2JSCompilerArguments, file: File): Boolean =
        file.exists() && when {
            // JS_IR
            args.isIrBackendEnabled() && args.isPreIrBackendDisabled() ->
                isKotlinLibrary(file)

            // JS_LEGACY
            !args.isIrBackendEnabled() && !args.isPreIrBackendDisabled() ->
                JsLibraryUtils.isKotlinJavascriptLibrary(file)

            // JS_BOTH
            args.isIrBackendEnabled() && !args.isPreIrBackendDisabled() ->
                isKotlinLibrary(file) && JsLibraryUtils.isKotlinJavascriptLibrary(file)

            else -> throw IllegalArgumentException("Cannot determine JS backend.")
        }

    override fun runJsCompilerAsync(
        options: CompilerJsOptions,
        freeArgs: List<String>,
        sources: List<File>,
        commonSources: List<File>,
        friendPaths: List<File>,
        libraries: List<File>,
        destination: File
    ) {
        val environment = prepareEnvironment(options.allWarningsAsErrors.get(), task.outputs.files.toList())
        val compilerRunner = prepareCompilerRunner()
        val compilerArgs = K2JSCompilerArguments().apply {
            options as CompilerJsOptionsDefault
            options.fillCompilerArguments(this)

            this@apply.freeArgs = options.freeCompilerArgs.get() + freeArgs
            this@apply.outputFile = File(destination, "dummy.js").absolutePath

            irOnly = this@apply.freeArgs.contains("-Xir-only")
            irProduceJs = this@apply.freeArgs.contains("-Xir-produce-js")
            irProduceKlibDir = this@apply.freeArgs.contains("-Xir-produce-klib-dir")
            irProduceKlibFile = this@apply.freeArgs.contains("-Xir-produce-klib-file")
            irBuildCache = this@apply.freeArgs.contains("-Xir-build-cache")
            wasm = this@apply.freeArgs.contains("-Xwasm")

            this@apply.friendModules = friendPaths.filter { libFilter(this, it) }
                .map { it.absolutePath }.joinToString(File.pathSeparator)
            this@apply.libraries = libraries.filter { libFilter(this, it) }
                .map { it.absolutePath }.joinToString(File.pathSeparator)
        }

        compilerRunner.runJsCompilerAsync(sources, commonSources, compilerArgs, environment, null)
    }
}

abstract class KotlinMetadataCompilerRunnerImpl @Inject constructor(
    private val task: Task,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : KotlinCompilerRunnerImpl(task, objectFactory, workerExecutor), KotlinMetadataCompilerRunner {

    override fun runMetadataCompilerAsync(
        options: CompilerCommonOptions,
        freeArgs: List<String>,
        sources: List<File>,
        commonSources: List<File>,
        friendPaths: List<File>,
        libraries: List<File>,
        destination: File
    ) {
        val environment = prepareEnvironment(options.allWarningsAsErrors.get(), task.outputs.files.toList())
        val compilerRunner = prepareCompilerRunner()
        val compilerArgs = K2MetadataCompilerArguments().apply {
            options as CompilerCommonOptionsDefault
            options.fillCompilerArguments(this)

            this@apply.friendPaths = friendPaths.map { it.absolutePath }.toTypedArray()
            this@apply.classpath = libraries.map { it.absolutePath }.joinToString(File.pathSeparator)
            this@apply.freeArgs = options.freeCompilerArgs.get() + freeArgs
            this@apply.destination = destination.absolutePath
        }

        compilerRunner.runMetadataCompilerAsync(sources, commonSources, compilerArgs, environment)
    }
}

abstract class KotlinNativeCompilerRunnerImpl @Inject constructor(
    private val task: Task,
    private val objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor,
    private val execOperations: ExecOperations
) : KotlinCompilerRunnerImpl(task, objectFactory, workerExecutor), KotlinNativeCompilerRunner {

    private val runnerSettings = org.jetbrains.kotlin.compilerRunner
        .KotlinNativeCompilerRunner.Settings.fromProject(task.project)

    override fun runNativeCompilerAsync(
        options: CompilerCommonOptions,
        freeArgs: List<String>,
        sources: List<File>,
        commonSources: List<File>,
        friendPaths: List<File>,
        libraries: List<File>,
        destination: File,
        target: String
    ) {
        val target = KonanTarget.predefinedTargets.get(target)!!
        val buildArgs: MutableList<String> = mutableListOf(
            "-o", destination.path,
            "-target", target.name,
            "-p", "library",
            "-Xmulti-platform"
        )
        libraries.flatMap { listOf("-l", it.absolutePath) }.let { buildArgs.addAll(it) }
        friendPaths.ifNotEmpty {
            buildArgs.add("-friend-modules")
            buildArgs.add(joinToString(File.pathSeparator))
        }

        if (options.verbose.get())
            buildArgs.add("-verbose")
        if (options.allWarningsAsErrors.get())
            buildArgs.add("-Werror")

        options.languageVersion.getOrNull()?.let {
            buildArgs.add("-language-version")
            buildArgs.add(it.version)
        }
        options.apiVersion.getOrNull()?.let {
            buildArgs.add("-api-version")
            buildArgs.add(it.version)
        }

        buildArgs.addAll(sources.map { it.absolutePath })
        buildArgs.addAll(freeArgs)
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
