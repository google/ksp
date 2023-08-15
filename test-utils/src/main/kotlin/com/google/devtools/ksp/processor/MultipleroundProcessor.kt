package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

class MultipleroundProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult(): List<String> {
        return result
    }

    var round = 0
    override fun process(resolver: Resolver): List<KSAnnotated> {
        fun gen(cls: String, ext: String) {
            env.codeGenerator.createNewFile(Dependencies(false), "com.example", cls, ext).use {
                it.write("package com.example;".toByteArray())
                it.write("interface $cls {}".toByteArray())
            }
        }

        when (round) {
            0 -> gen("I0", "kt")
            1 -> gen("I1", "java")
            2 -> gen("I2", "kt")
            3 -> gen("I3", "java")
            4 -> gen("I4", "kt")
            5 -> gen("I5", "java")
        }

        fun check(cls: String) {
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(cls))?.let { c ->
                result.add(
                    "${c.simpleName.asString()} : " +
                        c.superTypes.map { it.resolve().declaration.simpleName.asString() }.joinToString()
                )
            }
        }

        result.add("Round $round:")
        check("K")
        check("J")
        val newFiles = resolver.getNewFiles().map { it.fileName }.toSet()
        val allFiles = resolver.getAllFiles().map { it.fileName }
        result.add(allFiles.map { if (it in newFiles) "+$it" else it }.sorted().joinToString())

        round++
        return emptyList()
    }

    lateinit var env: SymbolProcessorEnvironment

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return this
    }
}
