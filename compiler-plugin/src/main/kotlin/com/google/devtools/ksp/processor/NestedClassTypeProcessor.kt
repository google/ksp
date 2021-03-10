package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.impl.kotlin.KSPropertyDeclarationImpl

class NestedClassTypeProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val c = resolver.getClassDeclarationByName("C")!!
        c.declarations.filterIsInstance<KSPropertyDeclaration>()
                .forEach{
                    result.add(it.simpleName.asString())
                    result.add(it.type.resolve().arguments.map { it.type?.annotations?.joinToString(separator = ",") { it.toString() } }.joinToString())
                    result.add(it.type.resolve().arguments.joinToString(separator = ",") { it.toString() })
                }
        return emptyList()
    }
}