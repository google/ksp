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

import com.google.devtools.ksp.processing.KSPSuggestedFix
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class SuggestedFixProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results.toList()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().forEach { file ->
            file.declarations.forEach { decl ->
                if (decl is KSClassDeclaration && decl.simpleName.asString() == "DeprecatedClass") {
                    val fixWithDesc = KSPSuggestedFix("NewClass", "Replace with NewClass")
                    env.logger.warn("Class is deprecated", decl, fixWithDesc)
                    results.add("warn:${fixWithDesc.replacementText}:${fixWithDesc.description}")

                    val fixWithoutDesc = KSPSuggestedFix("AlternativeClass")
                    env.logger.error("Use AlternativeClass instead", decl, fixWithoutDesc)
                    results.add("error:${fixWithoutDesc.replacementText}:${fixWithoutDesc.description}")
                }
            }
        }
        return emptyList()
    }

    lateinit var env: SymbolProcessorEnvironment

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return this
    }
}
