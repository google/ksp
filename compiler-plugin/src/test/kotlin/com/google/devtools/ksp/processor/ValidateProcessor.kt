package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate

class ValidateProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    private fun validate(symbol: KSDeclaration, predicate: (KSNode?, KSNode) -> Boolean = { _, _ -> true }) {
        if (symbol.validate(predicate)) {
            results.add("${symbol.simpleName.asString()} valid")
        } else {
            results.add("${symbol.simpleName.asString()} invalid")
        }
    }

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val ErrorInMember = resolver.getClassDeclarationByName("ErrorInMember")!!
        val SkipErrorInMember = resolver.getClassDeclarationByName("SkipErrorInMember")!!
        val GoodClass = resolver.getClassDeclarationByName("GoodClass")!!
        val C = resolver.getClassDeclarationByName("C")!!
        val BadJavaClass = resolver.getClassDeclarationByName("BadJavaClass")!!
        val ErrorAnnotationType = resolver.getClassDeclarationByName("ErrorAnnotationType")!!
        validate(ErrorInMember)
        ErrorInMember.declarations.forEach { validate(it) }
        validate(SkipErrorInMember) { node, _ ->
            node !is KSPropertyDeclaration && node !is KSFunctionDeclaration
        }
        SkipErrorInMember.declarations.forEach {
            validate(it) { node, _ ->
                node !is KSPropertyDeclaration && node !is KSFunctionDeclaration
            }
        }
        validate(GoodClass)
        validate(C)
        validate(BadJavaClass)
        validate(ErrorAnnotationType)
        return emptyList()
    }
}
