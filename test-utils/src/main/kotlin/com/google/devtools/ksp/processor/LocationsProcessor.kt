package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.NonExistLocation
import java.io.File

class LocationsProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult(): List<String> {
        return result.sorted()
    }

    var round = 0
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (round++ == 0) {
            resolver.getSymbolsWithAnnotation("Location").forEach {
                when (val location = it.location) {
                    is FileLocation -> {
                        val filename = File(location.filePath).name
                        val line = location.lineNumber
                        result.add("$it:$filename:$line")
                    }
                    is NonExistLocation -> result.add("$it:NonExistLocation")
                }
            }
        }

        return emptyList()
    }

    lateinit var env: SymbolProcessorEnvironment

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return this
    }
}
