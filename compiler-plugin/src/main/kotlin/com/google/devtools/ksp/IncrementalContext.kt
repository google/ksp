package com.google.devtools.ksp

import com.intellij.util.containers.MultiMap
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.LookupTrackerImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import org.jetbrains.kotlin.incremental.update
import java.io.DataInput
import java.io.DataOutput
import java.io.File

class IncrementalContext(
    options: KspOptions,
    componentProvider: ComponentProvider,
    anyChangesWildcard: File,
) : IncrementalContextBase(
    anyChangesWildcard,
    options.incrementalLog,
    options.projectBaseDir,
    options.cachesDir,
    options.kspOutputDir,
    options.knownModified,
    options.knownRemoved,
    options.changedClasses,
) {
    private val lookupTracker: LookupTracker = componentProvider.get()

    // Disable incremental processing if somehow DualLookupTracker failed to be registered.
    // This may happen when a platform hasn't support incremental compilation yet. E.g, Common / Metadata.
    override val isIncremental = options.incremental && lookupTracker is DualLookupTracker

    private val PATH_CONVERTER = RelativeFileToPathConverter(baseDir)
    private val icContext = IncrementalCompilationContext(PATH_CONVERTER, PATH_CONVERTER, true)

    private val symbolLookupCacheDir = File(cachesDir, "symbolLookups")
    override val symbolLookupTracker =
        LookupTrackerWrapperImpl((lookupTracker as? DualLookupTracker)?.symbolTracker ?: LookupTracker.DO_NOTHING)
    override val symbolLookupCache = LookupStorageWrapperImpl(LookupStorage(symbolLookupCacheDir, icContext))

    // TODO: rewrite LookupStorage to share file-to-id, etc.
    private val classLookupCacheDir = File(cachesDir, "classLookups")
    override val classLookupTracker =
        LookupTrackerWrapperImpl((lookupTracker as? DualLookupTracker)?.classTracker ?: LookupTracker.DO_NOTHING)
    override val classLookupCache = LookupStorageWrapperImpl(LookupStorage(classLookupCacheDir, icContext))

    override val sealedMap = FileToSymbolsMap(File(cachesDir, "sealed"), LookupSymbolExternalizer)

    override val symbolsMap = FileToSymbolsMap(File(cachesDir, "symbols"), LookupSymbolExternalizer)

    // Debugging and testing only.
    fun dumpLookupRecords(): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        symbolLookupTracker.lookups.entrySet().forEach { e ->
            val key = "${e.key.scope}.${e.key.name}"
            map[key] = e.value.map { PATH_CONVERTER.toFile(it).path }
        }
        return map
    }
}

class LookupTrackerWrapperImpl(val lookupTracker: LookupTracker) : LookupTrackerWrapper {
    override val lookups: MultiMap<LookupSymbolWrapper, String>
        get() = MultiMap<LookupSymbolWrapper, String>().also { wrapper ->
            (lookupTracker as LookupTrackerImpl).lookups.entrySet().forEach { e ->
                wrapper.putValues(LookupSymbolWrapper(e.key.name, e.key.scope), e.value)
            }
        }

    override fun record(filePath: String, scopeFqName: String, name: String) {
        lookupTracker.record(filePath, Position.NO_POSITION, scopeFqName, ScopeKind.PACKAGE, name)
    }
}

class LookupStorageWrapperImpl(
    val impl: LookupStorage
) : LookupStorageWrapper {
    override fun get(lookupSymbol: LookupSymbolWrapper): Collection<String> =
        impl.get(LookupSymbol(lookupSymbol.name, lookupSymbol.scope))

    override fun update(
        lookupTracker: LookupTrackerWrapper,
        filesToCompile: Iterable<File>,
        removedFiles: Iterable<File>
    ) {
        impl.update((lookupTracker as LookupTrackerWrapperImpl).lookupTracker, filesToCompile, removedFiles)
    }

    override fun removeLookupsFrom(files: Sequence<File>) = impl.removeLookupsFrom(files)

    override fun close() = impl.close()

    override fun flush() = impl.flush()
}

object LookupSymbolExternalizer : DataExternalizer<LookupSymbolWrapper> {
    override fun read(input: DataInput): LookupSymbolWrapper =
        LookupSymbolWrapper(IOUtil.readString(input), IOUtil.readString(input))

    override fun save(output: DataOutput, value: LookupSymbolWrapper) {
        IOUtil.writeString(value.name, output)
        IOUtil.writeString(value.scope, output)
    }
}

internal class RelativeFileToPathConverter(val baseDir: File) : FileToPathConverter {
    override fun toPath(file: File): String = file.path
    override fun toFile(path: String): File = File(path).relativeTo(baseDir)
}
