package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

open class ObjCacheBProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getClassDeclarationByName("BaseClass")!!.let { cls ->
            results.addAll(cls.getDeclaredProperties().map { "${it.simpleName.asString()}(${it.hasBackingField})" })
        }
        return emptyList()
    }
}
