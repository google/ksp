package com.google.devtools.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

internal class LegacySymbolProcessorAdapter(
    private val symbolProcessor: SymbolProcessor
) : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = symbolProcessor.apply {
        init(environment.options, environment.kotlinVersion, environment.codeGenerator, environment.logger)
    }
}
