package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.impl.BaseVisitor
import com.google.devtools.ksp.symbol.*

class MultiModuleTestProcessor : AbstractTestProcessor() {
    private val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver) {
        val target = resolver.getClassDeclarationByName("TestTarget")
        val classes = mutableSetOf<KSClassDeclaration>()
        val classCollector = object : BaseVisitor() {
            override fun visitClassDeclaration(type: KSClassDeclaration, data: Unit) {
                if (classes.add(type)) {
                    super.visitClassDeclaration(type, data)
                }
            }

            override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
                (property.type.resolve().declaration as? KSClassDeclaration)?.accept(this, Unit)
            }
        }
        target?.accept(classCollector, Unit)
        results.addAll(classes.map { it.toSignature() }.sorted())
    }

    private fun KSClassDeclaration.toSignature(): String {
        val id = qualifiedName?.asString() ?: "no-qual-name:($this)"
        return "$id[${origin.name}]"
    }
}