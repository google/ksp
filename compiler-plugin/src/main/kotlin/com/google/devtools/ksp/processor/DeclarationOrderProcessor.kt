package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver

class DeclarationOrderProcessor : AbstractTestProcessor() {
    private val result = mutableListOf<String>()
    override fun toResult() = result

    override fun process(resolver: Resolver) {
        val classNames = listOf(
            //"ClassInModule1", "JavaClassInModule1",
            "ClassInMain"//, "JavaClassInMain"
        )
        classNames.mapNotNull {
            resolver.getClassDeclarationByName(it)
        }.forEach { klass ->
            klass.declarations.forEach {
                println("${klass.qualifiedName?.asString()} $it")
                println("$it ${it.location}")
                result.add("$it : ${it.location}")
            }
        }
    }
}