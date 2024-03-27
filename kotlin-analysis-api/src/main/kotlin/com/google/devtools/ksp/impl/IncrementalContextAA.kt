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

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.common.FileToSymbolsMap
import com.google.devtools.ksp.common.IncrementalContextBase
import com.google.devtools.ksp.common.LookupStorageWrapper
import com.google.devtools.ksp.common.LookupSymbolWrapper
import com.google.devtools.ksp.common.LookupTrackerWrapper
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSFunctionDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.typeArguments
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiJavaFile
import com.intellij.util.containers.MultiMap
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.types.KtCapturedType
import org.jetbrains.kotlin.analysis.api.types.KtDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KtDynamicType
import org.jetbrains.kotlin.analysis.api.types.KtErrorType
import org.jetbrains.kotlin.analysis.api.types.KtFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KtIntegerLiteralType
import org.jetbrains.kotlin.analysis.api.types.KtIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
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

class IncrementalContextAA(
    override val isIncremental: Boolean,
    lookupTracker: LookupTracker,
    anyChangesWildcard: File,
    incrementalLog: Boolean,
    baseDir: File,
    cachesDir: File,
    kspOutputDir: File,
    knownModified: List<File>,
    knownRemoved: List<File>,
    changedClasses: List<String>,
) : IncrementalContextBase(
    anyChangesWildcard,
    incrementalLog,
    baseDir,
    cachesDir,
    kspOutputDir,
    knownModified,
    knownRemoved,
    changedClasses,
) {
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

    private fun recordWithArgs(type: KtType, file: PsiJavaFile) {
        type.typeArguments().forEach {
            it.type?.let { recordWithArgs(it, file) }
        }
        when (type) {
            is KtNonErrorClassType -> {
                val fqn = type.classId.asFqNameString()
                recordLookup(file, fqn)
            }
            is KtFlexibleType -> {
                recordWithArgs(type.lowerBound, file)
                recordWithArgs(type.upperBound, file)
            }
            is KtIntersectionType -> {
                type.conjuncts.forEach {
                    recordWithArgs(it, file)
                }
            }
            is KtCapturedType -> {
                type.projection.type?.let {
                    recordWithArgs(it, file)
                }
            }
            is KtDefinitelyNotNullType -> {
                recordWithArgs(type.original, file)
            }
            is KtErrorType, is KtIntegerLiteralType, is KtDynamicType, is KtTypeParameterType -> {}
        }
    }

    fun recordLookup(type: KtType, context: KSNode?) {
        val file = (context?.containingFile as? KSFileJavaImpl)?.psi ?: return

        recordWithArgs(type, file)
    }

    private fun recordLookupForDeclaration(symbol: KtSymbol, file: PsiJavaFile) {
        when (symbol) {
            is KtJavaFieldSymbol -> recordWithArgs(symbol.returnType, file)
            is KtPropertySymbol -> recordWithArgs(symbol.returnType, file)
            is KtFunctionLikeSymbol -> {
                recordWithArgs(symbol.returnType, file)
                symbol.valueParameters.forEach { recordWithArgs(it.returnType, file) }
                symbol.typeParameters.forEach {
                    it.upperBounds.forEach {
                        recordWithArgs(it, file)
                    }
                }
            }
        }
    }

    fun recordLookupForPropertyOrMethod(declaration: KSDeclaration) {
        val file = (declaration.containingFile as? KSFileJavaImpl)?.psi ?: return

        val symbol = when (declaration) {
            is KSPropertyDeclarationJavaImpl -> declaration.ktJavaFieldSymbol
            is KSPropertyDeclarationImpl -> declaration.ktPropertySymbol
            is KSFunctionDeclarationImpl -> declaration.ktFunctionSymbol
            else -> return
        }
        recordLookupForDeclaration(symbol, file)
    }

    internal fun recordLookupForGetAll(supers: List<KtType>, predicate: (KtSymbol) -> Boolean) {
        val visited: MutableSet<KtType> = mutableSetOf()
        analyze {
            supers.forEach {
                recordLookupWithSupertypes(it, visited) { type, file ->
                    if (type is KtNonErrorClassType) {
                        (type.classSymbol as? KtSymbolWithMembers)?.let {
                            val declared = it.getDeclaredMemberScope().getAllSymbols() +
                                it.getStaticDeclaredMemberScope().getAllSymbols()
                            declared.forEach {
                                if (predicate(it))
                                    recordLookupForDeclaration(it, file)
                            }
                        }
                    }
                }
            }
        }
    }

    private val KtSymbol.psiJavaFile: PsiJavaFile?
        get() = psi?.containingFile as? PsiJavaFile

    private val KtType.psiJavaFiles: List<PsiJavaFile>
        get() {
            return when (this) {
                is KtNonErrorClassType -> classSymbol.psiJavaFile?.let { listOf(it) } ?: emptyList()
                is KtFlexibleType -> lowerBound.psiJavaFiles + upperBound.psiJavaFiles
                is KtIntersectionType -> conjuncts.flatMap { it.psiJavaFiles }
                is KtCapturedType -> projection.type?.psiJavaFiles ?: emptyList()
                is KtDefinitelyNotNullType -> original.psiJavaFiles
                is KtErrorType, is KtIntegerLiteralType, is KtDynamicType, is KtTypeParameterType -> emptyList()
            }
        }

    fun recordLookupWithSupertypes(ktType: KtType, visited: MutableSet<KtType>, extra: (KtType, PsiJavaFile) -> Unit) {
        analyze {
            fun record(type: KtType) {
                if (type in visited)
                    return
                visited.add(type)
                for (superType in type.getDirectSuperTypes()) {
                    for (file in type.psiJavaFiles) {
                        recordWithArgs(superType, file)
                    }
                    record(superType)
                }
                for (file in type.psiJavaFiles) {
                    extra(type, file)
                }
            }
            record(ktType)
        }
    }
}

internal fun recordLookup(ktType: KtType, context: KSNode?) =
    ResolverAAImpl.instance.incrementalContext.recordLookup(ktType, context)

internal fun recordLookupWithSupertypes(ktType: KtType, extra: (KtType, PsiJavaFile) -> Unit = { _, _ -> }) =
    ResolverAAImpl.instance.incrementalContext.recordLookupWithSupertypes(ktType, mutableSetOf(), extra)

internal fun recordLookupForPropertyOrMethod(declaration: KSDeclaration) =
    ResolverAAImpl.instance.incrementalContext.recordLookupForPropertyOrMethod(declaration)

internal fun recordLookupForGetAllProperties(supers: List<KtType>) =
    ResolverAAImpl.instance.incrementalContext.recordLookupForGetAll(supers) {
        it is KtPropertySymbol || it is KtJavaFieldSymbol
    }

internal fun recordLookupForGetAllFunctions(supers: List<KtType>) =
    ResolverAAImpl.instance.incrementalContext.recordLookupForGetAll(supers) {
        it is KtFunctionLikeSymbol
    }

internal fun recordGetSealedSubclasses(classDeclaration: KSClassDeclaration) {
    if (classDeclaration.origin == Origin.KOTLIN) {
        ResolverAAImpl.instance.incrementalContext.recordGetSealedSubclasses(classDeclaration)
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
