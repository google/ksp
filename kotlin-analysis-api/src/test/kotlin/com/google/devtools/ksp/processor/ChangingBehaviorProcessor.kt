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
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.buildApiFeatures
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.Modifier

class ChangingBehaviorProcessor(val annotationNames: List<String>, override var apiFeatures: ApiFeatures) :
    AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Initialize api features to disable everything
        apiFeatures = buildApiFeatures {
            enableBackingFields = false
            enableContextParameters = false
        }

        // Call Resolver functions before changing api features
        results.add("apiFeatures = $apiFeatures")
        results.addAll(queryChangePoints(resolver))

        // Unsafely update apiFeatures
        apiFeatures = buildApiFeatures {
            enableBackingFields = true
            enableContextParameters = true
        }

        // Call same functions as before in Resolver to ensure it returns new symbols
        results.add("apiFeatures = $apiFeatures")
        results.addAll(queryChangePoints(resolver))

        return emptyList()
    }

    /**
     * Helper function that queries the resolver instance
     * and symbols where changes can occur.
     * This helper function ensures the call site remains
     * exactly the same, so the only change is the new [apiFeatures] value.
     *
     * Importantly, it only uses local mutability, such that the function is pure
     * given a resolver configuration. In other words, as it only depends on the
     * resolver, the only possible change is in the resolver.
     */
    @OptIn(KspExperimental::class)
    private fun queryChangePoints(resolver: Resolver): List<String> {
        val seenSymbols = mutableSetOf<KSAnnotated>()
        val localResults = mutableListOf<String>()
        annotationNames.forEach { annotationName ->
            val syms = resolver.getSymbolsWithAnnotation(annotationName).also(seenSymbols::addAll)
            localResults.add(
                "Resolver.getSymbolsWithAnnotation(\"$annotationName\") = ${
                    renderSymbols(
                        syms,
                        annotationName
                    )
                }"
            )
        }
        seenSymbols.forEach { sym ->
            when (sym) {
                is KSDeclaration -> {
                    val name = sym.qualifiedName?.asString() ?: sym.simpleName.asString()
                    val annotations = renderAnnotations(sym.annotations)
                    val modifiers = renderModifiers(resolver.effectiveJavaModifiers(sym))
                    localResults.add("$name.annotations = $annotations")
                    localResults.add("$name.modifiers = $modifiers")
                }

                else -> error("Unexpected annotated symbol $sym")
            }
        }
        return localResults
    }

    private fun renderSymbols(
        symbols: Sequence<KSAnnotated>,
        annotationName: String
    ): String = symbols.joinToString(", ") { sym ->
        when (sym) {
            is KSDeclaration -> sym.qualifiedName?.asString() ?: sym.simpleName.asString()

            else -> error("Unexpected annotated symbol $sym annotated with $annotationName")
        }
    }

    private fun renderModifiers(modifiers: Set<Modifier>): String =
        modifiers.toList().map { it.toString() }.sorted().joinToString(", ")

    private fun renderAnnotations(annotations: Sequence<KSAnnotation>): String =
        annotations.map { it.shortName.asString() }.toList().sorted().joinToString(", ")
}
