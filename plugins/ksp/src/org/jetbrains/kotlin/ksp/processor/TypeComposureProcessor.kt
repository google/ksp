/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.visitor.KSTopDownVisitor

open class TypeComposureProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver) {
        val files = resolver.getAllFiles()
        val classes = mutableSetOf<KSClassDeclaration>()
        val references = mutableSetOf<KSTypeReference>()

        files.forEach {
            it.accept(object: KSTopDownVisitor<Unit, Unit>(){
                override fun defaultHandler(node: KSNode, data: Unit) = Unit

                override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
                    super.visitClassDeclaration(classDeclaration, Unit)
                    classes.add(classDeclaration)
                }

                override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
                    super.visitTypeReference(typeReference, data)
                    references.add(typeReference)
                }
            }, Unit)
        }

        val composed = mutableSetOf<KSType>()
        val types = references.filter { it.resolve()!!.arguments.size == 1 }.map { it.resolve()!! }
        val refs0Arg = references.filter { it.resolve()!!.arguments.size == 0 }

        for (c in classes) {
            for (ref in refs0Arg) {
                composed.add(c.asType(listOf(resolver.getTypeArgument(ref, Variance.INVARIANT))))
                composed.add(c.asType(listOf(resolver.getTypeArgument(ref, Variance.COVARIANT))))
                composed.add(c.asType(listOf(resolver.getTypeArgument(ref, Variance.CONTRAVARIANT))))
                composed.add(c.asType(listOf(resolver.getTypeArgument(ref, Variance.STAR))))
            }
        }

        for (t in types) {
            for (ref in refs0Arg) {
                composed.add(t.replace(listOf(resolver.getTypeArgument(ref, Variance.INVARIANT))))
                composed.add(t.replace(listOf(resolver.getTypeArgument(ref, Variance.COVARIANT))))
                composed.add(t.replace(listOf(resolver.getTypeArgument(ref, Variance.CONTRAVARIANT))))
                composed.add(t.starProjection())
            }
        }

        val sorted = composed.sortedBy { it.toString() }
        for (i in sorted) {
            for (j in sorted) {
                results.add("$i ?= $j : ${i.isAssignableFrom(j)}")
            }
        }
    }

    override fun toResult(): List<String> {
        return results
    }
}
