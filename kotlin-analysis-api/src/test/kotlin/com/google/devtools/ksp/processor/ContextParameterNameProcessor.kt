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
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSContextParameter
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Pins [KSContextParameter.name] and `toString()`, in particular for unnamed context parameters,
 * `context(_: T)`.
 *
 * Each context parameter is identified by its owner's simple name and its index, rather than by a
 * `toString()`-based qualified name, because `toString()` of the context parameter is itself under
 * test and must not leak into the identifier.
 */
class ContextParameterNameProcessor(val packageNames: List<String>, override val enableNewFeatures: Boolean) :
    AbstractTestProcessor() {

    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results.sorted()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val sourceDeclarations = resolver.getAllFiles().flatMap { file -> file.declarations }
        val libraryDeclarations = packageNames.flatMap(resolver::getDeclarationsFromPackage)
        (sourceDeclarations + libraryDeclarations)
            .flatMap { declaration -> ownersIn(declaration) }
            .forEach { (owner, contextParameters) ->
                contextParameters.forEachIndexed { index, contextParameter ->
                    results.add(
                        "${owner.simpleName.asString()}[$index] " +
                            "name=${contextParameter.name?.asString() ?: "null"} " +
                            "toString=$contextParameter"
                    )
                }
            }
        return emptyList()
    }

    private fun ownersIn(declaration: KSDeclaration): List<Pair<KSDeclaration, List<KSContextParameter>>> =
        when (declaration) {
            is KSFunctionDeclaration -> listOf(declaration to declaration.contextParameters)
            is KSPropertyDeclaration -> listOf(declaration to declaration.contextParameters)
            is KSClassDeclaration -> declaration.declarations.flatMap(::ownersIn).toList()
            else -> emptyList()
        }
}
