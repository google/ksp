package com.google.devtools.ksp.processing

interface SymbolProcessorProvider {
  fun create(
    options: Map<String, String>,
    kotlinVersion: KotlinVersion,
    codeGenerator: CodeGenerator,
    logger: KSPLogger
  ): SymbolProcessor
}
