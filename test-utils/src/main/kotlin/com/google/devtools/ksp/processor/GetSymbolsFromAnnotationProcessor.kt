package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class GetSymbolsFromAnnotationProcessor : AbstractTestProcessor() {
    val result = mutableListOf<List<String>>()
    override fun toResult(): List<String> = result.flatten()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        result.add("==== Anno inDepth = false ====")
        resolver.getSymbolsWithAnnotation("Anno", inDepth = false).prepare().let { result.add(it) }
        result.add("==== Anno inDepth = true ====")
        resolver.getSymbolsWithAnnotation("Anno", inDepth = true).prepare().let { result.add(it) }
        result.add("==== Bnno inDepth = false ====")
        resolver.getSymbolsWithAnnotation("Bnno", inDepth = false).prepare().let { result.add(it) }
        result.add("==== Bnno inDepth = true ====")
        resolver.getSymbolsWithAnnotation("Bnno", inDepth = true).prepare().let { result.add(it) }
        result.add("==== A1 inDepth = false ====")
        resolver.getSymbolsWithAnnotation("A1", inDepth = false).prepare().let { result.add(it) }
        result.add("==== A1 inDepth = true ====")
        resolver.getSymbolsWithAnnotation("A1", inDepth = true).prepare().let { result.add(it) }
        result.add("==== A2 inDepth = false ====")
        resolver.getSymbolsWithAnnotation("A2", inDepth = false).prepare().let { result.add(it) }
        result.add("==== A2 inDepth = true ====")
        resolver.getSymbolsWithAnnotation("A2", inDepth = true).prepare().let { result.add(it) }
        result.add("==== Cnno inDepth = true ====")
        resolver.getSymbolsWithAnnotation("Cnno", inDepth = true).prepare().let { result.add(it) }
        return emptyList()
    }

    private fun Sequence<KSAnnotated>.prepare(): List<String> = toList().map { toString(it) }.sorted()

    private fun <A> MutableList<List<A>>.add(el: A) = add(listOf(el))

    private fun toString(annotated: KSAnnotated): String {
        return "$annotated:${annotated::class.supertypes.first().classifier.toString().substringAfterLast('.')}"
    }
}
