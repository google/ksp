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

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.intellij.util.containers.addIfNotNull

/**
 * This test asserts that [getAllSuperTypes] is stateless/referentially transparent,
 * i.e. reusing the same sequence gives equal output:
 * ```
 * val superTypes = ...getAllSuperTypes()
 * superTypes.terminalOperation() == superTypes.terminalOperation()
 * ```
 */
class AllSuperTypesRepeatableProcessor(val classNames: List<String>, override val enableNewFeatures: Boolean) :
    AbstractTestProcessor() {
    private val results: MutableList<String> = mutableListOf()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        classNames.forEach { className ->
            val superTypes = resolver.getClassDeclarationByName(className)?.getAllSuperTypes() ?: return@forEach
            results.add("Original:")
            superTypes.mapNotNull { it.declaration.qualifiedName?.asString() }.forEach(results::addIfNotNull)

            results.add("Copy:")
            superTypes.mapNotNull { it.declaration.qualifiedName?.asString() }.forEach(results::addIfNotNull)

            // Apply terminal operation that does not consume the entire sequence.
            superTypes.any { it.declaration.qualifiedName?.asString() == "L03b" }

            results.add("After Partial:")
            superTypes.mapNotNull { it.declaration.qualifiedName?.asString() }.forEach(results::addIfNotNull)
        }
        return emptyList()
    }
}
