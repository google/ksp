package com.google.devtools.ksp

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.kotlin.KSFileImpl
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.intellij.util.containers.MultiMap
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.storage.BasicMap
import org.jetbrains.kotlin.incremental.storage.CollectionExternalizer
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import org.jetbrains.kotlin.psi.KtFile
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.util.*

class FileToSymbolsMap(storageFile: File) : BasicMap<File, Collection<LookupSymbol>>(storageFile, FileKeyDescriptor, CollectionExternalizer(LookupSymbolExternalizer, { HashSet() })) {
    override fun dumpKey(key: File): String = key.toString()

    override fun dumpValue(value: Collection<LookupSymbol>): String = value.toString()

    fun add(file: File, symbol: LookupSymbol) {
        storage.append(file, listOf(symbol))
    }

    operator fun get(key: File): Collection<LookupSymbol>? = storage[key]

    operator fun set(key: File, symbols: Set<LookupSymbol>) {
        storage[key] = symbols
    }

    fun remove(key: File) {
        storage.remove(key)
    }

    val keys: Collection<File>
        get() = storage.keys
}

object FileKeyDescriptor : KeyDescriptor<File> {
    override fun read(input: DataInput): File {
        return File(IOUtil.readString(input))
    }

    override fun save(output: DataOutput, value: File) {
        IOUtil.writeString(value.path, output)
    }

    override fun getHashCode(value: File): Int = value.hashCode()

    override fun isEqual(val1: File, val2: File): Boolean = val1 == val2
}

object LookupSymbolExternalizer : DataExternalizer<LookupSymbol> {
    override fun read(input: DataInput): LookupSymbol = LookupSymbol(IOUtil.readString(input), IOUtil.readString(input))

    override fun save(output: DataOutput, value: LookupSymbol) {
        IOUtil.writeString(value.name, output)
        IOUtil.writeString(value.scope, output)
    }
}

object FileExternalizer : DataExternalizer<File> {
    override fun read(input: DataInput): File = File(IOUtil.readString(input))

    override fun save(output: DataOutput, value: File) {
        IOUtil.writeString(value.path, output)
    }
}

class FileToFilesMap(storageFile: File) : BasicMap<File, Collection<File>>(storageFile, FileKeyDescriptor, CollectionExternalizer(FileExternalizer, { HashSet() })) {

    operator fun get(key: File): Collection<File>? = storage[key]

    operator fun set(key: File, value: Collection<File>) {
        storage[key] = value
    }

    override fun dumpKey(key: File): String = key.path

    override fun dumpValue(value: Collection<File>) =
            value.dumpCollection()

    // TODO: remove values.
    fun remove(key: File) {
        storage.remove(key)
    }

    val keys: Collection<File>
        get() = storage.keys
}

object symbolCollector : KSDefaultVisitor<(LookupSymbol) -> Unit, Unit>() {
    override fun defaultHandler(node: KSNode, collect: (LookupSymbol) -> Unit) = Unit

    override fun visitDeclaration(declaration: KSDeclaration, collect: (LookupSymbol) -> Unit) {
        if (declaration.isPrivate())
            return

        val name = declaration.simpleName.asString()
        val scope = declaration.qualifiedName?.asString()?.let { it.substring(0, Math.max(it.length - name.length - 1, 0))} ?: return
        collect(LookupSymbol(name, scope))
    }

    override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, collect: (LookupSymbol) -> Unit) {
        // Local declarations aren't visible to other files / classes.
        if (declarationContainer is KSFunctionDeclaration)
            return

        declarationContainer.declarations.forEach {
            it.accept(this, collect)
        }
    }
}

internal class RelativeFileToPathConverter(val baseDir: File) : FileToPathConverter {
    override fun toPath(file: File): String = file.path
    override fun toFile(path: String): File = File(path).relativeTo(baseDir)
}

class IncrementalContext(
        private val options: KspOptions,
        private val ktFiles: Collection<KtFile>,
        private val componentProvider: ComponentProvider,
        private val anyChangesWildcard: File,
        private val isIncremental: Boolean
) {
    // Symbols defined in changed files. This is used to update symbolsMap in the end.
    private val updatedSymbols = MultiMap.createSet<File, LookupSymbol>()

    // Symbols defined in each file. This is
    private val symbolsMap = FileToSymbolsMap(File(options.cachesDir, "symbols"))

    private val ksFiles = ktFiles.map { KSFileImpl.getCached(it) }

    private val baseDir = options.projectBaseDir

    private val modified = options.knownModified.map{ it.relativeTo(baseDir) }.toSet()
    private val removed = options.knownRemoved.map { it.relativeTo(baseDir) }.toSet()

    private val lookupTracker: LookupTracker = componentProvider.get()
    private val lookupCacheDir = options.cachesDir
    private val PATH_CONVERTER = RelativeFileToPathConverter(baseDir)
    private val lookupCache = LookupStorage(lookupCacheDir, PATH_CONVERTER)

    private val sourceToOutputsMap = FileToFilesMap(File(options.cachesDir, "sourceToOutputs"))

    private fun String.toRelativeFile() = File(this).relativeTo(baseDir)
    private val KSFile.relativeFile
        get() = filePath.toRelativeFile()

    private fun cleanIncrementalCache() {
        options.cachesDir.deleteRecursively()
    }

    private fun collectDefinedSymbols() {
        ksFiles.forEach { file ->
            file.accept(symbolCollector) {
                updatedSymbols.putValue(file.relativeFile, it)
            }
        }
    }

    fun updateLookupCache(dirtyFiles: Collection<File>) {
        if (lookupTracker is LookupTrackerImpl) {
            lookupCache.update(lookupTracker, dirtyFiles, options.knownRemoved)
            lookupCache.flush(false)
            lookupCache.close()
        }
    }

    private fun calcDirtySetByDeps(): Set<File> {
        val changedSyms = mutableSetOf<LookupSymbol>()

        // Parse and add newly defined symbols in modified files.
        ksFiles.filter { it.relativeFile in modified }.forEach { file ->
            file.accept(symbolCollector) {
                updatedSymbols.putValue(file.relativeFile, it)
                changedSyms.add(it)
            }
        }

        // Add previously defined symbols in removed and modified files
        ksFiles.filter { it.relativeFile in removed || it.relativeFile in modified }.forEach { file ->
            symbolsMap[file.relativeFile]?.let {
                changedSyms.addAll(it)
            }
        }

        // For each changed symbol, either changed, modified or removed, invalidate files that looked them up, recursively.
        val invalidator = DepInvalidator(lookupCache, symbolsMap, ksFiles.filter { it.relativeFile in removed || it.relativeFile in modified }.map { it.relativeFile })
        changedSyms.forEach {
            invalidator.invalidate(it)
        }

        return invalidator.visitedFiles
    }

    // Propagate dirtiness by source-output maps.
    private fun calcDirtySetByOutputs(sourceToOutputs: FileToFilesMap,
                                      initialSet: Set<File>): Set<File> {
        val outputToSources = mutableMapOf<File, MutableSet<File>>()
        sourceToOutputs.keys.forEach { source ->
            if (source != anyChangesWildcard) {
                sourceToOutputs[source]!!.forEach { output ->
                    outputToSources.getOrPut(output) { mutableSetOf() }.add(source)
                }
            }
        }
        val visited = mutableSetOf<File>()
        fun visit(dirty: File) {
            if (dirty in visited)
                return

            visited.add(dirty)
            sourceToOutputs[dirty]?.forEach {
                outputToSources[it]?.forEach {
                    visit(it)
                }
            }
        }

        initialSet.forEach {
            visit(it)
        }

        return visited
    }

    fun logDirtyFilesByDeps(dirtyFiles: Collection<File>) {
        if (!options.incrementalLog)
            return

        val logFile = File(options.projectBaseDir, "build/kspDirtySetByDeps.log")
        logFile.appendText("All Files\n")
        ksFiles.forEach { logFile.appendText("  ${it.relativeFile}\n") }
        logFile.appendText("Modified\n")
        options.knownModified.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("Removed\n")
        options.knownRemoved.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("Dirty\n")
        dirtyFiles.forEach { logFile.appendText("  ${it}\n") }
        val percentage = "%.2f".format(dirtyFiles.size.toDouble() / ksFiles.size.toDouble() * 100)
        logFile.appendText("\nDirty / All: $percentage%\n\n")
    }

    fun logDirtyFilesByOutputs(dirtyFiles: Collection<File>) {
        if (!options.incrementalLog)
            return

        val allOutputs = mutableSetOf<File>()
        val validOutputs = mutableSetOf<File>()
        sourceToOutputsMap.keys.forEach { source ->
            val outputs = sourceToOutputsMap[source]!!
            if (source !in removed)
                validOutputs.addAll(outputs)
            allOutputs.addAll(outputs)
        }
        val outputsToRemove = allOutputs - validOutputs

        val logFile = File(options.projectBaseDir, "build/kspDirtySetByOutputs.log")
        logFile.appendText("Dirty sources\n")
        dirtyFiles.forEach { logFile.appendText("  $it\n") }
        logFile.appendText("Outputs to remove\n")
        outputsToRemove.forEach { logFile.appendText("  $it\n")}
        logFile.appendText("\n")
    }

    fun logSourceToOutputs() {
        if (!options.incrementalLog)
            return

        val logFile = File(options.projectBaseDir, "build/kspSourceToOutputs.log")
        logFile.appendText("All outputs\n")
        sourceToOutputsMap.keys.forEach { source ->
            logFile.appendText("  $source:\n")
            sourceToOutputsMap[source]!!.forEach { output ->
                logFile.appendText("    $output\n")
            }
        }
        logFile.appendText("\n")
    }

    fun logDirtyFiles(files: List<KSFile>) {
        if (!options.incrementalLog)
            return

        val logFile = File(options.projectBaseDir, "build/kspDirtySet.log")
        logFile.appendText("Dirty:\n")
        files.forEach {
            logFile.appendText("  ${it.relativeFile}\n")
        }
        logFile.appendText("\n")
    }

    // Beware: no side-effects here; Caches should only be touched in updateCaches.
    fun calcDirtyFiles(): Collection<KSFile> {
        if (isIncremental) {
            val dirtyFilesByDeps = calcDirtySetByDeps()

            logDirtyFilesByDeps(dirtyFilesByDeps)

            // modified can be seen as removed + new. Therefore the following check doesn't work:
            //   if (modified.any { it !in sourceToOutputsMap.keys }) ...
            val dirtyFilesByOutputs = if (modified.isNotEmpty()) {
                calcDirtySetByOutputs(sourceToOutputsMap, dirtyFilesByDeps + removed + anyChangesWildcard)
            } else {
                calcDirtySetByOutputs(sourceToOutputsMap, dirtyFilesByDeps + removed)
            }

            logDirtyFilesByOutputs(dirtyFilesByOutputs)
            logDirtyFiles(ksFiles.filter { it.relativeFile in dirtyFilesByOutputs })
            return ksFiles.filter { it.relativeFile in dirtyFilesByOutputs }
        } else {
            cleanIncrementalCache()
            collectDefinedSymbols()
            logDirtyFiles(ksFiles)
            return ksFiles
        }
    }

    fun updateSourceToOutputs(dirtyFiles: Collection<File>, outputs: Set<File>, sourceToOutputs: Map<File, Set<File>>) {
        // Prune deleted sources in source-to-outputs map.
        removed.forEach {
            sourceToOutputsMap.remove(it)
        }

        // TODO: Should unspecified outputs be associated to all files by default?
        //       If so, maybe simply disable incremental processing once detected that.

        val mutableSourceToOutputs = mutableMapOf<File, MutableSet<File>>().apply {
            sourceToOutputs.forEach {
                set(it.key, it.value.toMutableSet())
            }
        }

        // Associate unassociated outputs to ALL FILES.
        val associated = sourceToOutputs.values.flatten().toSet()
        val unassociated = outputs.filterNot { it in associated }
        mutableSourceToOutputs.getOrPut(anyChangesWildcard) { mutableSetOf() }.addAll(unassociated)
        ksFiles.forEach { ksFile ->
            mutableSourceToOutputs.getOrPut( ksFile.relativeFile ) { mutableSetOf() }.addAll(unassociated)
        }

        // Merge source-to-outputs map from those reprocessed.
        dirtyFiles.forEach { source ->
            mutableSourceToOutputs[source]?.let { sourceToOutputsMap[source] = it} ?: sourceToOutputsMap.remove(source)
        }

        logSourceToOutputs()

        sourceToOutputsMap.flush(false)
        sourceToOutputsMap.close()
    }

    // TODO: Recover if processing failed.
    fun updateOutputs(outputs: Set<File>, cleanOutputs: Collection<File>) {
        val outRoot = options.kspOutputDir
        val bakRoot = File(options.cachesDir, "backups")

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
            generated.abs().copyTo(generated.bak(), overwrite = true)
        }

        // Restore non-dirty outputs
        cleanOutputs.forEach { dst ->
            if (dst !in outputs)
                dst.bak().copyTo(dst.abs(), overwrite = false)
        }
    }

    // TODO: Don't do anything if processing failed.
    fun updateCaches(dirtyFiles: Collection<File>, outputs: Set<File>, sourceToOutputs: Map<File, Set<File>>) {
        updateSourceToOutputs(dirtyFiles, outputs, sourceToOutputs)
        updateLookupCache(dirtyFiles)

        // Update symbolsMap
        if (isIncremental) {
            // Update symbol caches from modified files.
            options.knownModified.forEach {
                symbolsMap.set(it, updatedSymbols[it].toSet())
            }

            // Remove symbol caches from removed files.
            options.knownRemoved.forEach {
                symbolsMap.remove(it)
            }
        } else {
            symbolsMap.clean()
            updatedSymbols.keySet().forEach {
                symbolsMap.set(it, updatedSymbols[it].toSet())
            }
        }
        symbolsMap.flush(false)
        symbolsMap.close()
    }

    fun updateCachesAndOutputs(dirtyFiles: Collection<KSFile>, outputs: Set<File>, sourceToOutputs: Map<File, Set<File>>) {
        val cleanOutputs = mutableSetOf<File>()
        val dirtyFilePaths = dirtyFiles.map { it.relativeFile }
        sourceToOutputsMap.keys.forEach { source ->
            if (source !in dirtyFilePaths && source != anyChangesWildcard)
                cleanOutputs.addAll(sourceToOutputsMap[source]!!)
        }

        updateCaches(dirtyFilePaths, outputs, sourceToOutputs)
        updateOutputs(outputs, cleanOutputs)

    }
}

internal class DepInvalidator(
        private val lookupCache: LookupStorage,
        private val symbolsMap: FileToSymbolsMap,
        changedFiles: List<File>
) {
    private val visitedSyms = mutableSetOf<LookupSymbol>()
    val visitedFiles = mutableSetOf<File>().apply { addAll(changedFiles) }

    fun invalidate(sym: LookupSymbol) {
        if (sym in visitedSyms)
            return
        visitedSyms.add(sym)
        lookupCache.get(sym).forEach {
            invalidate(File(it))
        }
    }

    private fun invalidate(file: File) {
        if (file in visitedFiles)
            return
        visitedFiles.add(file)
        symbolsMap[file]!!.forEach {
            invalidate(it)
        }
    }
}
