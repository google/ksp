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
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSContextParameter
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Pins how [KSContextParameter.type] is built.
 *
 * `KSContextParameterImpl.type` has two branches: a cheap one that wraps the PSI type reference
 * directly, and a fallback that resolves the type through the Analysis API. The two are *not*
 * observationally equivalent, so which one runs is user visible: the PSI branch reports the
 * location of the type reference itself, whereas the fallback has no location of its own and
 * borrows its parent's.
 *
 * A typealias is exercised too, but note that it does **not** discriminate between the branches:
 * `KSTypeImpl.declaration` is `type.abbreviation?.toDeclaration() ?: type.toDeclaration()`, so the
 * alias is recovered downstream either way. It is pinned here purely to guard that behavior.
 */
class ContextParameterTypeProcessor(val packageNames: List<String>, override val enableNewFeatures: Boolean) :
    AbstractTestProcessor() {

    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val sourceDeclarations = resolver.getAllFiles().flatMap { file -> file.declarations }
        val libraryDeclarations = packageNames.flatMap(resolver::getDeclarationsFromPackage)
        (sourceDeclarations + libraryDeclarations)
            .flatMap { declaration -> contextParametersIn(declaration) }
            .sortedBy { contextParameter -> contextParameter.fqn }
            .forEach { contextParameter ->
                val type = contextParameter.type
                val typeParent = when {
                    type.parent == null -> "null"
                    type.parent === contextParameter -> "self"
                    type.parent === contextParameter.parent -> "owner"
                    else -> "other"
                }
                results.add(
                    "${contextParameter.fqn} " +
                        "type=${type.resolve().declaration.simpleName.asString()} " +
                        "typeParent=$typeParent " +
                        "typeOrigin=${type.origin} " +
                        "typeLine=${type.line} " +
                        "paramLine=${contextParameter.line} " +
                        "ownerLine=${contextParameter.parent?.line}"
                )
            }
        return emptyList()
    }

    private fun contextParametersIn(declaration: KSDeclaration): List<KSContextParameter> =
        when (declaration) {
            is KSFunctionDeclaration -> declaration.contextParameters
            is KSPropertyDeclaration -> declaration.contextParameters
            is KSClassDeclaration ->
                declaration.declarations.flatMap(::contextParametersIn).toList()

            else -> emptyList()
        }

    private val KSNode.line: Int?
        get() = (location as? FileLocation)?.lineNumber

    private val KSNode.fqn: String
        get() = generateSequence(this) { it.parent }
            .takeWhile { it !is KSFile }
            .toList()
            .asReversed()
            .joinToString(separator = ".")
}
