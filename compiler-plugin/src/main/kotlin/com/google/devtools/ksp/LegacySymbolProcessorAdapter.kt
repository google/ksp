package com.google.devtools.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider

internal class LegacySymbolProcessorAdapter(
    private val symbolProcessor: SymbolProcessor
) : SymbolProcessorProvider {
    override fun create(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ): SymbolProcessor = symbolProcessor.apply {
        init(options, kotlinVersion, codeGenerator, logger)
    }
}
