/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.processor

import com.google.devtools.kotlin.symbol.processing.processing.Resolver
import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.visitor.KSTopDownVisitor

open class TypeComparisonProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val typeCollector = TypeCollector()
    val types = mutableSetOf<KSType>()

    override fun process(resolver: Resolver) {
        val files = resolver.getAllFiles()
        val ignoredNames = mutableSetOf<String>()

        files.forEach {
            it.accept(typeCollector, types)
            it.annotations.forEach {
                if (it.shortName.asString() == "Suppress") {
                    it.arguments.forEach {
                        (it.value as List<String>).forEach {
                            ignoredNames.add(it)
                        }
                    }
                }
            }
        }

        val sortedTypes = types.filterNot { it.declaration.simpleName.asString() in ignoredNames }.sortedBy { it.toString() }

        for (i in sortedTypes) {
            for (j in sortedTypes) {
                results.add("$i ?= $j : ${i.isAssignableFrom(j)}")
            }
        }
    }

    override fun toResult(): List<String> {
        return results
    }

}

class TypeCollectorNoAccessor : TypeCollector() {
    override fun visitPropertyGetter(getter: KSPropertyGetter, data: MutableCollection<KSType>) {
    }

    override fun visitPropertySetter(setter: KSPropertySetter, data: MutableCollection<KSType>) {
    }
}

open class TypeCollector : KSTopDownVisitor<MutableCollection<KSType>, Unit>() {
    override fun defaultHandler(node: KSNode, data: MutableCollection<KSType>) = Unit

    override fun visitTypeReference(typeReference: KSTypeReference, data: MutableCollection<KSType>) {
        super.visitTypeReference(typeReference, data)
        typeReference.resolve()?.let { data.add(it) }
    }
}
