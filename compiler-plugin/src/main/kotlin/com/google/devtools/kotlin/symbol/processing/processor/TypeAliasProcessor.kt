/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.processor

import com.google.devtools.kotlin.symbol.processing.processing.Resolver
import com.google.devtools.kotlin.symbol.processing.symbol.*

open class TypeAliasProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val typeCollector = TypeCollectorNoAccessor()
    val types = mutableListOf<KSType>()

    override fun process(resolver: Resolver) {
        val files = resolver.getAllFiles()

        files.forEach {
            it.accept(typeCollector, types)
        }

        val sortedTypes = types.sortedBy { it.declaration.simpleName.asString() }

        for (i in sortedTypes) {
            val r = mutableListOf<String>()
            var a: KSType? = i
            do {
                r.add(a!!.declaration.simpleName.asString())
                a = (a.declaration as? KSTypeAlias)?.type?.resolve()
            } while (a != null)
            results.add(r.joinToString(" = "))
        }

        for (i in types) {
            for (j in types) {
                assert(i == j)
            }
        }
    }

    override fun toResult(): List<String> {
        return results
    }

}

