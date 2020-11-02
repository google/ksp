package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class MangledNamesProcessor : AbstractTestProcessor() {
    private val results = mutableListOf<String>()
    override fun toResult() = results

    override fun process(resolver: Resolver) {
        val mangledNames = mutableMapOf<String, String>()
        resolver.getAllFiles().forEach {
            it.accept(MangledNamesVisitor(resolver), mangledNames)
        }
        results.addAll(
            mangledNames.entries.map {(decl, name) ->
                "${decl} -> $name"
            }
        )
    }

    private class MangledNamesVisitor(
        val resolver: Resolver
    ) : KSTopDownVisitor<MutableMap<String, String>, Unit>() {
        override fun defaultHandler(node: KSNode, data: MutableMap<String, String>) {

        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: MutableMap<String, String>) {
            super.visitFunctionDeclaration(function, data)
            data[function.simpleName.asString()] = resolver.getJvmName(function)
        }

        override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: MutableMap<String, String>) {
            super.visitPropertyAccessor(accessor, data)
            val key = if (accessor is KSPropertySetter) {
                "set-${accessor.receiver.simpleName.asString()}"
            } else {
                "get-${accessor.receiver.simpleName.asString()}"
            }
            data[key] = resolver.getJvmName(accessor)
        }
    }
}
