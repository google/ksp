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

open class TypeAliasProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val typeCollector = TypeCollectorNoAccessor()
    val types = mutableListOf<KSType>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val files = resolver.getNewFiles()

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
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }

}

