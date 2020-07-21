/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.utils.decodePluginOptions
import java.io.File

private val KSP_OPTIONS = CompilerConfigurationKey.create<KspOptions.Builder>("Ksp options")

class KotlinSymbolProcessingCommandLineProcessor : CommandLineProcessor {
    override val pluginId = "org.jetbrains.kotlin.ksp"

    override val pluginOptions: Collection<AbstractCliOption> = KspCliOption.values().asList()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        if (option !is KspCliOption) {
            throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }

        val kspOptions = configuration[KSP_OPTIONS]
            ?: KspOptions.Builder().also { configuration.put(KSP_OPTIONS, it) }

        if (option == @Suppress("DEPRECATION") KspCliOption.CONFIGURATION) {
            configuration.applyOptionsFrom(decodePluginOptions(value), pluginOptions)
        } else {
            kspOptions.processOption(option, value)
        }
    }

    private fun KspOptions.Builder.processOption(option: KspCliOption, value: String) = when (option) {
        KspCliOption.CONFIGURATION -> throw CliOptionProcessingException("${KspCliOption.CONFIGURATION.optionName} should be handled earlier")
        KspCliOption.PROCESSOR_CLASSPATH_OPTION -> processingClasspath += value.split(':').map{ File(it) }
        KspCliOption.CLASS_OUTPUT_DIR_OPTION -> classOutputDir = File(value)
        KspCliOption.JAVA_OUTPUT_DIR_OPTION -> javaOutputDir = File(value)
        KspCliOption.KOTLIN_OUTPUT_DIR_OPTION -> kotlinOutputDir = File(value)
        KspCliOption.RESOURCE_OUTPUT_DIR_OPTION -> resourceOutputDir = File(value)
    }
}

class KotlinSymbolProcessingComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
        val options = configuration[KSP_OPTIONS]?.apply {
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
        }?.build() ?: return
        if (options.processingClasspath.isNotEmpty()) {
            val kotlinSymbolProcessingHandlerExtension = KotlinSymbolProcessingExtension(options)
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
    @Deprecated("Do not use in CLI")
    CONFIGURATION("configuration", "<encoded>", "Encoded configuration"),

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
