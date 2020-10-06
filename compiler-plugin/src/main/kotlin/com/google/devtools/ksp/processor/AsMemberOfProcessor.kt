package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionType
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Nullability

class AsMemberOfProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver) {
        val base = resolver.getClassDeclarationByName("Base")!!
        val child1 = resolver.getClassDeclarationByName("Child1")!!
        addToResults(resolver, base, child1.asStarProjectedType())
        val child2 = resolver.getClassDeclarationByName("Child2")!!
        addToResults(resolver, base, child2.asStarProjectedType())
        val child2WithString = resolver.getTestProperty("child2WithString")
        addToResults(resolver, base, child2WithString.type.resolve())

        // check cases where given type is not a subtype hence it doesn't have any impact.
        val listOfStrings = resolver.getTestProperty("listOfStrings").type.resolve()
        val setOfStrings = resolver.getTestProperty("setOfStrings").type.resolve()
        val listClass = resolver.getClassDeclarationByName("kotlin.collections.List")!!
        val setClass = resolver.getClassDeclarationByName("kotlin.collections.Set")!!
        val listGet = listClass.getAllFunctions().first {
            it.simpleName.asString() == "get"
        }
        results.add("List#get")
        results.add("listOfStrings " + resolver.asMemberOf(listGet, listOfStrings).toSignature())
        results.add("setOfStrings " + resolver.asMemberOf(listGet, setOfStrings).toSignature())

        val setContains = setClass.getAllFunctions().first {
            it.simpleName.asString() == "contains"
        }
        results.add("Set#contains")
        results.add("listOfStrings " + resolver.asMemberOf(setContains, listOfStrings).toSignature())
        results.add("setOfStrings " + resolver.asMemberOf(setContains, setOfStrings).toSignature())

        val javaBase = resolver.getClassDeclarationByName("JavaBase")!!
        val javaChild1 = resolver.getClassDeclarationByName("JavaChild1")!!
        addToResults(resolver, javaBase, javaChild1.asStarProjectedType())
    }

    private fun Resolver.getTestProperty(propertyName: String): KSPropertyDeclaration {
        return getAllFiles().first {
            it.fileName == "Input.kt"
        }.declarations.first {
            it.simpleName.asString() == propertyName
        } as KSPropertyDeclaration
    }

    private fun addToResults(resolver: Resolver, baseClass: KSClassDeclaration, child: KSType) {
        results.add(child.toSignature())
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
        val name = this.declaration.qualifiedName?.asString() ?: this.declaration.simpleName.asString()
        val qName = name + nullability.toSignature()
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
        val params = parameterTypes.joinToString(", ") {
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
        val receiverSignature = if (extensionReceiverType != null) {
            extensionReceiverType!!.toSignature() + "."
        } else {
            ""
        }
        return "$receiverSignature$paramTypesSignature($params) -> $returnType"
    }

    private fun Nullability.toSignature() = when(this) {
        Nullability.NULLABLE -> "?"
        Nullability.NOT_NULL -> "!!"
        Nullability.PLATFORM -> ""
    }
}
