package com.google.devtools.ksp.gradle

import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.KSPJvmConfig
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader
import javax.inject.Inject

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
        }
    }

    companion object {
        @Internal
        internal fun registerKspAATaskJvm(
            kotlinCompilation: KotlinCompilation<*>,
            kotlinCompileProvider: TaskProvider<AbstractKotlinCompileTool<*>>,
            processorClasspath: Configuration,
        ): TaskProvider<KspAATask> {
            val project = kotlinCompilation.target.project
            val target = kotlinCompilation.target.name
            val sourceSetName = kotlinCompilation.defaultSourceSet.name
            val kspTaskName = kotlinCompileProvider.name.replaceFirst("compile", "ksp")
            val kspAADepCfg = project.configurations.detachedConfiguration(
                project.dependencies.create("${KspGradleSubplugin.KSP_GROUP_ID}:symbol-processing-aa:$KSP_VERSION")
            ).apply {
                isTransitive = false
            }
            val kspTaskProvider = project.tasks.register(kspTaskName, KspAATask::class.java) { kspAATask ->
                kspAATask.kspClasspath.from(kspAADepCfg)
                kspAATask.kspConfig.let { cfg ->
                    cfg.processorClasspath.from(processorClasspath)
                    cfg.moduleName.value(kotlinCompilation.defaultSourceSet.name)
                    val kotlinOutputDir = KspGradleSubplugin.getKspKotlinOutputDir(project, sourceSetName, target)
                    val javaOutputDir = KspGradleSubplugin.getKspJavaOutputDir(project, sourceSetName, target)
                    kotlinCompilation.allKotlinSourceSetsObservable.forAll { sourceSet ->
                        val filtered = sourceSet.kotlin.srcDirs.filter {
                            !kotlinOutputDir.isParentOf(it) && !javaOutputDir.isParentOf(it)
                        }.map {
                            // @SkipWhenEmpty doesn't work well with File.
                            project.objects.fileTree().from(it)
                        }
                        cfg.sourceRoots.from(filtered)
                        cfg.javaSourceRoots.from(filtered)
                        kspAATask.dependsOn(sourceSet.kotlin.nonSelfDeps(kspTaskName))
                    }
                    if (kotlinCompilation is KotlinCommonCompilation) {
                        cfg.commonSourceRoots.from(kotlinCompilation.defaultSourceSet.kotlin)
                    }
                    cfg.libraries.from(kotlinCompilation.compileDependencyFiles)
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
                    cfg.kotlinOutputDir.value(kotlinOutputDir)
                    cfg.javaOutputDir.value(javaOutputDir)
                    cfg.classOutputDir.value(KspGradleSubplugin.getKspClassOutputDir(project, sourceSetName, target))
                    cfg.resourceOutputDir.value(
                        KspGradleSubplugin.getKspResourceOutputDir(
                            project,
                            sourceSetName,
                            target
                        )
                    )
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
    abstract val sourceRoots: ConfigurableFileCollection

    @get:InputFiles
    abstract val commonSourceRoots: ConfigurableFileCollection

    @get:InputFiles
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
}

interface KspAAWorkParameter : WorkParameters {
    var config: KspGradleConfig
}

abstract class KspAAWorkerAction : WorkAction<KspAAWorkParameter> {
    override fun execute() {
        val gradleCfg = parameters.config
        val processorClassloader = URLClassLoader(
            gradleCfg.processorClasspath.files.map { it.toURI().toURL() }.toTypedArray(),
            SymbolProcessorProvider::class.java.classLoader
        )

        val processorProviders = ServiceLoader.load(
            SymbolProcessorProvider::class.java,
            processorClassloader
        ).toList()
        val kspConfig = KSPJvmConfig.Builder().apply {
            this.processorProviders = processorProviders
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

            // TODO:
            logger = CommandLineKSPLogger()
        }.build()
        KotlinSymbolProcessing(kspConfig).execute()
    }
}
