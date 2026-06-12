package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
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
        // getSymbolsWithAnnotation only returns symbols in the current compilation,
        // so the record class from the library module is looked up explicitly.
        val sourceRecords = resolver.getSymbolsWithAnnotation("kotlin.jvm.JvmRecord")
            .filterIsInstance<KSClassDeclaration>()
        val libRecords = listOfNotNull(resolver.getClassDeclarationByName("LibRecord"))
        (sourceRecords + libRecords)
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
