/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

open class TypeComposureProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val files = resolver.getNewFiles()
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
        val types = references.filter { it.resolve()!!.arguments.toList().size == 1 }.map { it.resolve()!! }
        val refs0Arg = references.filter { it.resolve()!!.arguments.toList().size == 0 }

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
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
