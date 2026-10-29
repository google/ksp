/*
 * Copyright 2026 Google LLC
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

package com.google.devtools.ksp.common

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import java.io.File

/**
 * Incremental-processing operations used by symbol processing and resolution.
 *
 * Cache storage, lookup trackers, and logging implementation details are deliberately excluded,
 * so callers do not depend on a particular incremental-context implementation.
 */
interface IncrementalContext {
    fun calcDirtyFiles(ksFiles: List<KSFile>): Collection<KSFile>

    fun registerGeneratedFiles(newFiles: Collection<KSFile>)

    fun <T> closeFilesOnException(f: () -> T): T

    fun closeFiles()

    fun updateCachesAndOutputs(
        dirtyFiles: Collection<KSFile>,
        outputs: Set<File>,
        sourceToOutputs: Map<File, Set<File>>,
    )

    fun recordLookup(psiFile: PsiJavaFile, fqn: String)

    fun recordLookup(type: KaType, context: KSNode?)

    fun recordClassReferenceLookup(type: KaType, declaration: KSDeclaration)

    fun recordClassReferenceLookup(fqn: String, declaration: KSDeclaration)

    fun recordLookupForPropertyOrMethod(declaration: KSDeclaration)

    fun recordLookupForGetAll(supers: List<KaType>, predicate: (KaSymbol) -> Boolean)

    fun recordLookupWithSupertypes(
        ktType: KaType,
        visited: MutableSet<KaType>,
        extra: (KaType, PsiJavaFile) -> Unit,
    )

    fun recordGetSealedSubclasses(classDeclaration: KSClassDeclaration)

    /** Lookup traces used by existing debug logging and incremental-processing tests. */
    fun dumpLookupRecords(): Map<String, List<String>>
}
