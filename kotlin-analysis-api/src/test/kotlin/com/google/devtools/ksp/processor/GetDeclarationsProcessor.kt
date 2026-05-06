package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class GetDeclarationsProcessor(val declarationNames: List<String>) : AbstractTestProcessor() {
    private val result = mutableListOf<String>()

    override fun toResult(): List<String> = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        declarationNames.forEach { declName ->
            resolver.getClassDeclarationByName(declName)?.declarations?.forEach { decl ->
                result.add("Declaration simpleName: ${decl.simpleName.asString()}")
                decl.qualifiedName?.let {
                    result.add("Declaration qualifiedName: ${it.asString()} (Origin: ${decl.origin})")
                }
            }
        }
        return emptyList()
    }
}
