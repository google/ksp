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

package com.google.devtools.ksp.common.impl

import com.google.devtools.ksp.common.visitor.CollectAnnotatedSymbolsVisitor
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeImpl
import com.google.devtools.ksp.impl.symbol.kotlin.Restorable
import com.google.devtools.ksp.impl.symbol.kotlin.fullyExpand
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import org.jetbrains.kotlin.analysis.api.types.symbol

/**
 * An [AnnotationResolutionStrategy] that uses the Kotlin Analysis API to resolve all symbols.
 */
class AAResolutionStrategy(
    override val newKSFiles: List<KSFile>,
    override val deferredSymbols: Map<SymbolProcessor, List<Restorable>>
) : AnnotationResolutionStrategy {

    override fun getSymbolsWithAnnotation(annotationName: String, inDepth: Boolean): Sequence<KSAnnotated> =
        if (inDepth)
            annotationToSymbolsWithLocalsCache[annotationName]?.asSequence() ?: emptySequence()
        else
            annotationToSymbolsCache[annotationName]?.asSequence() ?: emptySequence()

    private val annotationToSymbolsCache: Map<String, Collection<KSAnnotated>> by lazy {
        mapAnnotatedSymbols(false)
    }

    private val annotationToSymbolsWithLocalsCache: Map<String, Collection<KSAnnotated>> by lazy {
        mapAnnotatedSymbols(true)
    }

    private fun collectAnnotatedSymbols(inDepth: Boolean): Collection<KSAnnotated> {
        val visitor = CollectAnnotatedSymbolsVisitor(inDepth)

        for (file in newKSFiles) {
            file.accept(visitor, Unit)
        }

        return visitor.symbols
    }

    private fun mapAnnotatedSymbols(inDepth: Boolean): Map<String, Collection<KSAnnotated>> {
        val newSymbols = collectAnnotatedSymbols(inDepth)
        val withDeferred = newSymbols + deferredSymbolsRestored
        return mutableMapOf<String, MutableCollection<KSAnnotated>>().apply {
            withDeferred.forEach { annotated ->
                for (annotation in annotated.annotations) {
                    val kaType = (annotation.annotationType.resolve() as? KSTypeImpl)?.type ?: continue
                    val annotationFqN = kaType.fullyExpand().symbol?.classId?.asFqNameString() ?: continue
                    getOrPut(annotationFqN, ::mutableSetOf).add(annotated)
                }
            }
        }
    }

    private val deferredSymbolsRestored: Set<KSAnnotated> by lazy {
        deferredSymbols.values.flatten().mapNotNull { it.restore() }.toSet()
    }
}
