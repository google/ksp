package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

class ExitCodeProcessor : AbstractTestProcessor() {
    override fun toResult(): List<String> = emptyList()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (resolver.getNewFiles().single().fileName == "PrintError.kt") {
            env.logger.error("An error")
        }

        return emptyList()
    }

    lateinit var env: SymbolProcessorEnvironment

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return this
    }
}
