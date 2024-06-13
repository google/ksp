package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class JvmNameProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        listOf("TestDataClass", "TestLibDataClass").forEach { clsName ->
            resolver.getClassDeclarationByName(clsName)?.let { cls ->
                results.add(
                    cls.getAllProperties().map {
                        "(${it.getter?.let { resolver.getJvmName(it) }}, " +
                            "${it.setter?.let { resolver.getJvmName(it) }})"
                    }.toList().joinToString()
                )
            }
        }
        return emptyList()
    }
}
