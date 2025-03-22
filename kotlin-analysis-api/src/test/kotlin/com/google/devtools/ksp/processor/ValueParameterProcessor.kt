package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class ValueParameterProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (clsName in listOf("MyClassSrc", "MyClassLib")) {
            val clsDecl = resolver.getClassDeclarationByName(clsName)!!
            clsDecl.primaryConstructor!!.parameters.sortedBy { it.name!!.asString() }.forEach {
                results.add("$clsName.${it.name!!.asString()}: isVal: ${it.isVal}, isVar: ${it.isVar}")
            }
        }
        return emptyList()
    }
}
