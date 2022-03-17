package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class GetSymbolsFromAnnotationProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult(): List<String> = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        result.add("==== Anno superficial====")
        resolver.getSymbolsWithAnnotation("Anno").forEach { result.add(toString(it)) }
        result.add("==== Anno in depth ====")
        resolver.getSymbolsWithAnnotation("Anno", true).forEach { result.add(toString(it)) }
        result.add("==== Bnno superficial====")
        resolver.getSymbolsWithAnnotation("Bnno").forEach { result.add(toString(it)) }
        result.add("==== Bnno in depth ====")
        resolver.getSymbolsWithAnnotation("Bnno", true).forEach { result.add(toString(it)) }
        result.add("==== A1 superficial====")
        resolver.getSymbolsWithAnnotation("A1").forEach { result.add(toString(it)) }
        result.add("==== A1 in depth ====")
        resolver.getSymbolsWithAnnotation("A1", true).forEach { result.add(toString(it)) }
        result.add("==== A2 superficial====")
        resolver.getSymbolsWithAnnotation("A2").forEach { result.add(toString(it)) }
        result.add("==== A2 in depth ====")
        resolver.getSymbolsWithAnnotation("A2", true).forEach { result.add(toString(it)) }
        result.add("==== Cnno in depth ====")
        resolver.getSymbolsWithAnnotation("Cnno", true).forEach { result.add(toString(it)) }
        return emptyList()
    }

    fun toString(annotated: KSAnnotated): String {
        return "$annotated:${annotated::class.supertypes.first().classifier.toString().substringAfterLast('.')}"
    }
}
