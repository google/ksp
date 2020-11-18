package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.validate

class ValidateProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    private fun validate(symbol: KSDeclaration) {
        if (symbol.validate()) {
            results.add("${symbol.simpleName.asString()} valid")
        } else {
            results.add("${symbol.simpleName.asString()} invalid")
        }
    }
    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver) {
        val ErrorInMember = resolver.getClassDeclarationByName("ErrorInMember")!!
        val GoodClass = resolver.getClassDeclarationByName("GoodClass")!!
        val C = resolver.getClassDeclarationByName("C")!!
        val BadJavaClass = resolver.getClassDeclarationByName("BadJavaClass")!!
        val ErrorAnnotationType = resolver.getClassDeclarationByName("ErrorAnnotationType")!!
        validate(ErrorInMember)
        ErrorInMember.declarations.map { validate(it) }
        validate(GoodClass)
        validate(C)
        validate(BadJavaClass)
        validate(ErrorAnnotationType)
    }
}