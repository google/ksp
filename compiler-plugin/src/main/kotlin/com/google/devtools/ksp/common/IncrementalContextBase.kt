/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPackage
import com.intellij.util.containers.MultiMap
import java.io.File
import java.util.Date

object symbolCollector : KSDefaultVisitor<(LookupSymbolWrapper) -> Unit, Unit>() {
    override fun defaultHandler(node: KSNode, data: (LookupSymbolWrapper) -> Unit) = Unit

    override fun visitDeclaration(declaration: KSDeclaration, data: (LookupSymbolWrapper) -> Unit) {
        if (declaration.isPrivate())
            return

        val name = declaration.simpleName.asString()
        val scope =
            declaration.qualifiedName?.asString()?.let { it.substring(0, Math.max(it.length - name.length - 1, 0)) }
                ?: return
        data(LookupSymbolWrapper(name, scope))
    }

    override fun visitDeclarationContainer(
        declarationContainer: KSDeclarationContainer,
        data: (LookupSymbolWrapper) -> Unit
    ) {
        // Local declarations aren't visible to other files / classes.
        if (declarationContainer is KSFunctionDeclaration)
            return

        declarationContainer.declarations.forEach {
            it.accept(this, data)
        }
    }
}

abstract class IncrementalContextBase(
    protected val anyChangesWildcard: File,
    protected val incrementalLog: Boolean,
    protected val baseDir: File,
    protected val cachesDir: File,
    protected val kspOutputDir: File,
    protected val knownModified: List<File>,
    protected val knownRemoved: List<File>,
    protected val changedClasses: List<String>,
) {
    // Symbols defined in changed files. This is used to update symbolsMap in the end.
    private val updatedSymbols = MultiMap.createSet<File, LookupSymbolWrapper>()

    // Sealed classes / interfaces on which `getSealedSubclasses` is invoked.
    // This is used to update sealedMap in the end.
    private val updatedSealed = MultiMap.createSet<File, LookupSymbolWrapper>()

    // Sealed classes / interfaces on which `getSealedSubclasses` is invoked.
    // This is saved across processing.
    protected val sealedMap = FileToSymbolsMap(File(cachesDir, "sealed"))

    // Symbols defined in each file. This is saved across processing.
    protected val symbolsMap = FileToSymbolsMap(File(cachesDir, "symbols"))

    private val cachesUpToDateFile = File(cachesDir, "caches.uptodate")
    private val rebuild = !cachesUpToDateFile.exists()

    private val logsDir = File(cachesDir, "logs").apply { mkdirs() }
    private val buildTime = Date().time

    private val modified = knownModified.map { it.relativeTo(baseDir) }.toSet()
    private val removed = knownRemoved.map { it.relativeTo(baseDir) }.toSet()

    protected abstract val isIncremental: Boolean

    protected abstract val symbolLookupTracker: LookupTrackerWrapper
    protected abstract val symbolLookupCache: LookupStorageWrapper

    protected abstract val classLookupTracker: LookupTrackerWrapper
    protected abstract val classLookupCache: LookupStorageWrapper

    private val sourceToOutputsMap = FileToFilesMap(File(cachesDir, "sourceToOutputs"))

    private fun String.toRelativeFile() = File(this).relativeTo(baseDir)
    private val KSFile.relativeFile
        get() = filePath.toRelativeFile()

    private fun collectDefinedSymbols(ksFiles: Collection<KSFile>) {
        ksFiles.forEach { file ->
            file.accept(symbolCollector) {
                updatedSymbols.putValue(file.relativeFile, it)
            }
        }
    }

    private val removedOutputsKey = File("<This is a virtual key for removed outputs; DO NOT USE>")

    private fun updateFromRemovedOutputs() {
        val removedOutputs = sourceToOutputsMap.get(removedOutputsKey) ?: return

        symbolLookupCache.removeLookupsFrom(removedOutputs.asSequence())
        classLookupCache.removeLookupsFrom(removedOutputs.asSequence())
        removedOutputs.forEach {
            symbolsMap.remove(it)
            sealedMap.remove(it)
        }

        sourceToOutputsMap.removeRecursively(removedOutputsKey)
    }

    private fun updateLookupCache(dirtyFiles: Collection<File>) {
        symbolLookupCache.update(symbolLookupTracker, dirtyFiles, knownRemoved)
        symbolLookupCache.flush()
        symbolLookupCache.close()

        classLookupCache.update(classLookupTracker, dirtyFiles, knownRemoved)
        classLookupCache.flush()
        classLookupCache.close()
    }

    private fun logSourceToOutputs(outputs: Set<File>, sourceToOutputs: Map<File, Set<File>>) {
        if (!incrementalLog)
            return

        val logFile = File(logsDir, "kspSourceToOutputs.log")
        logFile.appendText("=== Build $buildTime ===\n")
        logFile.appendText("Accumulated source to outputs map\n")
        sourceToOutputsMap.keys.forEach { source ->
            logFile.appendText("  $source:\n")
            sourceToOutputsMap[source]!!.forEach { output ->
                logFile.appendText("    $output\n")
            }
        }
        logFile.appendText("\n")

        logFile.appendText("Reprocessed sources and their outputs\n")
        sourceToOutputs.forEach { (source, outputs) ->
            logFile.appendText("  $source:\n")
            outputs.forEach {
                logFile.appendText("    $it\n")
            }
        }
        logFile.appendText("\n")

        // Can be larger than the union of the above, because some outputs may have no source.
        logFile.appendText("All reprocessed outputs\n")
        outputs.forEach {
            logFile.appendText("  $it\n")
        }
        logFile.appendText("\n")
    }

    private fun logDirtyFiles(
        files: Collection<KSFile>,
        allFiles: Collection<KSFile>,
        removedOutputs: Collection<File> = emptyList(),
        dirtyFilesByCP: Collection<File> = emptyList(),
        dirtyFilesByNewSyms: Collection<File> = emptyList(),
        dirtyFilesBySealed: Collection<File> = emptyList(),
    ) {
        if (!incrementalLog)
            return

        val logFile = File(logsDir, "kspDirtySet.log")
        logFile.appendText("=== Build $buildTime ===\n")
        logFile.appendText("All Files\n")
        allFiles.forEach { logFile.appendText("  ${it.relativeFile}\n") }
        logFile.appendText("Modified\n")
        modified.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("Removed\n")
        removed.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("Disappeared Outputs\n")
        removedOutputs.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("Affected By CP\n")
        dirtyFilesByCP.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("Affected By new syms\n")
        dirtyFilesByNewSyms.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("Affected By sealed\n")
        dirtyFilesBySealed.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("CP changes\n")
        changedClasses.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("Dirty:\n")
        files.forEach {
            logFile.appendText("  ${it.relativeFile}\n")
        }
        val percentage = "%.2f".format(files.size.toDouble() / allFiles.size.toDouble() * 100)
        logFile.appendText("\nDirty / All: $percentage%\n\n")
    }

    // Beware: no side-effects here; Caches should only be touched in updateCaches.
    fun calcDirtyFiles(ksFiles: List<KSFile>): Collection<KSFile> = closeFilesOnException {
        if (!isIncremental) {
            return ksFiles
        }

        if (rebuild) {
            collectDefinedSymbols(ksFiles)
            logDirtyFiles(ksFiles, ksFiles)
            return ksFiles
        }

        val newSyms = mutableSetOf<LookupSymbolWrapper>()

        // Parse and add newly defined symbols in modified files.
        ksFiles.filter { it.relativeFile in modified }.forEach { file ->
            file.accept(symbolCollector) {
                updatedSymbols.putValue(file.relativeFile, it)
                newSyms.add(it)
            }
        }

        val dirtyFilesByNewSyms = newSyms.flatMap {
            symbolLookupCache.get(it).map { File(it) }
        }

        val dirtyFilesBySealed = sealedMap.keys

        // Calculate dirty files by dirty classes in CP.
        val dirtyFilesByCP = changedClasses.flatMap { fqn ->
            val name = fqn.substringAfterLast('.')
            val scope = fqn.substringBeforeLast('.', "<anonymous>")
            classLookupCache.get(LookupSymbolWrapper(name, scope)).map { File(it) } +
                symbolLookupCache.get(LookupSymbolWrapper(name, scope)).map { File(it) }
        }.toSet()

        // output files that exist in CURR~2 but not in CURR~1
        val removedOutputs = sourceToOutputsMap.get(removedOutputsKey) ?: emptyList()

        val noSourceFiles = changedClasses.map { fqn ->
            NoSourceFile(baseDir, fqn).filePath.toRelativeFile()
        }.toSet()

        val initialSet = mutableSetOf<File>()
        initialSet.addAll(modified)
        initialSet.addAll(removed)
        initialSet.addAll(removedOutputs)
        initialSet.addAll(dirtyFilesByCP)
        initialSet.addAll(dirtyFilesByNewSyms)
        initialSet.addAll(dirtyFilesBySealed)
        initialSet.addAll(noSourceFiles)

        // modified can be seen as removed + new. Therefore the following check doesn't work:
        //   if (modified.any { it !in sourceToOutputsMap.keys }) ...
        if (modified.isNotEmpty() || changedClasses.isNotEmpty()) {
            initialSet.add(anyChangesWildcard)
        }

        val dirtyFiles = DirtinessPropagator(
            symbolLookupCache,
            symbolsMap,
            sourceToOutputsMap,
            anyChangesWildcard,
            removedOutputsKey
        ).propagate(initialSet)

        updateFromRemovedOutputs()

        logDirtyFiles(
            ksFiles.filter { it.relativeFile in dirtyFiles },
            ksFiles,
            removedOutputs,
            dirtyFilesByCP,
            dirtyFilesByNewSyms,
            dirtyFilesBySealed
        )
        return ksFiles.filter { it.relativeFile in dirtyFiles }
    }

    // Loop detection isn't needed because of overwritten checks in CodeGeneratorImpl
    private fun FileToFilesMap.removeRecursively(src: File) {
        get(src)?.forEach { out ->
            removeRecursively(out)
        }
        remove(src)
    }

    private fun updateSourceToOutputs(
        dirtyFiles: Collection<File>,
        outputs: Set<File>,
        sourceToOutputs: Map<File, Set<File>>,
        removedOutputs: List<File>,
    ) {
        // Prune deleted sources in source-to-outputs map.
        removed.forEach {
            sourceToOutputsMap.removeRecursively(it)
        }

        dirtyFiles.filterNot { sourceToOutputs.containsKey(it) }.forEach {
            sourceToOutputsMap.removeRecursively(it)
        }

        removedOutputs.forEach {
            sourceToOutputsMap.removeRecursively(it)
        }
        sourceToOutputsMap.put(removedOutputsKey, removedOutputs)

        // Update source-to-outputs map from those reprocessed.
        sourceToOutputs.forEach { src, outs ->
            sourceToOutputsMap.put(src, outs.toList())
        }

        logSourceToOutputs(outputs, sourceToOutputs)

        sourceToOutputsMap.flush()
    }

    private fun updateOutputs(outputs: Set<File>, cleanOutputs: Collection<File>) {
        val outRoot = kspOutputDir
        val bakRoot = File(cachesDir, "backups")

        fun File.abs() = File(baseDir, path)
        fun File.bak() = File(bakRoot, abs().toRelativeString(outRoot))

        // Backing up outputs is necessary for two reasons:
        //
        // 1. Currently, outputs are always cleaned up in gradle plugin before compiler is called.
        //    Untouched outputs need to be restore.
        //
        //    TODO: need a change in upstream to not clean files in gradle plugin.
        //    Not cleaning files in gradle plugin has potentially fewer copies when processing succeeds.
        //
        // 2. Even if outputs are left from last compilation / processing, processors can still
        //    fail and the outputs will need to be restored.

        // Backup
        outputs.forEach { generated ->
            copyWithTimestamp(generated.abs(), generated.bak(), true)
        }

        // Restore non-dirty outputs
        cleanOutputs.forEach { dst ->
            if (dst !in outputs) {
                copyWithTimestamp(dst.bak(), dst.abs(), true)
            }
        }
    }

    private fun updateCaches(dirtyFiles: Collection<File>, outputs: Set<File>, sourceToOutputs: Map<File, Set<File>>) {
        // dirtyFiles may contain new files, which are unknown to sourceToOutputsMap.
        val oldOutputs = dirtyFiles.flatMap { sourceToOutputsMap[it] ?: emptyList() }.distinct()
        val removedOutputs = oldOutputs.filterNot { it in outputs }
        updateSourceToOutputs(dirtyFiles, outputs, sourceToOutputs, removedOutputs)
        updateLookupCache(dirtyFiles)

        // Update symbolsMap
        fun <K : Comparable<K>, V> update(m: PersistentMap<K, List<V>>, u: MultiMap<K, V>) {
            // Update symbol caches from modified files.
            u.keySet().forEach {
                m.put(it, u[it].toList())
            }
        }

        fun <K : Comparable<K>, V> remove(m: PersistentMap<K, List<V>>, removedKeys: Collection<K>) {
            // Remove symbol caches from removed files.
            removedKeys.forEach {
                m.remove(it)
            }
        }

        if (!rebuild) {
            update(sealedMap, updatedSealed)
            remove(sealedMap, removed)

            update(symbolsMap, updatedSymbols)
            remove(symbolsMap, removed)
        } else {
            symbolsMap.clear()
            update(symbolsMap, updatedSymbols)

            sealedMap.clear()
            update(sealedMap, updatedSealed)
        }
        symbolsMap.flush()
        sealedMap.flush()
    }

    fun registerGeneratedFiles(newFiles: Collection<KSFile>) = closeFilesOnException {
        if (!isIncremental)
            return@closeFilesOnException

        collectDefinedSymbols(newFiles)
    }

    private inline fun <T> closeFilesOnException(f: () -> T): T {
        try {
            return f()
        } catch (e: Exception) {
            symbolsMap.flush()
            sealedMap.flush()
            symbolLookupCache.close()
            classLookupCache.close()
            sourceToOutputsMap.flush()
            throw e
        }
    }

    // TODO: add a wildcard for outputs with no source and get rid of the outputs parameter.
    fun updateCachesAndOutputs(
        dirtyFiles: Collection<KSFile>,
        outputs: Set<File>,
        sourceToOutputs: Map<File, Set<File>>,
    ) = closeFilesOnException {
        if (!isIncremental)
            return

        cachesUpToDateFile.delete()
        assert(!cachesUpToDateFile.exists())

        val dirtySources = dirtyFiles.map { it.relativeFile }

        // Throw away results from clean inputs.
        //
        // One common misuse of incremental APIs is associating a non-root source, instead of the ones obtained from
        // root functions (e.g., getSymbolsWithAnnotation), to an output. This non-root source can be reached and
        // reprocessed even when it is clean. Because it is clean, it is not available via root functions. As a result,
        // other outputs that are solely based on it won't be re-generated and is deemed as removed.
        //
        // Assuming that the processors are deterministic, we are throwing away outputs from clean inputs, and
        // recovering them from the backup as a workaround for processors.

        val unassociated = outputs - sourceToOutputs.values.flatten()
        val dirties = HashSet(unassociated)
        fun markDirty(file: File) {
            dirties.add(file)
            sourceToOutputs.get(file)?.forEach {
                markDirty(it)
            }
        }
        fun isDirty(file: File) = file in dirties

        val roots = mutableSetOf(anyChangesWildcard, removedOutputsKey)
        roots.addAll(dirtySources)
        // TODO: find a better way to identify NoSourceFile
        roots.addAll(
            sourceToOutputs.keys.filter {
                it.path.startsWith("<NoSourceFile for ") &&
                    it.path.endsWith(" is a virtual file; DO NOT USE.>")
            }
        )
        roots.forEach {
            markDirty(it)
        }

        val dirtySourceToOutputs = sourceToOutputs.filter { (src, outs) ->
            isDirty(src)
        }
        val dirtyOutputs = outputs.filter(::isDirty).toSet()

        updateCaches(dirtySources, dirtyOutputs, dirtySourceToOutputs)

        val cleanOutputs = mutableSetOf<File>()
        sourceToOutputsMap.keys.forEach { source ->
            if (!isDirty(source))
                cleanOutputs.addAll(sourceToOutputsMap[source]!!)
        }
        sourceToOutputsMap.flush()
        updateOutputs(dirtyOutputs, cleanOutputs)

        cachesUpToDateFile.createNewFile()
        assert(cachesUpToDateFile.exists())
    }

    // Insert Java file -> names lookup records.
    fun recordLookup(psiFile: PsiJavaFile, fqn: String) {
        val path = psiFile.virtualFile.path
        val name = fqn.substringAfterLast('.')
        val scope = fqn.substringBeforeLast('.', "<anonymous>")

        // Java types are classes. Therefore lookups only happen in packages.
        fun record(scope: String, name: String) =
            symbolLookupTracker.record(path, scope, name)

        record(scope, name)

        // If a resolved name is from some * import, it is overridable by some out-of-file changes.
        // Therefore, the potential providers all need to be inserted. They are
        //   1. definition of the name in the same package
        //   2. other * imports
        val onDemandImports =
            psiFile.getOnDemandImports(false, false).mapNotNull { (it as? PsiPackage)?.qualifiedName }
        if (scope in onDemandImports) {
            record(psiFile.packageName, name)
            onDemandImports.forEach {
                record(it, name)
            }
        }
    }

    fun recordGetSealedSubclasses(classDeclaration: KSClassDeclaration) {
        val name = classDeclaration.simpleName.asString()
        val scope = classDeclaration.qualifiedName?.asString()
            ?.let { it.substring(0, Math.max(it.length - name.length - 1, 0)) } ?: return
        updatedSealed.putValue(classDeclaration.containingFile!!.relativeFile, LookupSymbolWrapper(name, scope))
    }
}

internal class DirtinessPropagator(
    private val lookupCache: LookupStorageWrapper,
    private val symbolsMap: FileToSymbolsMap,
    private val sourceToOutputs: FileToFilesMap,
    private val anyChangesWildcard: File,
    private val removedOutputsKey: File
) {
    private val visitedFiles = mutableSetOf<File>()
    private val visitedSyms = mutableSetOf<LookupSymbolWrapper>()

    private val outputToSources = mutableMapOf<File, MutableSet<File>>().apply {
        sourceToOutputs.keys.forEach { source ->
            if (source != anyChangesWildcard && source != removedOutputsKey) {
                sourceToOutputs[source]!!.forEach { output ->
                    getOrPut(output) { mutableSetOf() }.add(source)
                }
            }
        }
    }

    private fun visit(sym: LookupSymbolWrapper) {
        if (sym in visitedSyms)
            return
        visitedSyms.add(sym)

        lookupCache.get(sym).forEach {
            visit(File(it))
        }
    }

    private fun visit(file: File) {
        if (file in visitedFiles)
            return
        visitedFiles.add(file)

        // Propagate by dependencies
        symbolsMap[file]?.forEach {
            visit(it)
        }

        // Propagate by input-output relations
        // Given (..., I, ...) -> O:
        // 1) if I is dirty, then O is dirty.
        // 2) if O is dirty, then O must be regenerated, which requires all of its inputs to be reprocessed.
        sourceToOutputs[file]?.forEach {
            visit(it)
        }
        outputToSources[file]?.forEach {
            visit(it)
        }
    }

    fun propagate(initialSet: Collection<File>): Set<File> {
        initialSet.forEach { visit(it) }
        return visitedFiles
    }
}
