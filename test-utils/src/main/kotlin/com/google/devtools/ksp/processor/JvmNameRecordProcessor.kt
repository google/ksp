package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class JvmNameRecordProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("kotlin.jvm.JvmRecord")
            .filterIsInstance<KSClassDeclaration>()
            .flatMap { cls ->
                cls.getAllProperties().map { property ->
                    val accessorNames = listOfNotNull(
                        property.getter?.let { resolver.getJvmName(it) },
                        property.setter?.let { resolver.getJvmName(it) },
                    )
                    "${cls.simpleName.asString()}.${property.simpleName.asString()}: ${accessorNames.joinToString()}"
                }
            }
            .sorted()
            .let { results.addAll(it) }
        return emptyList()
    }
}
