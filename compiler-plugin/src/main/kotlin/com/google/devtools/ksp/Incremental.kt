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

package com.google.devtools.ksp

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.java.KSFunctionDeclarationJavaImpl
import com.google.devtools.ksp.symbol.impl.java.KSPropertyDeclarationJavaImpl
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.util.containers.MultiMap
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.incremental.storage.BasicMap
import org.jetbrains.kotlin.incremental.storage.CollectionExternalizer
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

abstract class PersistentMap<K : Comparable<K>, V>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<K>,
    valueExternalizer: DataExternalizer<V>,
) : BasicMap<K, V>(storageFile, keyDescriptor, valueExternalizer) {
    abstract operator fun get(key: K): V?
    abstract operator fun set(key: K, value: V)
    abstract fun remove(key: K)
}

class FileToSymbolsMap(storageFile: File) : PersistentMap<File, Collection<LookupSymbol>>(
    storageFile,
    FileKeyDescriptor,
    CollectionExternalizer(LookupSymbolExternalizer, { HashSet() })
) {
    override fun dumpKey(key: File): String = key.toString()

    override fun dumpValue(value: Collection<LookupSymbol>): String = value.toString()

    fun add(file: File, symbol: LookupSymbol) {
        storage.append(file, listOf(symbol))
    }

    override operator fun get(key: File): Collection<LookupSymbol>? = storage[key]

    override operator fun set(key: File, symbols: Collection<LookupSymbol>) {
        storage[key] = symbols
    }

    override fun remove(key: File) {
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

class FileToFilesMap(storageFile: File) : PersistentMap<File, Collection<File>>(
    storageFile,
    FileKeyDescriptor,
    CollectionExternalizer(FileExternalizer, { HashSet() })
) {

    override operator fun get(key: File): Collection<File>? = storage[key]

    override operator fun set(key: File, value: Collection<File>) {
        storage[key] = value
    }

    override fun dumpKey(key: File): String = key.path

    override fun dumpValue(value: Collection<File>) =
        value.dumpCollection()

    override fun remove(key: File) {
        storage.remove(key)
    }

    val keys: Collection<File>
        get() = storage.keys
}

object symbolCollector : KSDefaultVisitor<(LookupSymbol) -> Unit, Unit>() {
    override fun defaultHandler(node: KSNode, data: (LookupSymbol) -> Unit) = Unit

    override fun visitDeclaration(declaration: KSDeclaration, data: (LookupSymbol) -> Unit) {
        if (declaration.isPrivate())
            return

        val name = declaration.simpleName.asString()
        val scope =
            declaration.qualifiedName?.asString()?.let { it.substring(0, Math.max(it.length - name.length - 1, 0)) }
                ?: return
        data(LookupSymbol(name, scope))
    }

    override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: (LookupSymbol) -> Unit) {
        // Local declarations aren't visible to other files / classes.
        if (declarationContainer is KSFunctionDeclaration)
            return

        declarationContainer.declarations.forEach {
            it.accept(this, data)
        }
    }
}

internal class RelativeFileToPathConverter(val baseDir: File) : FileToPathConverter {
    override fun toPath(file: File): String = file.path
    override fun toFile(path: String): File = File(path).relativeTo(baseDir)
}

class IncrementalContext(
    private val options: KspOptions,
    private val componentProvider: ComponentProvider,
    private val anyChangesWildcard: File,
) {
    // Symbols defined in changed files. This is used to update symbolsMap in the end.
    private val updatedSymbols = MultiMap.createSet<File, LookupSymbol>()

    // Sealed classes / interfaces on which `getSealedSubclasses` is invoked.
    // This is used to update sealedMap in the end.
    private val updatedSealed = MultiMap.createSet<File, LookupSymbol>()

    // Sealed classes / interfaces on which `getSealedSubclasses` is invoked.
    // This is saved across processing.
    private val sealedMap = FileToSymbolsMap(File(options.cachesDir, "sealed"))

    // Symbols defined in each file. This is saved across processing.
    private val symbolsMap = FileToSymbolsMap(File(options.cachesDir, "symbols"))

    private val cachesUpToDateFile = File(options.cachesDir, "caches.uptodate")
    private val rebuild = !cachesUpToDateFile.exists()

    private val baseDir = options.projectBaseDir

    private val logsDir = File(options.cachesDir, "logs").apply { mkdirs() }
    private val buildTime = Date().time

    private val modified = options.knownModified.map { it.relativeTo(baseDir) }.toSet()
    private val removed = options.knownRemoved.map { it.relativeTo(baseDir) }.toSet()

    private val lookupTracker: LookupTracker = componentProvider.get()

    // Disable incremental processing if somehow DualLookupTracker failed to be registered.
    // This may happen when a platform hasn't support incremental compilation yet. E.g, Common / Metadata.
    private val isIncremental = options.incremental && lookupTracker is DualLookupTracker
    private val PATH_CONVERTER = RelativeFileToPathConverter(baseDir)

    private val symbolLookupTracker = (lookupTracker as? DualLookupTracker)?.symbolTracker ?: LookupTracker.DO_NOTHING
    private val symbolLookupCacheDir = File(options.cachesDir, "symbolLookups")
    private val symbolLookupCache = LookupStorage(symbolLookupCacheDir, PATH_CONVERTER)

    // TODO: rewrite LookupStorage to share file-to-id, etc.
    private val classLookupTracker = (lookupTracker as? DualLookupTracker)?.classTracker ?: LookupTracker.DO_NOTHING
    private val classLookupCacheDir = File(options.cachesDir, "classLookups")
    private val classLookupCache = LookupStorage(classLookupCacheDir, PATH_CONVERTER)

    private val sourceToOutputsMap = FileToFilesMap(File(options.cachesDir, "sourceToOutputs"))

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

        sourceToOutputsMap.remove(removedOutputsKey)
    }

    private fun updateLookupCache(dirtyFiles: Collection<File>) {
        symbolLookupCache.update(symbolLookupTracker, dirtyFiles, options.knownRemoved)
        symbolLookupCache.flush(false)
        symbolLookupCache.close()

        classLookupCache.update(classLookupTracker, dirtyFiles, options.knownRemoved)
        classLookupCache.flush(false)
        classLookupCache.close()
    }

    private fun logSourceToOutputs(outputs: Set<File>, sourceToOutputs: Map<File, Set<File>>) {
        if (!options.incrementalLog)
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
        if (!options.incrementalLog)
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
        options.changedClasses.forEach { logFile.appendText("  $it\n") }
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

        val newSyms = mutableSetOf<LookupSymbol>()

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

        val dirtyFilesBySealed = sealedMap.keys.flatMap { sealedMap[it]!! }.flatMap {
            symbolLookupCache.get(it).map { File(it) }
        }

        // Calculate dirty files by dirty classes in CP.
        val dirtyFilesByCP = options.changedClasses.flatMap { fqn ->
            val name = fqn.substringAfterLast('.')
            val scope = fqn.substringBeforeLast('.', "<anonymous>")
            classLookupCache.get(LookupSymbol(name, scope)).map { File(it) } +
                symbolLookupCache.get(LookupSymbol(name, scope)).map { File(it) }
        }.toSet()

        // output files that exist in CURR~2 but not in CURR~1
        val removedOutputs = sourceToOutputsMap.get(removedOutputsKey) ?: emptyList()

        val noSourceFiles = options.changedClasses.map { fqn ->
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
        if (modified.isNotEmpty() || options.changedClasses.isNotEmpty()) {
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

    private fun updateSourceToOutputs(
        dirtyFiles: Collection<File>,
        outputs: Set<File>,
        sourceToOutputs: Map<File, Set<File>>,
        removedOutputs: List<File>,
    ) {
        // Prune deleted sources in source-to-outputs map.
        removed.forEach {
            sourceToOutputsMap.remove(it)
        }

        dirtyFiles.filterNot { sourceToOutputs.containsKey(it) }.forEach {
            sourceToOutputsMap.remove(it)
        }

        removedOutputs.forEach {
            sourceToOutputsMap.remove(it)
        }
        sourceToOutputsMap[removedOutputsKey] = removedOutputs

        // Update source-to-outputs map from those reprocessed.
        sourceToOutputs.forEach { src, outs ->
            sourceToOutputsMap[src] = outs
        }

        logSourceToOutputs(outputs, sourceToOutputs)

        sourceToOutputsMap.flush(false)
    }

    private fun updateOutputs(outputs: Set<File>, cleanOutputs: Collection<File>) {
        val outRoot = options.kspOutputDir
        val bakRoot = File(options.cachesDir, "backups")

        fun File.abs() = File(baseDir, path)
        fun File.bak() = File(bakRoot, abs().toRelativeString(outRoot))

        // Copy recursively, including last-modified-time of file and its parent dirs.
        //
        // `java.nio.file.Files.copy(path1, path2, options...)` keeps last-modified-time (if supported) according to
        // https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html
        fun copy(src: File, dst: File, overwrite: Boolean) {
            if (!dst.parentFile.exists())
                copy(src.parentFile, dst.parentFile, false)
            if (overwrite) {
                Files.copy(
                    src.toPath(),
                    dst.toPath(),
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } else {
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            }
        }

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
            copy(generated.abs(), generated.bak(), true)
        }

        // Restore non-dirty outputs
        cleanOutputs.forEach { dst ->
            if (dst !in outputs) {
                copy(dst.bak(), dst.abs(), false)
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
        fun <K : Comparable<K>, V> update(m: PersistentMap<K, Collection<V>>, u: MultiMap<K, V>) {
            // Update symbol caches from modified files.
            u.keySet().forEach {
                m.set(it, u[it].toSet())
            }
        }

        fun <K : Comparable<K>, V> remove(m: PersistentMap<K, Collection<V>>, removedKeys: Collection<K>) {
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
            symbolsMap.clean()
            update(symbolsMap, updatedSymbols)

            sealedMap.clean()
            update(sealedMap, updatedSealed)
        }
        symbolsMap.flush(false)
        symbolsMap.close()
        sealedMap.flush(false)
        sealedMap.close()
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
            symbolsMap.close()
            sealedMap.close()
            symbolLookupCache.close()
            classLookupCache.close()
            sourceToOutputsMap.close()
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

        val dirtyFilePaths = dirtyFiles.map { it.relativeFile }

        updateCaches(dirtyFilePaths, outputs, sourceToOutputs)

        val cleanOutputs = mutableSetOf<File>()
        sourceToOutputsMap.keys.forEach { source ->
            if (source !in dirtyFilePaths && source != anyChangesWildcard && source != removedOutputsKey)
                cleanOutputs.addAll(sourceToOutputsMap[source]!!)
        }
        sourceToOutputsMap.close()
        updateOutputs(outputs, cleanOutputs)

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
            symbolLookupTracker.record(path, Position.NO_POSITION, scope, ScopeKind.PACKAGE, name)

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

    // Record a *leaf* type reference. This doesn't address type arguments.
    private fun recordLookup(ref: PsiClassReferenceType, def: PsiClass) {
        val psiFile = ref.reference.containingFile as? PsiJavaFile ?: return
        // A type parameter doesn't have qualified name.
        //
        // Note that bounds of type parameters, or other references in classes,
        // are not addressed recursively here. They are recorded in other places
        // with more contexts, when necessary.
        def.qualifiedName?.let { recordLookup(psiFile, it) }
    }

    // Record a type reference, including its type arguments.
    fun recordLookup(ref: PsiType) {
        when (ref) {
            is PsiArrayType -> recordLookup(ref.componentType)
            is PsiClassReferenceType -> {
                val def = ref.resolve() ?: return
                recordLookup(ref, def)
                // in case the corresponding KotlinType is passed through ways other than KSTypeReferenceJavaImpl
                ref.typeArguments().forEach {
                    if (it is PsiType) {
                        recordLookup(it)
                    }
                }
            }
            is PsiWildcardType -> ref.bound?.let { recordLookup(it) }
        }
    }

    // Record all references to super types (if they are written in Java) of a given type,
    // in its type hierarchy.
    fun recordLookupWithSupertypes(kotlinType: KotlinType) {
        (listOf(kotlinType) + kotlinType.supertypes()).mapNotNull {
            it.constructor.declarationDescriptor?.findPsi() as? PsiClass
        }.forEach {
            it.superTypes.forEach {
                recordLookup(it)
            }
        }
    }

    // Record all type references in a Java field.
    private fun recordLookupForJavaField(psi: PsiField) {
        recordLookup(psi.type)
    }

    // Record all type references in a Java method.
    private fun recordLookupForJavaMethod(psi: PsiMethod) {
        psi.parameterList.parameters.forEach {
            recordLookup(it.type)
        }
        psi.returnType?.let { recordLookup(it) }
        psi.typeParameters.forEach {
            it.bounds.mapNotNull { it as? PsiType }.forEach {
                recordLookup(it)
            }
        }
    }

    // Record all type references in a KSDeclaration
    fun recordLookupForDeclaration(declaration: KSDeclaration) {
        when (declaration) {
            is KSPropertyDeclarationJavaImpl -> recordLookupForJavaField(declaration.psi)
            is KSFunctionDeclarationJavaImpl -> recordLookupForJavaMethod(declaration.psi)
        }
    }

    // Record all type references in a CallableMemberDescriptor
    fun recordLookupForCallableMemberDescriptor(descriptor: CallableMemberDescriptor) {
        val psi = descriptor.findPsi()
        when (psi) {
            is PsiMethod -> recordLookupForJavaMethod(psi)
            is PsiField -> recordLookupForJavaField(psi)
        }
    }

    // Record references from all declared functions in the type hierarchy of the given class.
    // TODO: optimization: filter out inaccessible members
    fun recordLookupForGetAllFunctions(descriptor: ClassDescriptor) {
        recordLookupForGetAll(descriptor) {
            it.methods.forEach {
                recordLookupForJavaMethod(it)
            }
        }
    }

    // Record references from all declared fields in the type hierarchy of the given class.
    // TODO: optimization: filter out inaccessible members
    fun recordLookupForGetAllProperties(descriptor: ClassDescriptor) {
        recordLookupForGetAll(descriptor) {
            it.fields.forEach {
                recordLookupForJavaField(it)
            }
        }
    }

    fun recordLookupForGetAll(descriptor: ClassDescriptor, doChild: (PsiClass) -> Unit) {
        (descriptor.getAllSuperclassesWithoutAny() + descriptor).mapNotNull {
            it.findPsi() as? PsiClass
        }.forEach { psiClass ->
            psiClass.superTypes.forEach {
                recordLookup(it)
            }
            doChild(psiClass)
        }
    }

    fun recordGetSealedSubclasses(classDeclaration: KSClassDeclaration) {
        val name = classDeclaration.simpleName.asString()
        val scope = classDeclaration.qualifiedName?.asString()
            ?.let { it.substring(0, Math.max(it.length - name.length - 1, 0)) } ?: return
        updatedSealed.putValue(classDeclaration.containingFile!!.relativeFile, LookupSymbol(name, scope))
    }

    // Debugging and testing only.
    internal fun dumpLookupRecords(): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        (symbolLookupTracker as LookupTrackerImpl).lookups.entrySet().forEach { e ->
            val key = "${e.key.scope}.${e.key.name}"
            map[key] = e.value.map { PATH_CONVERTER.toFile(it).path }
        }
        return map
    }
}

internal class DirtinessPropagator(
    private val lookupCache: LookupStorage,
    private val symbolsMap: FileToSymbolsMap,
    private val sourceToOutputs: FileToFilesMap,
    private val anyChangesWildcard: File,
    private val removedOutputsKey: File
) {
    private val visitedFiles = mutableSetOf<File>()
    private val visitedSyms = mutableSetOf<LookupSymbol>()

    private val outputToSources = mutableMapOf<File, MutableSet<File>>().apply {
        sourceToOutputs.keys.forEach { source ->
            if (source != anyChangesWildcard && source != removedOutputsKey) {
                sourceToOutputs[source]!!.forEach { output ->
                    getOrPut(output) { mutableSetOf() }.add(source)
                }
            }
        }
    }

    private fun visit(sym: LookupSymbol) {
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
