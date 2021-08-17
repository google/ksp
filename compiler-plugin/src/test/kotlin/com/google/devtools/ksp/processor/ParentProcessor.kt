package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class ParentProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val collector = AllSymbolProcessor()
        val nodes = mutableSetOf<KSNode>()
        resolver.getAllFiles().forEach { it.accept(collector, nodes) }
        nodes.forEach {
            result.add("parent of $it: ${it.parent}")
        }
        return emptyList()
    }

    class AllSymbolProcessor : KSTopDownVisitor<MutableSet<KSNode>, Unit>() {
        override fun defaultHandler(node: KSNode, data: MutableSet<KSNode>) {
            data.add(node)
        }
    }
}
