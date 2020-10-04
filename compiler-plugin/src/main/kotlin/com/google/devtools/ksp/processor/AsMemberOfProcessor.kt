package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability

class AsMemberOfProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver) {
        val baseProperties = resolver.getClassDeclarationByName("Base")!!.getAllProperties()
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

    private fun Nullability.toSignature() = when(this) {
        Nullability.NULLABLE -> "?"
        Nullability.NOT_NULL -> "!!"
        Nullability.PLATFORM -> ""
    }
}