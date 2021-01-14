package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class ImplicitPropertyAccessorProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver) {
        val foo = resolver.getClassDeclarationByName("Foo")!!
        foo.declarations.filterIsInstance<KSPropertyDeclaration>().forEach { prop ->
            result.add(prop.getter?.returnType.toString())
            prop.setter?.parameter?.let {
                result.add(it.toString())
                result.add(it.type.toString())
            }
        }
    }
}