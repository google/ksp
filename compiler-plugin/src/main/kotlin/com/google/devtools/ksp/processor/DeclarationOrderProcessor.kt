package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.impl.binary.DeclarationOrdering

class DeclarationOrderProcessor : AbstractTestProcessor() {
    private val result = mutableListOf<String>()
    override fun toResult() = result

    override fun process(resolver: Resolver) {
        val originalStrictMode = DeclarationOrdering.STRICT_MODE
        try {
            DeclarationOrdering.STRICT_MODE = true
            val classNames = listOf(
                "lib.KotlinClass", "lib.JavaClass",
                "KotlinClass", "JavaClass"
            )
            classNames.map {
                checkNotNull(resolver.getClassDeclarationByName(it)) {
                    "cannot find $it"
                }
            }.forEach { klass ->
                result.add(klass.qualifiedName!!.asString())
                result.addAll(klass.getDeclaredProperties().map { it.toSignature(resolver) })
                result.addAll(klass.getDeclaredFunctions().map { it.toSignature(resolver) })
            }
        } finally {
            DeclarationOrdering.STRICT_MODE = originalStrictMode
        }
    }

    @OptIn(KspExperimental::class)
    private fun KSDeclaration.toSignature(
        resolver: Resolver
    ) = "${simpleName.asString()}:${resolver.mapToJvmSignature(this)}"
}