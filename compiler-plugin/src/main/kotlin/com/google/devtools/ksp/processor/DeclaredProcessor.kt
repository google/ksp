package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class DeclaredProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val sub = resolver.getClassDeclarationByName("Sub")!!
        val base = resolver.getClassDeclarationByName("Base")!!
        val javasource = resolver.getClassDeclarationByName("JavaSource")!!
        result.add("Base class declared functions:")
        sub.declarations.forEach { result.add(it.toString()) }
        result.add("Sub class declared functions:")
        base.declarations.forEach { result.add(it.toString()) }
        result.add("JavaSource class declared functions:")
        javasource.declarations.forEach { result.add(it.toString()) }
        return emptyList()
    }
}