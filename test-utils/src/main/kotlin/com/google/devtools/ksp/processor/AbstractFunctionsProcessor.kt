package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class AbstractFunctionsProcessor(override val enableNewFeatures: Boolean) : AbstractTestProcessor() {
    private val visitor = Visitor(enableNewFeatures)

    override fun toResult(): List<String> {
        return visitor.abstractFunctionNames.sorted()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().forEach { it.accept(visitor, Unit) }
        return emptyList()
    }

    private class Visitor(enableNewFeatures: Boolean) : KSTopDownVisitor<Unit, Unit>(enableNewFeatures) {
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
