package com.google.devtools.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class LateinitPropertiesProcessor : AbstractTestProcessor() {
    private val visitor = Visitor()

    override fun toResult(): List<String> {
        return visitor.lateinitPropertiesNames.sorted()
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getDeclarationsFromPackage("test.compiled").forEach {
            it.accept(visitor, Unit)
        }
        resolver.getNewFiles().forEach { it.accept(visitor, Unit) }
        return emptyList()
    }

    private class Visitor : KSTopDownVisitor<Unit, Unit>() {
        val lateinitPropertiesNames = arrayListOf<String>()

        override fun defaultHandler(node: KSNode, data: Unit) {
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            if (Modifier.LATEINIT in property.modifiers) {
                lateinitPropertiesNames += property.simpleName.asString()
            }
        }
    }
}
