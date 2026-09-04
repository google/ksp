/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

class MultiRoundPackageDeclarationProcessor(
    val packageNames: List<String>,
    override val enableNewFeatures: Boolean
) : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    private lateinit var env: SymbolProcessorEnvironment
    private var round = 0

    override fun toResult(): List<String> {
        return results
    }

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        super.create(environment)
        env = environment
        return this
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        results.add("Round $round:")
        packageNames.forEach { pkgName ->
            val decls = resolver.getDeclarationsFromPackage(pkgName)
                .map { it.qualifiedName?.asString() ?: it.simpleName.asString() }
                .sorted()
                .toList()
            results.add("$pkgName: $decls")
        }

        if (round == 0) {
            generateDummyFileToAdvanceToNextRound()
        }

        round++
        return emptyList()
    }

    private fun generateDummyFileToAdvanceToNextRound() {
        val dependencies = Dependencies(aggregating = true, sources = arrayOf())
        env.codeGenerator.createNewFile(dependencies, "com.example.generated", "GeneratedClass", "kt").use {
            it.write("package com.example.generated\n\nclass GeneratedClass\n".toByteArray())
        }
    }
}
