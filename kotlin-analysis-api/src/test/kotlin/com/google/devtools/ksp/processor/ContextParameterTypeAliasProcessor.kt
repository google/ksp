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
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference

/**
 * Pins that a typealias written as a context parameter type resolves to the same [KSType] shape as
 * the same typealias written as a value parameter type, for both source and library declarations.
 *
 * `KSType.declaration` prefers the type's abbreviation, but `KSType.arguments` reads the type
 * arguments of whichever type it was built from. So if the expanded type is passed instead of the
 * abbreviated one, a type written as `StrMap<Int>` (where `typealias StrMap<V> = Map<String, V>`)
 * reports `declaration=StrMap` alongside the *expanded* arguments `[String, Int]`, and no longer
 * matches the same type written elsewhere.
 *
 * For each function, the context parameter and value parameter types are reported side by side,
 * together with whether the two resolved types compare equal.
 */
class ContextParameterTypeAliasProcessor(val packageNames: List<String>, override val enableNewFeatures: Boolean) :
    AbstractTestProcessor() {

    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val sourceFunctions = resolver.getAllFiles().flatMap { it.declarations }
        val libraryFunctions = packageNames.flatMap(resolver::getDeclarationsFromPackage)
        (sourceFunctions + libraryFunctions)
            .filterIsInstance<KSFunctionDeclaration>()
            .filter { it.contextParameters.isNotEmpty() }
            .sortedBy { it.simpleName.asString() }
            .forEach { function ->
                val contextType = function.contextParameters.single().type
                val valueType = function.parameters.single().type
                results.add("${function.simpleName.asString()} context: ${contextType.describe()}")
                results.add("${function.simpleName.asString()} value: ${valueType.describe()}")
                results.add(
                    "${function.simpleName.asString()} equal: ${contextType.resolve() == valueType.resolve()}"
                )
            }
        return emptyList()
    }

    private fun KSTypeReference.describe(): String = resolve().describe()

    private fun KSType.describe(): String =
        "${declaration.simpleName.asString()}${arguments.map { it.type?.describe() }}"
}
