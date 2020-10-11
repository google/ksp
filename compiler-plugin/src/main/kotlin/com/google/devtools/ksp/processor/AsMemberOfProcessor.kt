package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Nullability

@Suppress("unused") // used by generated tests
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
        val child2WithString = resolver.getDeclaration<KSPropertyDeclaration>("child2WithString")
        addToResults(resolver, base, child2WithString.type.resolve())

        // check cases where given type is not a subtype
        val notAChild = resolver.getClassDeclarationByName("NotAChild")!!
        addToResults(resolver, base, notAChild.asStarProjectedType())
        val listOfStrings = resolver.getDeclaration<KSPropertyDeclaration>("listOfStrings").type.resolve()
        val setOfStrings = resolver.getDeclaration<KSPropertyDeclaration>("setOfStrings").type.resolve()
        val listClass = resolver.getClassDeclarationByName("kotlin.collections.List")!!
        val setClass = resolver.getClassDeclarationByName("kotlin.collections.Set")!!
        val listGet = listClass.getAllFunctions().first {
            it.simpleName.asString() == "get"
        }
        results.add("List#get")
        results.add("listOfStrings: " + resolver.asMemberOfSignature(listGet, listOfStrings))
        results.add("setOfStrings: " + resolver.asMemberOfSignature(listGet, setOfStrings))

        val setContains = setClass.getAllFunctions().first {
            it.simpleName.asString() == "contains"
        }
        results.add("Set#contains")
        results.add("listOfStrings: " + resolver.asMemberOfSignature(setContains, listOfStrings))
        results.add("setOfStrings: " + resolver.asMemberOfSignature(setContains, setOfStrings))

        val javaBase = resolver.getClassDeclarationByName("JavaBase")!!
        val javaChild1 = resolver.getClassDeclarationByName("JavaChild1")!!
        addToResults(resolver, javaBase, javaChild1.asStarProjectedType())

        val fileLevelFunction = resolver.getDeclaration<KSFunctionDeclaration>("fileLevelFunction")
        results.add("fileLevelFunction: " + resolver.asMemberOfSignature(fileLevelFunction, listOfStrings))

        // TODO we should eventually support this, probably as different asReceiverOf kind of API
        val fileLevelExtensionFunction = resolver.getDeclaration<KSFunctionDeclaration>("fileLevelExtensionFunction")
        results.add("fileLevelExtensionFunction: " + resolver.asMemberOfSignature(fileLevelExtensionFunction, listOfStrings))

        val fileLevelProperty = resolver.getDeclaration<KSPropertyDeclaration>("fileLevelProperty")
        results.add("fileLevelProperty: " + resolver.asMemberOfSignature(fileLevelProperty, listOfStrings))

        val errorType = resolver.getDeclaration<KSPropertyDeclaration>("errorType").type.resolve()
        results.add("errorType: " + resolver.asMemberOfSignature(listGet, errorType))
    }

    private inline fun <reified T : KSDeclaration> Resolver.getDeclaration(name: String): T {
        return getAllFiles().first {
            it.fileName == "Input.kt"
        }.declarations.filterIsInstance<T>().first {
            it.simpleName.asString() == name
        }
    }

    private fun addToResults(resolver: Resolver, baseClass: KSClassDeclaration, child: KSType) {
        results.add(child.toSignature())
        val baseProperties = baseClass.getAllProperties()
        val baseFunction = baseClass.getDeclaredFunctions()
        results.addAll(
            baseProperties.map { property ->
                val typeSignature = resolver.asMemberOfSignature(
                    property = property,
                    containing = child
                )
                "${property.simpleName.asString()}: $typeSignature"
            }
        )
        results.addAll(
            baseFunction.map { function ->
                val functionSignature = resolver.asMemberOfSignature(
                    function = function,
                    containing = child
                )
                "${function.simpleName.asString()}: $functionSignature"
            }
        )
    }

    private fun Resolver.asMemberOfSignature(
        function: KSFunctionDeclaration,
        containing: KSType
    ): String {
        val result = kotlin.runCatching {
            asMemberOf(function, containing)
        }
        return if (result.isSuccess) {
            result.getOrThrow().toSignature()
        } else {
            result.exceptionOrNull()!!.toSignature()
        }
    }

    private fun Resolver.asMemberOfSignature(
        property: KSPropertyDeclaration,
        containing: KSType
    ): String {
        val result = kotlin.runCatching {
            asMemberOf(property, containing)
        }
        return if (result.isSuccess) {
            result.getOrThrow().toSignature()
        } else {
            result.exceptionOrNull()!!.toSignature()
        }
    }

    private fun Throwable.toSignature() = "${this::class.qualifiedName}: $message"
    private fun KSType.toSignature(): String {
        val name = this.declaration.qualifiedName?.asString()
            ?: this.declaration.simpleName.asString()
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

    private fun KSFunction.toSignature(): String {
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

    private fun Nullability.toSignature() = when (this) {
        Nullability.NULLABLE -> "?"
        Nullability.NOT_NULL -> "!!"
        Nullability.PLATFORM -> ""
    }
}
