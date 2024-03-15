package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class GetSubPackagesProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        addPackage("lib1", resolver)
        addPackage("lib2", resolver)
        addPackage("main", resolver)
        addPackage("non.exist", resolver)
        return emptyList()
    }

    @KspExperimental
    private fun addPackage(name: String, resolver: Resolver) {
        results.add("subpackages of $name")
        results += resolver.getSubpackagesOf(name).toList()
    }
}
