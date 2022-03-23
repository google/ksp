package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class HelloWorldProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val fooKt = resolver.getAllFiles().single()
        results.add(fooKt.fileName)
        val bar = resolver.getAllFiles().single { it.fileName == "Foo.kt" }
            .declarations.single { it.simpleName.asString() == "Foo" }
        results.add(bar.simpleName.asString())
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
