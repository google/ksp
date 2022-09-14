package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class ParameterTypeProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result.sorted()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().forEach {
            it.accept(
                object : KSTopDownVisitor<Unit, Unit>() {
                    override fun defaultHandler(node: KSNode, data: Unit) {
                    }

                    override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit) {
                        result.add("${valueParameter.name?.asString()}: ${valueParameter.type.resolve()}")
                    }
                },
                Unit
            )
        }
        return emptyList()
    }
}
