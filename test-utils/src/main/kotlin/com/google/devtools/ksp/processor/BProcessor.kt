package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

open class BProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getClassDeclarationByName("BaseClass")!!.let { cls ->
            println(cls.getDeclaredProperties().map { "${it.simpleName.asString()}(${it.hasBackingField})" }.toList())
            // `hasBackingField` is true when running the test individually but is false when running the whole KSPAATest.
        }
        return emptyList()
    }
}
