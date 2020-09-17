/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.kotlin.KSTypeImpl
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import org.jetbrains.kotlin.types.getAbbreviation

open class FunctionTypeAliasProcessor: AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val typeRefCollector = RefCollector()
    val refs = mutableSetOf<KSTypeReference>()

    override fun process(resolver: Resolver) {
        val files = resolver.getAllFiles()

        files.forEach {
            it.accept(typeRefCollector, refs)
        }

        val types = refs.mapNotNull { it.resolve() }.sortedBy { it.toString() }.distinctBy { it.toString() }

        for (i in types) {
            for (j in types) {
                results.add("$i ?= $j : ${i.isAssignableFrom(j)} / ${i == j}")
            }
        }
    }

    override fun toResult(): List<String> {
        return results
    }

}

open class RefCollector : KSTopDownVisitor<MutableCollection<KSTypeReference>, Unit>() {
    override fun defaultHandler(node: KSNode, data: MutableCollection<KSTypeReference>) = Unit

    override fun visitTypeReference(typeReference: KSTypeReference, data: MutableCollection<KSTypeReference>) {
        super.visitTypeReference(typeReference, data)
        data.add(typeReference)
    }
}
