/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp

import com.intellij.mock.MockProject
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
        KspCliOption.GENERATED_SOURCES_DIR_OPTION -> sourcesOutputDir = File(value)
        KspCliOption.GENERATED_CLASSES_DIR_OPTION -> classesOutputDir = File(value)
    }
}

class KotlinSymbolProcessingComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val options = configuration[KSP_OPTIONS]?.build() ?: return
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

    GENERATED_SOURCES_DIR_OPTION(
        "sources",
        "<sources>",
        "Dir of generated sources",
        false
    ),

    GENERATED_CLASSES_DIR_OPTION(
        "classes",
        "<classes>",
        "Dir of generated classes",
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

    val sourcesOutputDir: File,
    val classesOutputDir: File,

    val processingClasspath: List<File>,
    val processors: List<String>,

    val processingOptions: Map<String, String>
) {
    class Builder {
        var projectBaseDir: File? = null
        val compileClasspath: MutableList<File> = mutableListOf()
        val javaSourceRoots: MutableList<File> = mutableListOf()

        var sourcesOutputDir: File? = null
        var classesOutputDir: File? = null

        val processingClasspath: MutableList<File> = mutableListOf()
        val processors: MutableList<String> = mutableListOf()

        val processingOptions: MutableMap<String, String> = mutableMapOf()

        fun build(): KspOptions {
            return KspOptions(
                projectBaseDir, compileClasspath, javaSourceRoots,
                sourcesOutputDir!!, classesOutputDir!!,
                processingClasspath, processors, processingOptions
            )
        }
    }
}
