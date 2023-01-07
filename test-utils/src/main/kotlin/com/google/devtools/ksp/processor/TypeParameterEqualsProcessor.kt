package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class TypeParameterEqualsProcessor : AbstractTestProcessor() {
    val result = mutableListOf<Boolean>()

    override fun toResult(): List<String> {
        return result.map { it.toString() }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val foo = resolver.getClassDeclarationByName("Foo")!!
        val i = resolver.getClassDeclarationByName("I")!!
        result.add(foo.typeParameters.first() == foo.getDeclaredProperties().first().type.resolve().declaration)
        result.add(i.typeParameters[0] == i.typeParameters[1].bounds.single().resolve().declaration)
        return emptyList()
    }
}
