/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSTypeReferenceDescriptorImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSTypeImpl
import org.jetbrains.kotlin.ksp.visitor.KSTopDownVisitor

open class PlatformDeclarationProcessor: AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val collector = EverythingVisitor()
    val declarations = mutableListOf<KSDeclaration>()

    override fun process(resolver: Resolver) {
        val files = resolver.getAllFiles()

        files.forEach {
            it.accept(collector, declarations)
        }

        declarations.sortedBy { "${it.containingFile?.fileName} : ${it.qualifiedName?.asString()}" }.forEach {
            val r = mutableListOf<Any?>()
            r.add(it.containingFile?.fileName)
            r.add(it.qualifiedName?.asString())
            r.add(it.isActual)
            r.add(it.isExpect)
            r.add(it.findActuals().joinToString(", ", "[", "]") { it.containingFile?.fileName.toString() })
            r.add(it.findExpects().joinToString(", ", "[", "]") { it.containingFile?.fileName.toString() })
            results.add(r.map { it.toString() }.joinToString(" : "))
        }
    }

    override fun toResult(): List<String> {
        return results
    }

}

class EverythingVisitor : KSTopDownVisitor<MutableList<KSDeclaration>, Unit>() {
    override fun defaultHandler(node: KSNode, data: MutableList<KSDeclaration>) = Unit

    override fun visitDeclaration(declaration: KSDeclaration, data: MutableList<KSDeclaration>) {
        super.visitDeclaration(declaration, data)
        data.add(declaration)
    }
}
