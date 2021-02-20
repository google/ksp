package com.google.devtools.ksp.processing

/**
 * [SymbolProcessorProvider] is the interface used by plugins to integrate into Kotlin Symbol Processing.
 */
interface SymbolProcessorProvider {
    /**
    * Called by Kotlin Symbol Processing to create the processor.
    *
    * @param options passed from command line, Gradle, etc.
    * @param kotlinVersion language version of compilation environment.
    * @param codeGenerator creates managed files.
    * @param logger for logging to build output.
    */
    fun create(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ): SymbolProcessor
}
