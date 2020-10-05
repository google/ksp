package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionType
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.symbol.impl.kotlin.KSTypeArgumentLiteImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSTypeReferenceImpl
import com.google.devtools.ksp.symbol.impl.synthetic.KSTypeReferenceSyntheticImpl

class AsMemberOfProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver) {
        val child1 = resolver.getClassDeclarationByName("Child1")!!
        addToResults(resolver, child1.asStarProjectedType())
        val child2 = resolver.getClassDeclarationByName("Child2")!!
        addToResults(resolver, child2.asStarProjectedType())
        val child2WithString = resolver.getAllFiles().first {
            it.fileName == "Input.kt"
        }.declarations.first {
            it.simpleName.asString() == "child2WithString"
        } as KSPropertyDeclaration
        addToResults(resolver, child2WithString.type.resolve())
    }

    private fun addToResults(resolver: Resolver, child: KSType) {
        results.add(child.toSignature())
        val baseClass = resolver.getClassDeclarationByName("Base")!!
        val baseProperties = baseClass.getAllProperties()
        val baseFunction = baseClass.getDeclaredFunctions()
        results.addAll(
            baseProperties.map { property ->
                val typeSignature = resolver.asMemberOf(
                    property = property,
                    containing = child
                ).toSignature()
                "${property.simpleName.asString()}: $typeSignature"
            }
        )
        results.addAll(
            baseFunction.map { function ->
                val functionSignature = resolver.asMemberOf(
                    function = function,
                    containing = child
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

    private fun KSTypeParameter.toSignature(): String {
        val boundsSignature = if (bounds.isEmpty()) {
            ""
        } else {
            bounds.joinToString(
                separator = ", ",
                prefix = ": "
            ) {
                it.resolve().toSignature()
            }
        }
        val varianceSignature = if (variance.label.isBlank()) {
            ""
        } else {
            "${variance.label} "
        }
        val name = this.name.asString()
        return "$varianceSignature$name$boundsSignature"
    }

    private fun KSFunctionType.toSignature(): String {
        val returnType = this.returnType?.toSignature() ?: "no-return-type"
        val params = parametersTypes.joinToString(", ") {
            it?.toSignature() ?: "no-type-param"
        }
        val paramTypeArgs = this.typeParameters.joinToString(", ") {
            it.toSignature()
        }
        val paramTypesSignature = if (paramTypeArgs.isBlank()) {
            ""
        } else {
            "<$paramTypeArgs>"
        }
        return "$paramTypesSignature($params) -> $returnType"
    }

    private fun Nullability.toSignature() = when(this) {
        Nullability.NULLABLE -> "?"
        Nullability.NOT_NULL -> "!!"
        Nullability.PLATFORM -> ""
    }
}