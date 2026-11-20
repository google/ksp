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

import com.google.devtools.ksp.IncrementalContextLoggingOptions
import com.google.devtools.ksp.buildApiFeatures
import com.google.devtools.ksp.impl.symbol.kotlin.separateQualifierAndName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPackage
import com.intellij.util.containers.MultiMap
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.attribute.BasicFileAttributes
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

object SymbolCollector : KSDefaultVisitor<(LookupSymbolWrapper) -> Unit, Unit>(
    buildApiFeatures {
        enableBackingFields = true
        enableContextParameters = true
    }
) {
    override fun defaultHandler(node: KSNode, data: (LookupSymbolWrapper) -> Unit) = Unit

    override fun visitDeclaration(declaration: KSDeclaration, data: (LookupSymbolWrapper) -> Unit) {
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

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
abstract class IncrementalContextBase(
    protected val anyChangesWildcard: File,
    protected val loggingOptions: IncrementalContextLoggingOptions,
    protected val baseDir: File,
    protected val cachesDir: File,
    protected val kspOutputDir: File,
    protected val knownModified: List<File>,
    protected val knownRemoved: List<File>,
    protected val changedClasses: List<String>,
) {
    // Symbols defined in changed files. This is used to update symbolsMap in the end.
    protected val updatedSymbols = MultiMap.createSet<File, LookupSymbolWrapper>()

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
    protected val buildTime = Date().time

    private val modified = knownModified.map { it.relativeTo(baseDir) }.toSet()
    private val removed = knownRemoved.map { it.relativeTo(baseDir) }.toSet()

    protected abstract val isIncremental: Boolean

    protected abstract val symbolLookupTracker: LookupTrackerWrapper
    protected abstract val symbolLookupCache: LookupStorageWrapper

    protected abstract val classLookupTracker: LookupTrackerWrapper
    protected abstract val classLookupCache: LookupStorageWrapper

    private val sourceToOutputsMap = FileToFilesMap(File(cachesDir, "sourceToOutputs"))

    private val onDemandImportsCache = ConcurrentHashMap<PsiJavaFile, List<String>>()

    protected val logFiles: MutableList<Closeable> = mutableListOf()

    private fun String.toRelativeFile(): File {
        val file = File(this)
        val absFile = if (file.isAbsolute) file else File(baseDir, this)
        return absFile.absoluteFile.relativeTo(baseDir.absoluteFile)
    }

    private fun File.toRelativeFile(): File {
        val absFile = if (this.isAbsolute) this else File(baseDir, this.path)
        return absFile.absoluteFile.relativeTo(baseDir.absoluteFile)
    }

    private val KSFile.relativeFile
        get() = filePath.toRelativeFile()

    private fun collectDefinedSymbols(ksFiles: Collection<KSFile>) {
        ksFiles.forEach { file ->
            file.accept(SymbolCollector) {
                updatedSymbols.putValue(file.relativeFile, it)
            }
        }
    }

    private val removedOutputsKey = File("<This is a virtual key for removed outputs; DO NOT USE>")
    private val unassociatedOutputsKey = File("<This is a virtual key for unassociated outputs; DO NOT USE>")
    private val syntheticKeys = setOf(anyChangesWildcard, removedOutputsKey, unassociatedOutputsKey)

    // Synthetic files in the symbol lookup cache, under which classpath lookups by processors are recorded:
    // - classpathQueriesKey: classes and packages that processors asked the Resolver for, e.g. by
    //   getClassDeclarationByName or getDeclarationsFromPackage. If one changes, processors must run.
    // - librarySymbolsKey: library classes that processors saw while processing. Only aggregating outputs depend on
    //   these: isolating outputs are covered by the lookups of their sources.
    // A package scan is recorded as the name PACKAGE_SCAN in the scope of the package.
    private val classpathQueriesKey = File("<This is a virtual key for classpath queries; DO NOT USE>")
    private val librarySymbolsKey = File("<This is a virtual key for library symbols; DO NOT USE>")
    private val classpathLookupKeys = setOf(classpathQueriesKey, librarySymbolsKey)

    // Marks caches that record classpath lookups; see canSkipProcessing. Bump the version whenever what is recorded
    // under classpathLookupKeys changes.
    private val classpathLookupsMarkerKey = File("<This is a virtual key for classpath lookups v1; DO NOT USE>")

    private fun updateFromRemovedOutputs() {
        val removedOutputs = sourceToOutputsMap[removedOutputsKey] ?: return

        symbolLookupCache.removeLookupsFrom(removedOutputs.asSequence())
        classLookupCache.removeLookupsFrom(removedOutputs.asSequence())
        removedOutputs.forEach {
            symbolsMap.remove(it)
            sealedMap.remove(it)
        }

        sourceToOutputsMap.removeRecursively(removedOutputsKey)
    }

    private fun updateLookupCache(dirtyFiles: Collection<File>) {
        // Every run that processes reruns all processors and regenerates all aggregating outputs (see
        // initialDirtyFiles), so its classpath lookups replace the old ones.
        val classpathLookupFiles = classpathLookupKeys.map { it.lookupPath() }
        symbolLookupCache.update(symbolLookupTracker, dirtyFiles + classpathLookupFiles, knownRemoved)
        symbolLookupCache.flush()
        symbolLookupCache.close()

        classLookupCache.update(classLookupTracker, dirtyFiles, knownRemoved)
        classLookupCache.flush()
        classLookupCache.close()
    }

    private fun logSourceToOutputs(outputs: Set<File>, sourceToOutputs: Map<File, Set<File>>) {
        if (!loggingOptions.incrementalLoggingEnabled)
            return

        mkLogFile("kspSourceToOutputs.log").bufferedWriter().use { logFile ->
            logFile.write("Accumulated source to outputs map\n")
            sourceToOutputsMap.keys.forEach { source ->
                logFile.write("  $source:\n")
                sourceToOutputsMap[source]!!.forEach { output ->
                    logFile.write("    $output\n")
                }
            }
            logFile.write("\n")

            logFile.write("Reprocessed sources and their outputs\n")
            sourceToOutputs.forEach { (source, outputs) ->
                logFile.write("  $source:\n")
                outputs.forEach {
                    logFile.write("    $it\n")
                }
            }
            logFile.write("\n")

            // Can be larger than the union of the above, because some outputs may have no source.
            logFile.write("All reprocessed outputs\n")
            outputs.forEach {
                logFile.write("  $it\n")
            }
            logFile.write("\n")
        }
    }

    private fun logDirtyFiles(
        files: Collection<File>,
        allFiles: Collection<File>,
        removedOutputs: Collection<File> = emptyList(),
        dirtyFilesByCP: Collection<File> = emptyList(),
        dirtyFilesByNewSyms: Collection<File> = emptyList(),
        dirtyFilesBySealed: Collection<File> = emptyList(),
    ) {
        if (!loggingOptions.incrementalLoggingEnabled) {
            return
        }

        mkLogFile("kspDirtySet.log").bufferedWriter().use { logFile ->
            logFile.write("All Files\n")
            allFiles.forEach { logFile.write("  $it\n") }
            logFile.write("Modified\n")
            modified.forEach { logFile.write("  $it\n") }
            logFile.write("Removed\n")
            removed.forEach { logFile.write("  $it\n") }
            logFile.write("Disappeared Outputs\n")
            removedOutputs.forEach { logFile.write("  $it\n") }
            logFile.write("Affected By CP\n")
            dirtyFilesByCP.forEach { logFile.write("  $it\n") }
            logFile.write("Affected By new syms\n")
            dirtyFilesByNewSyms.forEach { logFile.write("  $it\n") }
            logFile.write("Affected By sealed\n")
            dirtyFilesBySealed.forEach { logFile.write("  $it\n") }
            logFile.write("CP changes\n")
            changedClasses.forEach { logFile.write("  $it\n") }
            logFile.write("Dirty:\n")
            files.forEach {
                logFile.write("  $it\n")
            }
            val ratio = if (allFiles.isEmpty()) 0.0 else files.size.toDouble() / allFiles.size.toDouble()
            val percentage = "%.2f".format(ratio * 100)
            logFile.write("\nDirty / All: $percentage%\n\n")
        }
    }

    /**
     * Fast path for incremental runs in which nothing is dirty: no source was modified or removed, no classpath
     * change reaches a source file or an output of this module, and no changed class is one that processors asked
     * the Resolver for (by name, e.g. `getClassDeclarationByName`, or by scanning its package, e.g.
     * `getDeclarationsFromPackage`). Processors are then not run at all.
     *
     * Aggregating outputs are regenerated whenever KSP processes, but they don't prevent the early exit unless a
     * processor saw a changed class while generating them.
     *
     * Returns `true` if processing can be skipped. The clean outputs have then been restored from their backups, the
     * logs written and the lookup caches closed, so this context must not be used again.
     * Returns `false` if processing is needed. Nothing has been changed, and the caller continues with
     * [calcDirtyFiles] as usual.
     *
     * [allSourceFiles] is only used for logging and is only called when incremental logging is enabled.
     */
    fun tryEarlyExitIfClean(allSourceFiles: () -> Collection<File>): Boolean = closeFilesOnException {
        if (!canSkipProcessing()) {
            return@closeFilesOnException false
        }
        if (loggingOptions.incrementalLoggingEnabled) {
            logDirtyFiles(emptyList(), allSourceFiles().map { it.toRelativeFile() }.distinct())
            logBeforeCacheFlush(emptySet(), emptyMap())
        }
        restoreCleanOutputs()
        closeFiles()
        true
    }

    private fun canSkipProcessing(): Boolean {
        if (!isIncremental || rebuild) return false
        // A missing key means the caches were written by an older KSP that didn't record outputs without sources
        // (Dependencies(false)). We can't prove there are none, and they'd be lost if we exited early.
        if (unassociatedOutputsKey !in sourceToOutputsMap) return false
        // A missing key means the caches were written by an older KSP that didn't record classpath lookups. Any
        // processor may have asked for a changed class, so processors must run once to record them.
        if (classpathLookupsMarkerKey !in sourceToOutputsMap) return false
        // Nothing to propagate from means calcDirtyFiles would return no files. Newly defined symbols can be ignored:
        // they only come from modified files.
        if (changedInputs().isNotEmpty()) return false
        // A processor asked for a class that changed, so it may generate something else now, even if it generated
        // nothing last time.
        if (classpathQueriesKey in lookupsOfChangedClasses) return false
        // Aggregating outputs are regenerated whenever KSP processes, so they only prevent the early exit if a
        // processor saw a changed class while generating them.
        if (!sourceToOutputsMap[anyChangesWildcard].isNullOrEmpty() && librarySymbolsKey in lookupsOfChangedClasses) {
            return false
        }
        // If an output can't be restored (neither it nor its backup exists), let the normal path handle it. It
        // invalidates caches.uptodate before restoring, so the next run is a full rebuild instead of the same failure.
        if (cleanOutputs().any { !it.abs().exists() && !it.bak().exists() }) return false
        return true
    }

    // Like the restore step in updateOutputs, outputs are restored from their backups. Outputs with the same size and
    // timestamp as their backup are skipped: the Gradle plugin only deletes outputs when KSP processes, so on an early
    // exit they are usually still in place. If they were changed outside of KSP, Gradle reruns the task
    // non-incrementally, which invalidates the caches, so there is no early exit.
    private fun restoreCleanOutputs() {
        try {
            cleanOutputs().forEach { output ->
                val bak = output.bak()
                val abs = output.abs()
                if (bak.exists() && !abs.matches(bak)) copyWithTimestamp(bak, abs, true)
            }
        } catch (e: Exception) {
            // Like updateCachesAndOutputs: make the next run a full rebuild rather than failing the same way again.
            cachesUpToDateFile.delete()
            throw e
        }
    }

    private fun cleanOutputs(): Set<File> = sourceToOutputsMap.values.flatten().toSet()

    // The files and classpath lookup keys that looked up a changed class: by its name, or by scanning a package that
    // contains it.
    private val lookupsOfChangedClasses: Set<File> by lazy {
        changedClasses.flatMap { fqn ->
            val (scope, name) = separateQualifierAndName(fqn)
            val symbol = LookupSymbolWrapper(name, scope)
            val packageScans = enclosingScopes(fqn).flatMap { symbolLookupCache[LookupSymbolWrapper(PACKAGE_SCAN, it)] }
            classLookupCache[symbol] + symbolLookupCache[symbol] + packageScans
        }.map { it.toRelativeFile() }.toSet()
    }

    // Files in this module that looked up a class that changed on the classpath.
    private fun dirtyFilesByClasspath(): Set<File> = lookupsOfChangedClasses - classpathLookupKeys

    private fun noSourceFilesOfChangedClasses(): Set<File> = changedClasses.map { fqn ->
        NoSourceFile(baseDir, fqn).filePath.toRelativeFile()
    }.toSet()

    // The files from which calcDirtyFiles propagates dirtiness, apart from aggregating outputs and the files that look
    // up newly defined symbols. Shared with canSkipProcessing so that both agree on what is dirty.
    private fun changedInputs(dirtyFilesByCP: Set<File> = dirtyFilesByClasspath()): Set<File> = buildSet {
        addAll(modified)
        addAll(removed)
        addAll(dirtyFilesByCP)
        // Files that called getSealedSubclasses are always dirty.
        addAll(sealedMap.keys)
        addAll(noSourceFilesOfChangedClasses().flatMap { sourceToOutputsMap[it].orEmpty() })
        addAll(sourceToOutputsMap[unassociatedOutputsKey].orEmpty())
        addAll(sourceToOutputsMap[removedOutputsKey].orEmpty())
    }

    // Aggregating outputs are dirty whenever KSP processes: they can depend on any change. That they don't depend on
    // a changed class is only used to skip processing; see canSkipProcessing.
    private fun initialDirtyFiles(dirtyFilesByCP: Set<File>): Set<File> =
        changedInputs(dirtyFilesByCP) + sourceToOutputsMap[anyChangesWildcard].orEmpty()

    // Beware: no side-effects here; Caches should only be touched in updateCaches.
    fun calcDirtyFiles(ksFiles: List<KSFile>): Collection<KSFile> = closeFilesOnException {
        if (!isIncremental) {
            return@closeFilesOnException ksFiles
        }

        if (rebuild) {
            collectDefinedSymbols(ksFiles)
            val allFiles = ksFiles.map { it.relativeFile }
            logDirtyFiles(allFiles, allFiles)
            return@closeFilesOnException ksFiles
        }

        val newSyms = mutableSetOf<LookupSymbolWrapper>()

        // Parse and add newly defined symbols in modified files.
        ksFiles.filter { it.relativeFile in modified }.forEach { file ->
            file.accept(SymbolCollector) {
                updatedSymbols.putValue(file.relativeFile, it)
                newSyms.add(it)
            }
        }

        val dirtyFilesByNewSyms = newSyms.flatMap { newSym ->
            symbolLookupCache[newSym].map { it.toRelativeFile() }
        } - classpathLookupKeys
        val dirtyFilesBySealed = sealedMap.keys

        // Calculate dirty files by dirty classes in CP.
        val dirtyFilesByCP = dirtyFilesByClasspath()

        // output files that exist in CURR~2 but not in CURR~1
        val removedOutputs = sourceToOutputsMap[removedOutputsKey] ?: emptyList()

        val initialSet = initialDirtyFiles(dirtyFilesByCP) + dirtyFilesByNewSyms

        val dirtyFiles = DirtinessPropagator(
            symbolLookupCache,
            symbolsMap,
            sourceToOutputsMap,
            syntheticKeys,
        ).propagate(initialSet)

        updateFromRemovedOutputs()

        val dirtyKSFiles = ksFiles.filter { it.relativeFile in dirtyFiles }

        logDirtyFiles(
            dirtyKSFiles.map { it.relativeFile },
            ksFiles.map { it.relativeFile },
            removedOutputs,
            dirtyFilesByCP,
            dirtyFilesByNewSyms,
            dirtyFilesBySealed
        )
        return@closeFilesOnException dirtyKSFiles
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

        if (unassociatedOutputsKey !in sourceToOutputs) {
            sourceToOutputsMap.removeRecursively(unassociatedOutputsKey)
            // Always record the key (possibly empty), so that a missing key unambiguously means the caches were
            // written by an older KSP that didn't track unassociated outputs. See tryEarlyExitIfClean.
            sourceToOutputsMap[unassociatedOutputsKey] = emptyList()
        }
        // The classpath lookups of this run were recorded; see updateLookupCache.
        sourceToOutputsMap[classpathLookupsMarkerKey] = emptyList()

        removedOutputs.forEach {
            sourceToOutputsMap.removeRecursively(it)
        }
        sourceToOutputsMap[removedOutputsKey] = removedOutputs

        // Update source-to-outputs map from those reprocessed.
        sourceToOutputs.forEach { (src, outs) ->
            sourceToOutputsMap[src] = outs.toList()
        }

        logBeforeCacheFlush(outputs, sourceToOutputs)

        sourceToOutputsMap.flush()
    }

    private fun File.abs() = File(baseDir, path)
    private fun File.bak() = File(File(cachesDir, "backups"), abs().toRelativeString(kspOutputDir))

    // Backups are copied with their timestamp, so an output that was restored or backed up and left alone matches.
    private fun File.matches(backup: File): Boolean {
        val attributes = attributesOrNull() ?: return false
        val backupAttributes = backup.attributesOrNull() ?: return false
        return attributes.isRegularFile &&
            attributes.size() == backupAttributes.size() &&
            attributes.lastModifiedTime().toMillis() == backupAttributes.lastModifiedTime().toMillis()
    }

    private fun File.attributesOrNull(): BasicFileAttributes? = try {
        Files.readAttributes(toPath(), BasicFileAttributes::class.java)
    } catch (e: NoSuchFileException) {
        null
    }

    private fun updateOutputs(outputs: Set<File>, cleanOutputs: Collection<File>) {
        // Backing up outputs is necessary for two reasons:
        //
        // 1. Currently, outputs are cleaned up in the Gradle plugin when KSP processes.
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

    private fun updateCaches(
        dirtyFiles: Collection<File>,
        outputs: Set<File>,
        sourceToOutputs: Map<File, Set<File>>
    ) {
        // dirtyFiles may contain new files, which are unknown to sourceToOutputsMap.
        val oldOutputs = (
            dirtyFiles.flatMap { sourceToOutputsMap[it] ?: emptyList() } +
                (sourceToOutputsMap[unassociatedOutputsKey] ?: emptyList())
            ).distinct()
        val removedOutputs = oldOutputs.filterNot { it in outputs }
        updateSourceToOutputs(dirtyFiles, outputs, sourceToOutputs, removedOutputs)
        updateLookupCache(dirtyFiles)

        // Update symbolsMap
        fun <K : Comparable<K>, V> update(m: PersistentMap<K, List<V>>, u: MultiMap<K, V>) {
            // Update symbol caches from modified files.
            u.keySet().forEach {
                m[it] = u[it].toList()
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

    fun <T> closeFilesOnException(f: () -> T): T {
        try {
            return f()
        } catch (e: Exception) {
            closeFiles()
            throw e
        }
    }

    // Doesn't flush the caches: they are only written by updateCachesAndOutputs. Called on errors, where writing
    // half-updated caches while caches.uptodate is still present would corrupt the next incremental run.
    fun closeFiles() {
        onDemandImportsCache.clear()
        symbolLookupCache.close()
        classLookupCache.close()
        logFiles.forEach { it.close() }
    }

    // TODO: add a wildcard for outputs with no source and get rid of the outputs parameter.
    fun updateCachesAndOutputs(
        dirtyFiles: Collection<KSFile>,
        outputs: Set<File>,
        sourceToOutputs: Map<File, Set<File>>,
    ) = closeFilesOnException {
        if (!isIncremental)
            return@closeFilesOnException

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

        val relativeOutputs = outputs.map { it.toRelativeFile() }.toSet()
        val unassociated = relativeOutputs - sourceToOutputs.values.flatten()
        val dirties = HashSet(unassociated)
        fun markDirty(file: File) {
            dirties.add(file)
            sourceToOutputs[file]?.forEach {
                markDirty(it)
            }
        }

        fun isDirty(file: File) = file in dirties

        val roots = syntheticKeys.toMutableSet()
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

        val dirtySourceToOutputs = sourceToOutputs.filter { (src, _) ->
            isDirty(src)
        }.toMutableMap()
        if (unassociated.isNotEmpty()) {
            dirtySourceToOutputs[unassociatedOutputsKey] = unassociated
        }
        val dirtyOutputs = relativeOutputs.filter(::isDirty).toSet()

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

    /**
     * Insert Java file -> names lookup records.
     * In other words, it inserts the names of symbols that may invalidate the [psiFile].
     * There are several cases where that may happen:
     * - If `fqn` has been looked up in the file, then `fqn` may invalidate the file
     * (`fqn` may come from the same file or out of file).
     * - If `sym` is a named import, then `sym` may invalidate the file.
     * - If `sym` is an on-demand import, then `sym` may invalidate the file.
     * - If `sym` is an implicit import, i.e., from the same package, then `sym` may invalidate the file.
     *
     * If [psiFile] is a class file of a library, `fqn` is recorded with [recordLibraryClass] instead.
     */
    fun recordLookup(psiFile: PsiJavaFile, fqn: String) {
        if (!isIncremental)
            return

        // Resolved in a Java library class, so only aggregating outputs can depend on it.
        if (psiFile.fileType.isBinary) {
            recordLibraryClass(fqn)
            return
        }

        val path = psiFile.virtualFile.path
        val (scope, name) = separateQualifierAndName(fqn)

        // Java types are classes. Therefore lookups only happen in packages.
        fun record(scope: String, name: String) =
            symbolLookupTracker.record(path, scope, name)

        record(scope, name)

        // If a resolved name is from some * import, it is overridable by some out-of-file changes.
        // Therefore, the potential providers all need to be inserted. They are
        //   1. definition of the name in the same package
        //   2. other * imports
        val onDemandImports = onDemandImportsCache.getOrPut(psiFile) {
            psiFile.getOnDemandImports().mapNotNull { (it.importReference?.resolve() as? PsiPackage)?.qualifiedName }
        }
        if (scope in onDemandImports) {
            record(psiFile.packageName, name)
            onDemandImports.forEach {
                record(it, name)
            }
        }
    }

    /**
     * Returns all on-demand imports for `this`, i.e., `import X.*`.
     */
    private fun PsiJavaFile.getOnDemandImports(): List<PsiImportStatement> =
        this.importList?.importStatements
            ?.filter { it.isOnDemand }
            ?: emptyList()

    fun recordGetSealedSubclasses(classDeclaration: KSClassDeclaration) {
        if (!isIncremental)
            return

        val name = classDeclaration.simpleName.asString()
        val scope = classDeclaration.qualifiedName?.asString()
            ?.let { it.substring(0, (it.length - name.length - 1).coerceAtLeast(0)) } ?: return
        updatedSealed.putValue(classDeclaration.containingFile!!.relativeFile, LookupSymbolWrapper(name, scope))
    }

    /** Records that a processor asked the Resolver for the class [fqn] by name. */
    fun recordClasspathQuery(fqn: String) = recordClass(classpathQueriesKey, fqn)

    /** Records that a processor asked the Resolver for the declarations in [packageName]. */
    fun recordPackageQuery(packageName: String) = recordPackageScan(classpathQueriesKey, packageName)

    /** Records that a processor saw the library class [fqn]. */
    fun recordLibraryClass(fqn: String) = recordClass(librarySymbolsKey, fqn)

    /**
     * Records that a processor saw something in the library package [packageName] that only changes as some class
     * in the package does, e.g. a typealias or the subclasses of a sealed class.
     */
    fun recordLibraryPackage(packageName: String) = recordPackageScan(librarySymbolsKey, packageName)

    // Nested classes are spelled with dots, like changedClasses.
    private fun recordClass(key: File, fqn: String) {
        val (scope, name) = separateQualifierAndName(fqn.replace('$', '.'))
        record(key, scope, name)
    }

    private fun recordPackageScan(key: File, packageName: String) = record(key, packageName, PACKAGE_SCAN)

    private fun record(key: File, scope: String, name: String) {
        if (!isIncremental)
            return

        symbolLookupTracker.record(key.lookupPath().path, scope, name)
    }

    // Paths in the lookup storage must be absolute: they are read back relative to baseDir.
    private fun File.lookupPath() = File(baseDir, path)

    protected open fun logBeforeCacheFlush(outputs: Set<File>, sourceToOutputs: Map<File, Set<File>>) {
        logSourceToOutputs(outputs, sourceToOutputs)
    }

    protected fun mkFileInLogDir(fileName: String): File =
        File(logsDir, fileName)
            .also {
                if (it.delete()) {
                    it.createNewFile()
                }
            }

    protected fun mkLogFile(fileName: String): File = mkFileInLogDir(fileName)
        .also { it.appendText("=== Build $buildTime ===\n") }
}

// The name under which a scan of all declarations in a package is recorded.
private const val PACKAGE_SCAN = "*"

// The packages in which a scan finds the changed class [fqn] or one of its outer classes: "a.b.C.D" is found by scans
// of "a.b.C", "a.b", "a" and "". Nested classes are spelled with dots in changedClasses, so outer classes can't be told
// apart from packages.
private fun enclosingScopes(fqn: String): Sequence<String> =
    generateSequence(fqn.substringBeforeLast('.', "")) { scope ->
        if (scope.isEmpty()) null else scope.substringBeforeLast('.', "")
    }

internal class DirtinessPropagator(
    private val lookupCache: LookupStorageWrapper,
    private val symbolsMap: FileToSymbolsMap,
    private val sourceToOutputs: FileToFilesMap,
    private val syntheticKeys: Set<File>,
) {
    private val visitedFiles = mutableSetOf<File>()
    private val visitedSyms = mutableSetOf<LookupSymbolWrapper>()

    private fun File.isSyntheticKey() = this in syntheticKeys

    private val outputToSources = mutableMapOf<File, MutableSet<File>>().apply {
        sourceToOutputs.keys.forEach { source ->
            if (!source.isSyntheticKey()) {
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

        lookupCache[sym].forEach {
            visit(File(it))
        }
    }

    private fun visit(file: File) {
        if (file in visitedFiles)
            return
        visitedFiles.add(file)

        // Propagate by dependencies
        if (NoSourceFile.isSyntheticFile(file.name)) {
            val fqn = stripSyntheticFileNameModifiers(file.name)
            val (scope, name) = separateQualifierAndName(fqn)
            visit(LookupSymbolWrapper(name, scope))
        } else {
            symbolsMap[file]?.forEach {
                visit(it)
            }
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
