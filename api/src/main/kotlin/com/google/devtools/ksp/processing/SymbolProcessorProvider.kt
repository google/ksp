package com.google.devtools.ksp.processing

/**
 * [SymbolProcessorProvider] is the interface used by plugins to integrate into Kotlin Symbol Processing.
 */
interface SymbolProcessorProvider {
    /**
     * Called by Kotlin Symbol Processing to create the processor.
     */
    fun create(environment: SymbolProcessorEnvironment): SymbolProcessor
}
