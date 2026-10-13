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

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSContextParameter
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Checks that a [KSContextParameter] is *canonical*: for one context parameter in the source there
 * must be exactly one [KSContextParameter] instance, no matter which API produced it.
 *
 * KSP relies on this invariant everywhere it deduplicates symbols with a `Set` or `distinct()`
 * (most notably when merging freshly discovered symbols with symbols restored from a previous
 * round), because the `KS*` implementations inherit identity `equals`/`hashCode` from `Any`.
 *
 * A context parameter can be reached through three independent producers:
 *  1. the declaration tree, via `KSFunctionDeclaration.contextParameters` /
 *     `KSPropertyDeclaration.contextParameters`;
 *  2. `Resolver.getSymbolsWithAnnotation`, which under the experimental PSI resolution strategy
 *     goes through `KaContextParameterSymbol.toKSContextParameter` instead of the declaration tree;
 *  3. deferral, i.e. `Deferrable.defer()` in round N followed by `Restorable.restore()` in
 *     round N + 1.
 *
 * The Analysis API hands out a *fresh* `KaContextParameterSymbol` for each of these, so the
 * producers only converge on a single `KSContextParameter` if the `KSContextParameterImpl` cache is
 * keyed structurally. This processor therefore compares, per round, the instances reported by
 * `getSymbolsWithAnnotation` against the instances reachable from the declaration tree.
 *
 * `distinctInstances` is the size of the de-duplicated set of all instances seen for one context
 * parameter; it is `1` when the invariant holds and `2` when the producers disagree.
 */
class ContextParameterIdentityProcessor(
    val annotationNames: List<String>,
    override val enableNewFeatures: Boolean
) : AbstractTestProcessor() {

    private val results = mutableListOf<String>()
    private var round = 0
    private lateinit var env: SymbolProcessorEnvironment

    override fun toResult(): List<String> = results

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return super.create(environment)
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val nonAnnotatedContextParameters = mutableSetOf<KSContextParameter>()

        // Producer 1: the declaration tree. In round 1 the files are restored from deferred
        // pointers, so this exercises a different code path than in round 0.
        val declaredByFqn: Map<String, List<KSContextParameter>> = resolver.getAllFiles()
            .flatMap { file -> file.declarations }
            .flatMap { declaration -> contextParametersIn(declaration) }
            .toList()
            .also { params ->
                // Also add context parameters without annotations to a set which will be deferred along with the reported ones.
                nonAnnotatedContextParameters.addAll(params.filter {
                    it.annotations.toList().isEmpty()
                }.also(::println))
            }
            .groupBy { it.fqn }

        // Producers 2 and 3: getSymbolsWithAnnotation, which in round 1 serves restored symbols.
        val reported: List<KSContextParameter> = annotationNames
            .flatMap { annotationName -> resolver.getSymbolsWithAnnotation(annotationName).toList() }
            .filterIsInstance<KSContextParameter>()

        results.add("round$round reportedContextParameters=${reported.size}")
        reported.groupBy { it.fqn }.toSortedMap().forEach { (fqn, reportedInstances) ->
            val declaredInstances = declaredByFqn[fqn].orEmpty()
            val canonical = declaredInstances.isNotEmpty() &&
                reportedInstances.all { reported -> declaredInstances.any { it === reported } }
            val distinctInstances = (reportedInstances + declaredInstances).distinct().size
            results.add("round$round $fqn canonical=$canonical distinctInstances=$distinctInstances")
        }

        results.add("round$round non-annotated context parameters ${nonAnnotatedContextParameters.size}: ${nonAnnotatedContextParameters.map { it.fqn }}")
        if (round == 0) {
            round++
            // A generated file is required for KSP to run another round, which is what lets us
            // observe the symbols coming back through Restorable.restore().
            generateFileToTriggerNextRound()
            return reported + nonAnnotatedContextParameters
        }
        return emptyList()
    }

    private fun generateFileToTriggerNextRound() {
        env.codeGenerator.createNewFile(Dependencies(aggregating = false), "", "Unused", "kt").use {
            it.write("class Unused\n".toByteArray())
        }
    }

    private fun contextParametersIn(declaration: KSDeclaration): List<KSContextParameter> =
        when (declaration) {
            is KSFunctionDeclaration -> declaration.contextParameters
            is KSPropertyDeclaration -> declaration.contextParameters
            is KSClassDeclaration ->
                declaration.declarations.flatMap { contextParametersIn(it) }.toList()

            else -> emptyList()
        }

    private val KSNode.fqn: String
        get() = generateSequence(this) { it.parent }
            .takeWhile { it !is KSFile }
            .toList()
            .asReversed()
            .joinToString(separator = ".")
}
