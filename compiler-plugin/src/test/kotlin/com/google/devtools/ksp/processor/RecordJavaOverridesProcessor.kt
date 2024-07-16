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
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*

class RecordJavaOverridesProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        val finalResult = mutableListOf(results[0])
        finalResult.addAll(results.subList(1, results.size).sorted())
        return finalResult
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        var A_f1: KSFunctionDeclaration? = null
        var A_f2: KSFunctionDeclaration? = null
        var C_f1: KSFunctionDeclaration? = null
        resolver.getAllFiles().forEach {
            if (it.fileName == "A.java") {
                val c = it.declarations.single {
                    it is KSClassDeclaration && it.simpleName.asString() == "A"
                } as KSClassDeclaration
                A_f1 = c.declarations.single {
                    it is KSFunctionDeclaration && it.simpleName.asString() == "f1"
                } as KSFunctionDeclaration
                A_f2 = c.declarations.single {
                    it is KSFunctionDeclaration && it.simpleName.asString() == "f2"
                } as KSFunctionDeclaration
            } else if (it.fileName == "C.java") {
                val c = it.declarations.single {
                    it is KSClassDeclaration && it.simpleName.asString() == "C"
                } as KSClassDeclaration
                C_f1 = c.declarations.single {
                    it is KSFunctionDeclaration && it.simpleName.asString() == "f1"
                } as KSFunctionDeclaration
            }
        }

        resolver.overrides(A_f1!!, C_f1!!)
        A_f2!!.findOverridee()

        val m = when (resolver) {
            is ResolverImpl -> resolver.incrementalContext.dumpLookupRecords().toSortedMap()
            else -> throw IllegalStateException("Unknown Resolver: $resolver")
        }
        m.forEach { symbol, files ->
            files.filter { it.endsWith(".java") }.sorted().forEach {
                results.add("$symbol: $it")
            }
        }
        return emptyList()
    }
}
