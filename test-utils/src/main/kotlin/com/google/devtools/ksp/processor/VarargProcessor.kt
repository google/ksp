package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class VarargProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (clsName in listOf("s.K", "s.J", "l.K", "l.J")) {
            val clsDecl = resolver.getClassDeclarationByName(clsName)!!
            val func = clsDecl.getDeclaredFunctions().single { it.simpleName.asString() == "foo" }
            val funcName = func.qualifiedName!!.asString()
            val param = func.parameters.single()
            val paramName = param.name!!.asString()
            val paramType = param.type.resolve()
            val isVararg = if (param.isVararg) "vararg " else ""
            results.add("$funcName($isVararg$paramName: $paramType)")
        }
        return emptyList()
    }
}
