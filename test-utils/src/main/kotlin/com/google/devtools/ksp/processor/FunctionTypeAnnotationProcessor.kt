package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class FunctionTypeAnnotationProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val files = resolver.getNewFiles()

        files.forEach {
            it.accept(
                object : KSTopDownVisitor<Unit, Unit>() {
                    override fun defaultHandler(node: KSNode, data: Unit) = Unit

                    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
                        val type = property.type.resolve()
                        val propertyName = property.simpleName.asString()
                        val typeName = type.declaration.simpleName.asString()
                        results.add("$propertyName: $typeName ${type.annotations.joinToString { it.toString() }}")
                    }
                },
                Unit
            )
        }
        return emptyList()
    }
}
