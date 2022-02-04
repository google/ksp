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

package com.google.devtools.ksp

import com.google.devtools.ksp.processing.impl.MessageCollectorBasedKSPLogger
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeListener
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.toKotlinVersion
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import java.io.File

private val KSP_OPTIONS = CompilerConfigurationKey.create<KspOptions.Builder>("Ksp options")

class KotlinSymbolProcessingCommandLineProcessor : CommandLineProcessor {
    override val pluginId = "com.google.devtools.ksp.symbol-processing"

    override val pluginOptions: Collection<AbstractCliOption> = KspCliOption.values().asList()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        if (option !is KspCliOption) {
            throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }

        val kspOptions = configuration[KSP_OPTIONS]
            ?: KspOptions.Builder().also { configuration.put(KSP_OPTIONS, it) }

        kspOptions.processOption(option, value)
    }

    private fun KspOptions.Builder.processOption(option: KspCliOption, value: String) = when (option) {
        KspCliOption.PROCESSOR_CLASSPATH_OPTION -> processingClasspath += value.split(File.pathSeparator).map {
            File(it)
        }
        KspCliOption.CLASS_OUTPUT_DIR_OPTION -> classOutputDir = File(value)
        KspCliOption.JAVA_OUTPUT_DIR_OPTION -> javaOutputDir = File(value)
        KspCliOption.KOTLIN_OUTPUT_DIR_OPTION -> kotlinOutputDir = File(value)
        KspCliOption.RESOURCE_OUTPUT_DIR_OPTION -> resourceOutputDir = File(value)
        KspCliOption.CACHES_DIR_OPTION -> cachesDir = File(value)
        KspCliOption.KSP_OUTPUT_DIR_OPTION -> kspOutputDir = File(value)
        KspCliOption.PROJECT_BASE_DIR_OPTION -> projectBaseDir = File(value)
        KspCliOption.PROCESSING_OPTIONS_OPTION -> {
            val (k, v) = value.split('=', ignoreCase = false, limit = 2)
            processingOptions.put(k, v)
        }
        KspCliOption.KNOWN_MODIFIED_OPTION -> knownModified.addAll(value.split(File.pathSeparator).map { File(it) })
        KspCliOption.KNOWN_REMOVED_OPTION -> knownRemoved.addAll(value.split(File.pathSeparator).map { File(it) })
        KspCliOption.INCREMENTAL_OPTION -> incremental = value.toBoolean()
        KspCliOption.INCREMENTAL_LOG_OPTION -> incrementalLog = value.toBoolean()
        KspCliOption.ALL_WARNINGS_AS_ERRORS_OPTION -> allWarningsAsErrors = value.toBoolean()
        KspCliOption.WITH_COMPILATION_OPTION -> withCompilation = value.toBoolean()
        KspCliOption.CHANGED_CLASSES_OPTION -> changedClasses.addAll(value.split(':'))
    }
}

// Changes here may break some third party libraries like Kotlin Compile Testing, where the compiler is invoked in
// another way. Do our best to notify them when changing this.
//
// Third party libraries:
//   https://github.com/tschuchortdev/kotlin-compile-testing
class KotlinSymbolProcessingComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
        val options = configuration[KSP_OPTIONS]?.apply {
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
            languageVersion = configuration.languageVersionSettings.languageVersion.toKotlinVersion()
            apiVersion = configuration.languageVersionSettings.apiVersion.toKotlinVersion()
            compilerVersion = KotlinCompilerVersion.getVersion().toKotlinVersion()
        }?.build() ?: return
        val messageCollector = configuration.get(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY)
            ?: throw IllegalStateException("ksp: message collector not found!")
        val wrappedMessageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: throw IllegalStateException("ksp: message collector not found!")
        val logger = MessageCollectorBasedKSPLogger(
            messageCollector, wrappedMessageCollector, options.allWarningsAsErrors
        )
        if (options.processingClasspath.isNotEmpty()) {
            if (options.withCompilation && options.incremental) {
                throw IllegalStateException("ksp: `incremental` is incompatible with `withCompilation`.")
            }
            val kotlinSymbolProcessingHandlerExtension = KotlinSymbolProcessingExtension(options, logger)
            AnalysisHandlerExtension.registerExtension(project, kotlinSymbolProcessingHandlerExtension)
            configuration.put(CommonConfigurationKeys.LOOKUP_TRACKER, DualLookupTracker())

            // Dummy extension point; Required by dropPsiCaches().
            CoreApplicationEnvironment.registerExtensionPoint(
                project.extensionArea, PsiTreeChangeListener.EP.name, PsiTreeChangeAdapter::class.java
            )
        }
    }
}

enum class KspCliOption(
    override val optionName: String,
    override val valueDescription: String,
    override val description: String,
    override val required: Boolean = false,
    override val allowMultipleOccurrences: Boolean = false
) : AbstractCliOption {
    CLASS_OUTPUT_DIR_OPTION(
        "classOutputDir",
        "<classOutputDir>",
        "Dir of generated classes",
        false
    ),

    JAVA_OUTPUT_DIR_OPTION(
        "javaOutputDir",
        "<javaOutputDir>",
        "Dir of generated Java sources",
        false
    ),

    KOTLIN_OUTPUT_DIR_OPTION(
        "kotlinOutputDir",
        "<kotlinOutputDir>",
        "Dir of generated Kotlin sources",
        false
    ),

    RESOURCE_OUTPUT_DIR_OPTION(
        "resourceOutputDir",
        "<resourceOutputDir>",
        "Dir of generated resources",
        false
    ),

    CACHES_DIR_OPTION(
        "cachesDir",
        "<cachesDir>",
        "Dir of caches",
        false
    ),

    PROJECT_BASE_DIR_OPTION(
        "projectBaseDir",
        "<projectBaseDir>",
        "path to gradle project",
        false
    ),

    KSP_OUTPUT_DIR_OPTION(
        "kspOutputDir",
        "<kspOutputDir>",
        "root of ksp output dirs",
        false
    ),

    PROCESSING_OPTIONS_OPTION(
        "apoption",
        "<apOption>",
        "processor defined option",
        false,
        true
    ),

    PROCESSOR_CLASSPATH_OPTION(
        "apclasspath",
        "<classpath>",
        "processor classpath",
        false
    ),

    KNOWN_MODIFIED_OPTION(
        "knownModified",
        "<knownModified>",
        "known modified files",
        false,
        false
    ),

    KNOWN_REMOVED_OPTION(
        "knownRemoved",
        "<knownRemoved>",
        "known removed fiels",
        false,
        false
    ),

    INCREMENTAL_OPTION(
        "incremental",
        "<incremental>",
        "processing incrementally",
        false,
        false
    ),

    INCREMENTAL_LOG_OPTION(
        "incrementalLog",
        "<incrementalLog>",
        "log dirty files",
        false,
        false
    ),

    ALL_WARNINGS_AS_ERRORS_OPTION(
        "allWarningsAsErrors",
        "<allWarningsAsErrors>",
        "treat all warnings as errors",
        false,
        false
    ),

    WITH_COMPILATION_OPTION(
        "withCompilation",
        "<withCompilation>",
        "Run processors and compilation in a single compiler invocation",
        false,
        false
    ),

    CHANGED_CLASSES_OPTION(
        "changedClasses",
        "<changedClasses>",
        "canonical / dot-separated names of dirty classes in classpath",
        false,
        false
    );
}

class KspOptions(
    val projectBaseDir: File,
    val compileClasspath: List<File>,
    val javaSourceRoots: List<File>,

    val classOutputDir: File,
    val javaOutputDir: File,
    val kotlinOutputDir: File,
    val resourceOutputDir: File,

    val processingClasspath: List<File>,
    val processors: List<String>,

    val processingOptions: Map<String, String>,

    val knownModified: List<File>,
    val knownRemoved: List<File>,

    val cachesDir: File,
    val kspOutputDir: File,
    val incremental: Boolean,
    val incrementalLog: Boolean,
    val allWarningsAsErrors: Boolean,
    val withCompilation: Boolean,
    val changedClasses: List<String>,

    val languageVersion: KotlinVersion,
    val apiVersion: KotlinVersion,
    val compilerVersion: KotlinVersion,
) {
    class Builder {
        var projectBaseDir: File? = null
        val compileClasspath: MutableList<File> = mutableListOf()
        val javaSourceRoots: MutableList<File> = mutableListOf()

        var classOutputDir: File? = null
        var javaOutputDir: File? = null
        var kotlinOutputDir: File? = null
        var resourceOutputDir: File? = null

        val processingClasspath: MutableList<File> = mutableListOf()
        val processors: MutableList<String> = mutableListOf()

        val processingOptions: MutableMap<String, String> = mutableMapOf()

        val knownModified: MutableList<File> = mutableListOf()
        val knownRemoved: MutableList<File> = mutableListOf()

        var cachesDir: File? = null
        var kspOutputDir: File? = null
        var incremental: Boolean = false
        var incrementalLog: Boolean = false
        var allWarningsAsErrors: Boolean = false
        var withCompilation: Boolean = false
        var changedClasses: MutableList<String> = mutableListOf()

        var languageVersion: KotlinVersion = LanguageVersion.LATEST_STABLE.toKotlinVersion()
        var apiVersion: KotlinVersion = ApiVersion.LATEST_STABLE.toKotlinVersion()
        var compilerVersion: KotlinVersion = KotlinVersion.CURRENT

        fun build(): KspOptions {
            return KspOptions(
                projectBaseDir!!, compileClasspath, javaSourceRoots,
                classOutputDir!!,
                javaOutputDir!!,
                kotlinOutputDir!!,
                resourceOutputDir!!,
                processingClasspath, processors, processingOptions,
                knownModified, knownRemoved, cachesDir!!, kspOutputDir!!, incremental, incrementalLog,
                allWarningsAsErrors, withCompilation, changedClasses,
                languageVersion, apiVersion, compilerVersion
            )
        }
    }
}

private fun String?.toKotlinVersion(): KotlinVersion {
    if (this == null)
        return KotlinVersion.CURRENT

    return split('-').first().split('.').map { it.toInt() }.let {
        when (it.size) {
            1 -> KotlinVersion(it[0], 0, 0)
            2 -> KotlinVersion(it[0], it[1], 0)
            3 -> KotlinVersion(it[0], it[1], it[2])
            else -> KotlinVersion.CURRENT
        }
    }
}

internal fun ApiVersion.toKotlinVersion(): KotlinVersion = version.canonical.toKotlinVersion()
