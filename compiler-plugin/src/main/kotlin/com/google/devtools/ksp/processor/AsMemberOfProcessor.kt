package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionType
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability

class AsMemberOfProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver) {
        val baseClass = resolver.getClassDeclarationByName("Base")!!
        val baseProperties = baseClass.getAllProperties()
        val baseFunction = baseClass.getDeclaredFunctions()
        val child1 = resolver.getClassDeclarationByName("Child1")!!.asStarProjectedType()
        results.add("Child1")
        results.addAll(
            baseProperties.map { property ->
                val typeSignature = resolver.asMemberOf(
                    property = property,
                    containing = child1
                ).toSignature()
                "${property.simpleName.asString()}: $typeSignature"
            }
        )
        results.addAll(
            baseFunction.map {function ->
                val functionSignature = resolver.asMemberOf(
                    function = function,
                    containing = child1
                ).toSignature()
                "${function.simpleName.asString()}: $functionSignature"
            }
        )
    }

    private fun KSType.toSignature(): String {
        val qName = this.declaration.qualifiedName!!.asString() + nullability.toSignature()
        if (arguments.isEmpty()) {
            return qName
        }
        val args = arguments.joinToString(", ") {
            it.type?.resolve()?.toSignature() ?: "no-type"
        }
        return "$qName<$args>"
    }

    private fun KSFunctionType.toSignature(): String {
        val returnType = this.returnType?.toSignature() ?: "no-return-type"
        return "$returnType()"
    }

    private fun Nullability.toSignature() = when(this) {
        Nullability.NULLABLE -> "?"
        Nullability.NOT_NULL -> "!!"
        Nullability.PLATFORM -> ""
    }
}