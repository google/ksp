package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeImpl
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class DefaultKClassValueProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val example1 = resolver.getClassDeclarationByName("Example")!!.annotations.first()
        val example2 = resolver.getClassDeclarationByName("Example2")!!.annotations.first()
        val arg1 = (example1.arguments.single().value as KSTypeImpl).declaration.qualifiedName!!
        val defaultArg1 = (example1.defaultArguments.single().value as KSTypeImpl).declaration.qualifiedName!!
        results.add(defaultArg1.asString())
        results.add(arg1.asString())
        val arg2 = (example2.arguments.single().value as KSTypeImpl).declaration.qualifiedName!!
        val defaultArg2 = (example2.defaultArguments.single().value as KSTypeImpl).declaration.qualifiedName!!
        results.add(defaultArg2.asString())
        results.add(arg2.asString())
        return emptyList()
    }
}
