package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class TypeAnnotationProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val myList = resolver.getClassDeclarationByName("MyClass")!!.getDeclaredProperties().single()
        val myStringClass = resolver.getClassDeclarationByName("MyStringClass")!!.asStarProjectedType()
        result.add(myList.type.resolve().annotations.joinToString())
        result.add(myList.asMemberOf(myStringClass).annotations.joinToString())
        result.add(myList.type.resolve().let { it.replace(it.arguments) }.annotations.joinToString())
        result.add(myList.type.resolve().starProjection().annotations.joinToString())
        return emptyList()
    }
}
