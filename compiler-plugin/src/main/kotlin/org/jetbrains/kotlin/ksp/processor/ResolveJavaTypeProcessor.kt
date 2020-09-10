/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.visitor.KSTopDownVisitor

class ResolveJavaTypeProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val visitor = ResolveJavaTypeVisitor()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver) {
        val symbol = resolver.getClassDeclarationByName(resolver.getKSNameFromString("C"))
        assert(symbol?.origin == Origin.JAVA)
        symbol!!.accept(visitor, Unit)
    }

    inner class ResolveJavaTypeVisitor : KSTopDownVisitor<Unit, Unit>() {
        override fun defaultHandler(node: KSNode, data: Unit) {
        }

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            classDeclaration.declarations.map { it.accept(this, Unit) }
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            function.returnType?.accept(this, Unit)
        }

        override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
            if (typeReference.origin == Origin.JAVA) {
                results.add(typeReference.render())
            }
        }
    }

    fun KSTypeReference.render(): String {
        val sb = StringBuilder(this.resolve()?.declaration?.qualifiedName?.asString() ?: "<ERROR>")
        if (this.resolve()?.arguments?.isNotEmpty() == true) {
            sb.append("<${this.resolve()!!.arguments.map {
                when (it.variance) {
                    Variance.STAR -> "*"
                    Variance.INVARIANT -> ""
                    Variance.CONTRAVARIANT -> "in "
                    Variance.COVARIANT -> "out "
                } + it.type?.render() 
            }.joinToString(", ")}>")
        }
        if (this.resolve()?.nullability != Nullability.NOT_NULL) {
            sb.append("?")
        }
        return sb.toString()
    }
}