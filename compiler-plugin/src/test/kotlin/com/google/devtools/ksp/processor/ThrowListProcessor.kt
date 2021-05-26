package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType

@KspExperimental
class ThrowListProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val klass = resolver.getClassDeclarationByName("ThrowsKt")!!
        val jlass = resolver.getClassDeclarationByName("ThrowsException")!!
        result.add(resolver.getJvmCheckedException(klass.declarations.single { it.simpleName.asString() == "throwsKT" } as KSFunctionDeclaration).toResult())
        result.add(resolver.getJvmCheckedException((jlass.declarations.single { it.simpleName.asString() == "foo" } as KSFunctionDeclaration)).toResult())
        return emptyList()
    }

    private fun Sequence<KSType>.toResult() = this.joinToString(separator = ",") { it.declaration.qualifiedName!!.asString() }
}
