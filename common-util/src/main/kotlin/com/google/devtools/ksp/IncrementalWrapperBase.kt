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

package com.google.devtools.ksp

import com.intellij.util.containers.MultiMap
import java.io.File

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
