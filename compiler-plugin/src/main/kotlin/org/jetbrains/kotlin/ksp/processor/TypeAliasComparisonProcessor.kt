/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.visitor.KSTopDownVisitor

open class TypeAliasComparisonProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val typeRefCollector = TypeRefCollector()
    val refs = mutableSetOf<KSTypeReference>()

    override fun process(resolver: Resolver) {
        val files = resolver.getAllFiles()

        files.forEach {
            it.accept(typeRefCollector, refs)
        }

        fun KSType.aliases(): List<KSType> =
            listOf(this) + ((this.declaration as? KSTypeAlias)?.type?.resolve()?.aliases() ?: emptyList())

        val interesting = setOf("Anno", "Bnno")
        val iRefs = refs.filterNot { it.annotations.all { it.shortName.asString() !in interesting } }
        val types = iRefs.map { it.resolve()!! }.flatMap { it.aliases() }

        for (i in types) {
            for (j in types) {
                results.add("$i = $j : ${i.isAssignableFrom(j)}")
            }
        }
    }

    override fun toResult(): List<String> {
        return results
    }

}

open class TypeRefCollector : KSTopDownVisitor<MutableCollection<KSTypeReference>, Unit>() {
    override fun defaultHandler(node: KSNode, data: MutableCollection<KSTypeReference>) = Unit

    override fun visitTypeReference(typeReference: KSTypeReference, data: MutableCollection<KSTypeReference>) {
        super.visitTypeReference(typeReference, data)
        data.add(typeReference)
    }
}
