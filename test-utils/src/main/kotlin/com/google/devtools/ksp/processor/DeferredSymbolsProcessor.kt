package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

class DeferredSymbolsProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult(): List<String> {
        return result.sorted()
    }

    var round = 0
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()

        if (round++ == 0) {
            env.codeGenerator.createNewFile(Dependencies(false), "", "Unused", "kt").use {
                it.write("class Unused".toByteArray())
            }

            deferred.addAll(resolver.getSymbolsWithAnnotation("Defer"))
        } else {
            val symbols = resolver.getSymbolsWithAnnotation("Defer")
            result.addAll(symbols.map(Any::toString).toList())
        }

        return deferred
    }

    lateinit var env: SymbolProcessorEnvironment

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return this
    }
}
