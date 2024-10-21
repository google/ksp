package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class ImplicitPropertyAccessorProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        listOf("Foo", "lib.Bar").forEach { clsName ->
            val cls = resolver.getClassDeclarationByName(clsName)!!
            cls.declarations.filterIsInstance<KSPropertyDeclaration>().forEach { prop ->
                prop.getter?.let { getter ->
                    result.add("$getter: ${getter.returnType}")
                }
                prop.setter?.let { setter ->
                    val param = setter.parameter
                    val paramName = param.name?.getShortName()
                    result.add("$setter($paramName: ${setter.parameter.type})")
                }
            }
        }
        return emptyList()
    }
}
