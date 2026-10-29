package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class ReachableAnnotationsFromGivenClasses(
    val classNames: List<String>,
    override val enableNewFeatures: Boolean
) : AbstractTestProcessor() {

    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val seenNames = mutableSetOf<String>()
        val seenNodes = mutableSetOf<KSNode>()
        val visitor = DeclarationVisitor(seenNodes)
        classNames.forEach { className ->
            resolver
                .getClassDeclarationByName(className)
                ?.accept(visitor, seenNames)
        }
        results.addAll(seenNames)
        return emptyList()
    }

    inner class DeclarationVisitor(val seen: MutableSet<KSNode>) :
        KSTopDownVisitor<MutableSet<String>, Unit>(enableNewFeatures) {
        override fun defaultHandler(
            node: KSNode,
            data: MutableSet<String>
        ) {
            seen.add(node)
        }

        override fun visitDeclaration(declaration: KSDeclaration, data: MutableSet<String>) {
            data.add(declaration.qualifiedName?.asString() ?: declaration.simpleName.asString())
            super.visitDeclaration(declaration, data)
        }

        override fun visitAnnotation(annotation: KSAnnotation, data: MutableSet<String>) {
            super.visitAnnotation(annotation, data)
            val declaration = annotation.annotationType.resolve().declaration
            if (!seen.contains(declaration)) {
                declaration.accept(this, data)
            }
        }
    }
}
