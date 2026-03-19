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

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.impl.symbol.kotlin.Restorable
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile

/**
 * Represents a strategy for resolving annotated symbols.
 */
interface AnnotationResolutionStrategy {

    /**
     * Files that are new in the current round of processing.
     */
    val newKSFiles: List<KSFile>

    /**
     * Symbols that were deferred by symbol processors in the previous round.
     */
    val deferredSymbols: Map<SymbolProcessor, List<Restorable>>

    /**
     * Get all symbols with specified annotation in the current compilation unit.
     * Note that in multiple round processing, only symbols from deferred symbols of last round and symbols from newly generated files will be returned in this function.
     *
     * @param annotationName is the fully qualified name of the fully expanded type (i.e. no type alias); using '.' as separator.
     * @param inDepth whether to check symbols in depth, i.e. check symbols from local declarations. Operation can be expensive if true.
     * @return Elements annotated with the specified annotation.
     *
     */
    fun getSymbolsWithAnnotation(annotationName: String, inDepth: Boolean): Sequence<KSAnnotated>
}
