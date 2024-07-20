package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration

class TypeParameterVarianceProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        fun KSDeclaration.printTypeParams(): String {
            val params = typeParameters.joinToString {
                "${it.variance} ${it.name.asString()} ${it.bounds.joinToString { it.resolve().toString() }}"
            }
            return "${simpleName.asString()} $params"
        }

        results.add(resolver.getClassDeclarationByName("Bar")!!.printTypeParams())
        results.add(resolver.getClassDeclarationByName("BarIn")!!.printTypeParams())
        results.add(resolver.getClassDeclarationByName("BarOut")!!.printTypeParams())
        results.add(resolver.getClassDeclarationByName("BarBounds")!!.printTypeParams())

        return emptyList()
    }
}
