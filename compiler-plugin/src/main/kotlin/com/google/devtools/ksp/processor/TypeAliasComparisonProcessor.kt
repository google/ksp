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
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.visitor.KSTopDownVisitor

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
