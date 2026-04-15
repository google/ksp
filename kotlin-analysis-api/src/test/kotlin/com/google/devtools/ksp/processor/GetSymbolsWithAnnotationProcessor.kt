package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode

class GetSymbolsWithAnnotationProcessor(val annotationNames: List<String>) : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results.toList().sorted()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        annotationNames.forEach { annotationName ->
            resolver.getSymbolsWithAnnotation(annotationName).forEach {
                results.add("$annotationName: ${it.fqn}")
            }
        }
        return emptyList()
    }

    private val KSAnnotated.fqn: String
        get() = findAllQualifiers(this).joinToString(separator = ".")

    private fun findAllQualifiers(node: KSNode): List<KSNode> {
        val result = mutableListOf(node)
        var current = node.parent
        while (current != null && current !is KSFile) {
            result.add(current)
            current = current.parent
        }
        result.reverse()
        return result
    }
}
