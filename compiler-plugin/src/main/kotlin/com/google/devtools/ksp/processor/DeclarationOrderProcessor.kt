package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSName

class DeclarationOrderProcessor : AbstractTestProcessor() {
    private val result = mutableListOf<KSName>()
    override fun toResult() = result.map { it.asString() }

    override fun process(resolver: Resolver) {
        val classNames = listOf(
            "lib.KotlinClass", "lib.JavaClass",
            "KotlinClass", "JavaClass"
        )
        classNames.map {
            checkNotNull(resolver.getClassDeclarationByName(it)) {
                "cannot find $it"
            }
        }.forEach { klass ->
            result.add(klass.qualifiedName!!)
            result.addAll(klass.getDeclaredProperties().map { it.simpleName })
            result.addAll(klass.getDeclaredFunctions().map { it.simpleName })
        }
    }
}