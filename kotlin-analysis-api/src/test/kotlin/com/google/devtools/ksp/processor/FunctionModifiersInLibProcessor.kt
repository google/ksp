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

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * Prints the modifiers of the functions declared in [lib.Impl], which lives in a dependency, so
 * that the compiler symbol based `modifiers` implementation is exercised.
 */
class FunctionModifiersInLibProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val impl = resolver.getClassDeclarationByName("lib.Impl")
            ?: error("lib.Impl not found")
        impl.getDeclaredFunctions()
            .filterNot { it.isConstructor() }
            .map { function ->
                val modifiers = function.modifiers.map { it.toString() }.sorted().joinToString(", ")
                "${function.simpleName.asString()} : $modifiers"
            }
            .sorted()
            .forEach(results::add)
        return emptyList()
    }
}
