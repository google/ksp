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

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import com.google.devtools.ksp.processing.impl.MessageCollectorBasedKSPLogger
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File
import org.jetbrains.kotlin.config.JVMConfigurationKeys

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
        KspCliOption.PROCESSOR_CLASSPATH_OPTION -> processingClasspath += value.split(File.pathSeparator).map{ File(it) }
        KspCliOption.CLASS_OUTPUT_DIR_OPTION -> classOutputDir = File(value)
        KspCliOption.JAVA_OUTPUT_DIR_OPTION -> javaOutputDir = File(value)
        KspCliOption.KOTLIN_OUTPUT_DIR_OPTION -> kotlinOutputDir = File(value)
        KspCliOption.RESOURCE_OUTPUT_DIR_OPTION -> resourceOutputDir = File(value)
        KspCliOption.PROCESSING_OPTIONS_OPTION -> {
            val (k, v) = value.split('=', ignoreCase = false, limit = 2)
            processingOptions.put(k, v)
        }
    }
}

class KotlinSymbolProcessingComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
        val options = configuration[KSP_OPTIONS]?.apply {
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
        }?.build() ?: return
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: throw IllegalStateException("message collector not found!")
        val logger = MessageCollectorBasedKSPLogger(messageCollector)
        if (options.processingClasspath.isNotEmpty()) {
            val kotlinSymbolProcessingHandlerExtension = KotlinSymbolProcessingExtension(options, logger)
            AnalysisHandlerExtension.registerExtension(project, kotlinSymbolProcessingHandlerExtension)
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
    );
}

class KspOptions(
    val projectBaseDir: File?,
    val compileClasspath: List<File>,
    val javaSourceRoots: List<File>,

    val classOutputDir: File,
    val javaOutputDir: File,
    val kotlinOutputDir: File,
    val resourceOutputDir: File,

    val processingClasspath: List<File>,
    val processors: List<String>,

    val processingOptions: Map<String, String>
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

        fun build(): KspOptions {
            return KspOptions(
                projectBaseDir, compileClasspath, javaSourceRoots,
                classOutputDir!!,
                javaOutputDir!!,
                kotlinOutputDir!!,
                resourceOutputDir!!,
                processingClasspath, processors, processingOptions
            )
        }
    }
}
