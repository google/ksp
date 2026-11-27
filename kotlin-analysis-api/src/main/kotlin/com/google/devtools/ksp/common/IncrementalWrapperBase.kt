/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.symbol.KSDeclaration
import com.intellij.util.containers.MultiMap
import java.io.File

/**
 * Canonical scope identifier used by Kotlin's incremental lookup storage for declarations in the root (unnamed)
 * package.
 */
internal const val ROOT_PACKAGE_SCOPE: String = "<anonymous>"

/**
 * Normalizes an empty package/classifier scope to [ROOT_PACKAGE_SCOPE] so all lookup trackers and symbol collectors
 * agree on the root-package scope key.
 */
internal fun String.normalizeLookupScope(): String = ifEmpty { ROOT_PACKAGE_SCOPE }

/**
 * Extracts a [LookupSymbolWrapper] from this [KSDeclaration] by stripping [KSDeclaration.simpleName] from the end of
 * [KSDeclaration.qualifiedName], preserving dots inside backticked simple names and normalizing root-package scopes to
 * [ROOT_PACKAGE_SCOPE]. Returns `null` when [KSDeclaration.qualifiedName] is `null` (e.g., local or anonymous symbols).
 */
internal fun KSDeclaration.toLookupSymbolWrapper(): LookupSymbolWrapper? {
    val qName = qualifiedName?.asString() ?: return null
    val name = simpleName.asString()
    val scope = when {
        qName == name -> ROOT_PACKAGE_SCOPE
        qName.length > name.length && qName.endsWith(name) && qName[qName.length - name.length - 1] == '.' ->
            qName.substring(0, qName.length - name.length - 1).normalizeLookupScope()
        else -> qName.removeSuffix(name).removeSuffix(".").normalizeLookupScope()
    }
    return LookupSymbolWrapper(name, scope)
}

/**
 * Formats this [LookupSymbolWrapper] as a Graphviz `.dot` vertex name, omitting [ROOT_PACKAGE_SCOPE] (and legacy empty
 * scopes) while preserving any leading dots that belong to [LookupSymbolWrapper.name].
 */
internal fun LookupSymbolWrapper.toGraphVertexName(): String =
    if (scope.isEmpty() || scope == ROOT_PACKAGE_SCOPE) name else "$scope.$name"

interface LookupTrackerWrapper {
    fun record(
        filePath: String,
        scopeFqName: String,
        name: String
    )

    val lookups: MultiMap<LookupSymbolWrapper, String>
}

data class LookupSymbolWrapper(val name: String, val scope: String) : Comparable<LookupSymbolWrapper> {
    override fun compareTo(other: LookupSymbolWrapper): Int {
        val scopeCompare = scope.compareTo(other.scope)
        if (scopeCompare != 0) return scopeCompare

        return name.compareTo(other.name)
    }
}

interface LookupStorageWrapper {
    fun removeLookupsFrom(files: Sequence<File>)
    fun update(lookupTracker: LookupTrackerWrapper, filesToCompile: Iterable<File>, removedFiles: Iterable<File>)
    fun flush()
    fun close()
    operator fun get(lookupSymbol: LookupSymbolWrapper): Collection<String>
}
