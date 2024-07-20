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

import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

class RecordJavaSupertypesProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        val finalResult = mutableListOf(results[0])
        finalResult.addAll(results.subList(1, results.size).sorted())
        return finalResult
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val types = mutableSetOf<KSType>()
        resolver.getAllFiles().forEach {
            it.accept(TypeCollectorNoAccessor(), types)
        }
        types.forEach {
            resolver.builtIns.anyType.isAssignableFrom(it)
        }
        val m = when (resolver) {
            is ResolverAAImpl -> resolver.incrementalContext.dumpLookupRecords().toSortedMap()
            else -> throw IllegalStateException("Unknown Resolver: $resolver")
        }
        m.forEach { symbol, files ->
            files.filter { it.endsWith(".java") }.sorted().forEach {
                val fn = it.substringAfterLast("java-sources/")
                results.add("$symbol: $fn")
            }
        }
        return emptyList()
    }
}
