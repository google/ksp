package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class AbstractFunctionsProcessor : AbstractTestProcessor() {
    private val visitor = Visitor()

    override fun toResult(): List<String> {
        return visitor.abstractFunctionNames.sorted()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().forEach { it.accept(visitor, Unit) }
        return emptyList()
    }

    private class Visitor : KSTopDownVisitor<Unit, Unit>() {
        val abstractFunctionNames = arrayListOf<String>()

        override fun defaultHandler(node: KSNode, data: Unit) {
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            if (function.isAbstract) {
                abstractFunctionNames += function.simpleName.asString()
            }
        }
    }
}
