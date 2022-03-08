package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class RawTypesProcessor : AbstractTestProcessor() {
    private val rawTypedEntityNames = arrayListOf<String>()

    override fun toResult(): List<String> {
        return rawTypedEntityNames.sorted()
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val visitor = object : KSTopDownVisitor<Unit, Unit>() {
            private val KSType.isRawType
                get() = resolver.isJavaRawType(this)

            override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
                function.parameters.forEach { param ->
                    val type = param.type.resolve()
                    if (type.isRawType) {
                        rawTypedEntityNames += param.name?.asString() ?: "???"
                    }
                }
                val returnType = function.returnType?.resolve() ?: return
                if (returnType.isRawType) {
                    rawTypedEntityNames += function.simpleName.asString()
                }
            }

            override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
                val type = property.type.resolve()
                if (type.isRawType) {
                    rawTypedEntityNames += property.simpleName.asString()
                }
            }

            override fun defaultHandler(node: KSNode, data: Unit) = Unit
        }

        resolver.getDeclarationsFromPackage("").forEach { it.accept(visitor, Unit) }

        return emptyList()
    }
}
