package com.google.devtools.ksp.processing

class SymbolProcessorEnvironment(
    /**
     * passed from command line, Gradle, etc.
     */
    val options: Map<String, String>,
    /**
     * language version of compilation environment.
     */
    val kotlinVersion: KotlinVersion,
    /**
     * creates managed files.
     */
    val codeGenerator: CodeGenerator,
    /**
     * for logging to build output.
     */
    val logger: KSPLogger
)
