package com.google.devtools.ksp.gradle

import com.google.devtools.ksp.impl.KSPJvmConfig
import com.google.devtools.ksp.impl.KSPLoader
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.impl.KspGradleLogger
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.ServiceLoader
import java.util.concurrent.Callable
import javax.inject.Inject

@CacheableTask
abstract class KspAATask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {
    @get:Classpath
    abstract val kspClasspath: ConfigurableFileCollection

    @get:Nested
    abstract val kspConfig: KspGradleConfig

    @TaskAction
    fun execute() {
        // FIXME: Create a class loader with clean classpath instead of shadowing existing ones. It'll require either:
        //  1. passing arguments by data structures in stdlib, or
        //  2. hoisting and publishing KspGradleConfig into another package.

        val workerQueue = workerExecutor.classLoaderIsolation {
            it.classpath.setFrom(kspClasspath)
        }
        workerQueue.submit(KspAAWorkerAction::class.java) {
            it.config = kspConfig
            it.kspClassPath = kspClasspath
        }
    }

    companion object {
        @Internal
        internal fun registerKspAATaskJvm(
            kotlinCompilation: KotlinCompilation<*>,
            kotlinCompileProvider: TaskProvider<AbstractKotlinCompileTool<*>>,
            processorClasspath: Configuration,
            kspGeneratedSourceSet: KotlinSourceSet,
            kspExtension: KspExtension,
        ): TaskProvider<KspAATask> {
            val project = kotlinCompilation.target.project
            val target = kotlinCompilation.target.name
            val sourceSetName = kotlinCompilation.defaultSourceSet.name
            val kspTaskName = kotlinCompileProvider.name.replaceFirst("compile", "ksp")
            val kspAADepCfg = project.configurations.detachedConfiguration(
                project.dependencies.create("${KspGradleSubplugin.KSP_GROUP_ID}:symbol-processing-aa:$KSP_VERSION"),
                project.dependencies.create("org.jetbrains.intellij.deps:trove4j:1.0.20200330"),
            ).apply {
                isTransitive = false
            }
            val kspTaskProvider = project.tasks.register(kspTaskName, KspAATask::class.java) { kspAATask ->
                kspAATask.kspClasspath.from(kspAADepCfg)
                kspAATask.kspConfig.let { cfg ->
                    cfg.processorClasspath.from(processorClasspath)
                    cfg.moduleName.value(kotlinCompilation.defaultSourceSet.name)
                    kotlinCompilation.allKotlinSourceSetsObservable
                        .forAll { sourceSet ->
                            if (sourceSet == kspGeneratedSourceSet) return@forAll
                            cfg.sourceRoots.from(sourceSet.kotlin)
                            cfg.javaSourceRoots.from(sourceSet.kotlin)
                        }
                    if (kotlinCompilation is KotlinCommonCompilation) {
                        cfg.commonSourceRoots.from(kotlinCompilation.defaultSourceSet.kotlin)
                    }
                    // FIXME: figure out how to filter or set variant attributes correctly.
                    // cfg.libraries.from(kotlinCompilation.compileDependencyFiles)
                    val kspOutputDir = KspGradleSubplugin.getKspOutputDir(project, sourceSetName, target)
                    cfg.libraries.from(
                        project.files(
                            Callable {
                                kotlinCompileProvider.get().libraries.filter {
                                    !kspOutputDir.isParentOf(it) &&
                                        !(it.isDirectory && it.listFiles()?.isEmpty() == true)
                                }
                            }
                        )
                    )
                    val options = kotlinCompilation.compilerOptions.options
                    if (options is KotlinJvmCompilerOptions) {
                        // TODO: set proper jdk home
                        cfg.jdkHome.value(File(System.getProperty("java.home")))
                    }

                    val compilerOptions = kotlinCompilation.compilerOptions.options
                    val langVer = compilerOptions.languageVersion.orNull?.version ?: KSP_KOTLIN_BASE_VERSION
                    val apiVer = compilerOptions.apiVersion.orNull?.version ?: KSP_KOTLIN_BASE_VERSION
                    cfg.languageVersion.value(langVer.split('.', '-').take(2).joinToString("."))
                    cfg.apiVersion.value(apiVer.split('.', '-').take(2).joinToString("."))

                    cfg.projectBaseDir.value(File(project.project.projectDir.canonicalPath))
                    cfg.cachesDir.value(KspGradleSubplugin.getKspCachesDir(project, sourceSetName, target))
                    cfg.outputBaseDir.value(KspGradleSubplugin.getKspOutputDir(project, sourceSetName, target))
                    cfg.kotlinOutputDir.value(KspGradleSubplugin.getKspKotlinOutputDir(project, sourceSetName, target))
                    cfg.javaOutputDir.value(KspGradleSubplugin.getKspJavaOutputDir(project, sourceSetName, target))
                    cfg.classOutputDir.value(KspGradleSubplugin.getKspClassOutputDir(project, sourceSetName, target))
                    cfg.resourceOutputDir.value(
                        KspGradleSubplugin.getKspResourceOutputDir(
                            project,
                            sourceSetName,
                            target
                        )
                    )
                    val apOptions = mutableMapOf<String, String>()
                    apOptions.putAll(kspExtension.apOptions)
                    kspExtension.commandLineArgumentProviders.forEach { provider ->
                        provider.asArguments().forEach { argument ->
                            val kv = Regex("(\\S+)=(\\S+)").matchEntire(argument)?.groupValues
                            if (kv == null || kv.size != 3) {
                                throw IllegalArgumentException("KSP apoption does not match (\\S+)=(\\S+): $argument")
                            }
                            apOptions.put(kv[1], kv[2])
                        }
                    }
                    cfg.processorOptions.value(apOptions)
                    val logLevel = LogLevel.values().first {
                        project.logger.isEnabled(it)
                    }
                    cfg.logLevel.value(logLevel)
                    cfg.allWarningsAsErrors.value(kspExtension.allWarningsAsErrors)
                    cfg.excludedProcessors.value(kspExtension.excludedProcessors)

                    val jvmDefaultMode = project.provider {
                        compilerOptions.freeCompilerArgs.get().lastOrNull {
                            it.startsWith("-Xjvm-default=")
                        }?.substringAfter("=") ?: "disable"
                    }
                    cfg.jvmDefaultMode.value(jvmDefaultMode)

                    val jvmTarget = project.provider {
                        (compilerOptions as KotlinJvmCompilerOptions).jvmTarget.get().target
                    }
                    cfg.jvmTarget.value(jvmTarget)
                }
            }

            return kspTaskProvider
        }
    }
}

abstract class KspGradleConfig @Inject constructor() {
    @get:Classpath
    abstract val processorClasspath: ConfigurableFileCollection

    @get:Input
    abstract val moduleName: Property<String>

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoots: ConfigurableFileCollection

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val commonSourceRoots: ConfigurableFileCollection

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val javaSourceRoots: ConfigurableFileCollection

    @get:Classpath
    abstract val libraries: ConfigurableFileCollection

    @get:Input
    abstract val jdkHome: Property<File>

    @get:Internal
    abstract val projectBaseDir: Property<File>

    @get:Internal
    abstract val outputBaseDir: Property<File>

    @get:Internal
    abstract val cachesDir: Property<File>

    @get:OutputDirectory
    abstract val kotlinOutputDir: Property<File>

    @get:OutputDirectory
    abstract val javaOutputDir: Property<File>

    @get:OutputDirectory
    abstract val classOutputDir: Property<File>

    @get:OutputDirectory
    abstract val resourceOutputDir: Property<File>

    @get:Input
    abstract val languageVersion: Property<String>

    @get:Input
    abstract val apiVersion: Property<String>

    @get:Input
    abstract val processorOptions: MapProperty<String, String>

    // Unfortunately, passing project.logger over is not possible.
    @get:Input
    abstract val logLevel: Property<LogLevel>

    @get:Input
    abstract val allWarningsAsErrors: Property<Boolean>

    @get:Input
    abstract val excludedProcessors: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val jvmTarget: Property<String>

    @get:Input
    @get:Optional
    abstract val jvmDefaultMode: Property<String>
}

interface KspAAWorkParameter : WorkParameters {
    var config: KspGradleConfig
    var kspClassPath: ConfigurableFileCollection
}

abstract class KspAAWorkerAction : WorkAction<KspAAWorkParameter> {
    override fun execute() {
        val gradleCfg = parameters.config
        val kspClassPath = parameters.kspClassPath
        val isolatedClassLoader = URLClassLoader(
            kspClassPath.files.map { it.toURI().toURL() }.toTypedArray(),
            ClassLoader.getPlatformClassLoader()
        )

        // Clean stale files for now.
        // TODO: support incremental processing.
        gradleCfg.outputBaseDir.get().deleteRecursively()
        gradleCfg.cachesDir.get().deleteRecursively()

        val processorClassloader = URLClassLoader(
            gradleCfg.processorClasspath.files.map { it.toURI().toURL() }.toTypedArray(),
            isolatedClassLoader
        )

        val excludedProcessors = gradleCfg.excludedProcessors.get()
        val processorProviders = ServiceLoader.load(
            processorClassloader.loadClass("com.google.devtools.ksp.processing.SymbolProcessorProvider"),
            processorClassloader
        ).filter {
            it.javaClass.name !in excludedProcessors
        }.toList()

        val kspGradleLogger = KspGradleLogger(gradleCfg.logLevel.get().ordinal)

        if (processorProviders.isEmpty()) {
            kspGradleLogger.error("No providers found in processor classpath.")
            throw Exception("KSP failed with exit code: ${KotlinSymbolProcessing.ExitCode.PROCESSING_ERROR}")
        } else {
            kspGradleLogger.info(
                "loaded provider(s): " +
                    processorProviders.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.javaClass.name }
            )
        }

        val kspConfig = KSPJvmConfig.Builder().apply {
            moduleName = gradleCfg.moduleName.get()
            sourceRoots = gradleCfg.sourceRoots.files.toList()
            javaSourceRoots = gradleCfg.javaSourceRoots.files.toList()
            commonSourceRoots = gradleCfg.commonSourceRoots.files.toList()
            libraries = gradleCfg.libraries.files.toList()
            this.jdkHome = gradleCfg.jdkHome.get()
            projectBaseDir = gradleCfg.projectBaseDir.get()
            outputBaseDir = gradleCfg.outputBaseDir.get()
            cachesDir = gradleCfg.cachesDir.get()
            kotlinOutputDir = gradleCfg.kotlinOutputDir.get()
            javaOutputDir = gradleCfg.javaOutputDir.get()
            classOutputDir = gradleCfg.classOutputDir.get()
            resourceOutputDir = gradleCfg.resourceOutputDir.get()

            languageVersion = gradleCfg.languageVersion.get()
            apiVersion = gradleCfg.apiVersion.get()

            processorOptions = gradleCfg.processorOptions.get()
            allWarningsAsErrors = gradleCfg.allWarningsAsErrors.get()

            jvmTarget = gradleCfg.jvmTarget.get()
            jvmDefaultMode = gradleCfg.jvmDefaultMode.get()
        }.build()
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(kspConfig)

        val exitCode = try {
            val kspLoaderClass = isolatedClassLoader.loadClass(KSPLoader::class.java.canonicalName)
            val runMethod = kspLoaderClass.getMethod(
                "loadAndRunKSP",
                ByteArray::class.java,
                List::class.java,
                Int::class.java
            )
            val returnCode = runMethod.invoke(
                null,
                byteArrayOutputStream.toByteArray(),
                processorProviders,
                gradleCfg.logLevel.get().ordinal
            ) as Int
            KotlinSymbolProcessing.ExitCode.values()[returnCode]
        } catch (e: Exception) {
            require(e is InvocationTargetException)
            kspGradleLogger.exception(e.targetException)
            throw e.targetException
        }

        if (exitCode != KotlinSymbolProcessing.ExitCode.OK) {
            throw Exception("KSP failed with exit code: $exitCode")
        }
    }
}
