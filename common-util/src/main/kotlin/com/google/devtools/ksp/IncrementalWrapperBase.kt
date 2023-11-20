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
