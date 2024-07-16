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
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaDynamicType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.LookupTrackerImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import org.jetbrains.kotlin.incremental.update
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

    // Debugging and testing only.
    fun dumpLookupRecords(): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        symbolLookupTracker.lookups.entrySet().forEach { e ->
            val key = "${e.key.scope}.${e.key.name}"
            map[key] = e.value.map { PATH_CONVERTER.toFile(it).path }
        }
        return map
    }

    private fun recordWithArgs(type: KaType, file: PsiJavaFile) {
        type.typeArguments().forEach {
            it.type?.let { recordWithArgs(it, file) }
        }
        when (type) {
            is KaClassType -> {
                val fqn = type.classId.asFqNameString()
                recordLookup(file, fqn)
            }
            is KaFlexibleType -> {
                recordWithArgs(type.lowerBound, file)
                recordWithArgs(type.upperBound, file)
            }
            is KaIntersectionType -> {
                type.conjuncts.forEach {
                    recordWithArgs(it, file)
                }
            }
            is KaCapturedType -> {
                type.projection.type?.let {
                    recordWithArgs(it, file)
                }
            }
            is KaDefinitelyNotNullType -> {
                recordWithArgs(type.original, file)
            }
            is KaErrorType, is KaDynamicType, is KaTypeParameterType -> {}
        }
    }

    fun recordLookup(type: KaType, context: KSNode?) {
        val file = (context?.containingFile as? KSFileJavaImpl)?.psi ?: return

        recordWithArgs(type, file)
    }

    @OptIn(KaExperimentalApi::class)
    private fun recordLookupForDeclaration(symbol: KaSymbol, file: PsiJavaFile) {
        when (symbol) {
            is KaJavaFieldSymbol -> recordWithArgs(symbol.returnType, file)
            is KaPropertySymbol -> recordWithArgs(symbol.returnType, file)
            is KaFunctionSymbol -> {
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

    internal fun recordLookupForGetAll(supers: List<KaType>, predicate: (KaSymbol) -> Boolean) {
        val visited: MutableSet<KaType> = mutableSetOf()
        analyze {
            supers.forEach {
                recordLookupWithSupertypes(it, visited) { type, file ->
                    if (type is KaClassType) {
                        (type.symbol as? KaDeclarationContainerSymbol)?.let {
                            val declared = it.declaredMemberScope.declarations +
                                it.staticDeclaredMemberScope.declarations
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

    private val KaSymbol.psiJavaFile: PsiJavaFile?
        get() = psi?.containingFile as? PsiJavaFile

    private val KaType.psiJavaFiles: List<PsiJavaFile>
        get() {
            return when (this) {
                is KaClassType -> symbol.psiJavaFile?.let { listOf(it) } ?: emptyList()
                is KaFlexibleType -> lowerBound.psiJavaFiles + upperBound.psiJavaFiles
                is KaIntersectionType -> conjuncts.flatMap { it.psiJavaFiles }
                is KaCapturedType -> projection.type?.psiJavaFiles ?: emptyList()
                is KaDefinitelyNotNullType -> original.psiJavaFiles
                is KaErrorType, is KaDynamicType, is KaTypeParameterType -> emptyList()
                else -> TODO()
            }
        }

    fun recordLookupWithSupertypes(ktType: KaType, visited: MutableSet<KaType>, extra: (KaType, PsiJavaFile) -> Unit) {
        analyze {
            fun record(type: KaType) {
                if (type in visited)
                    return
                visited.add(type)
                for (superType in type.directSupertypes(false)) {
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

internal fun recordLookup(ktType: KaType, context: KSNode?) =
    ResolverAAImpl.instance.incrementalContext.recordLookup(ktType, context)

internal fun recordLookupWithSupertypes(ktType: KaType, extra: (KaType, PsiJavaFile) -> Unit = { _, _ -> }) =
    ResolverAAImpl.instance.incrementalContext.recordLookupWithSupertypes(ktType, mutableSetOf(), extra)

internal fun recordLookupForPropertyOrMethod(declaration: KSDeclaration) =
    ResolverAAImpl.instance.incrementalContext.recordLookupForPropertyOrMethod(declaration)

internal fun recordLookupForGetAllProperties(supers: List<KaType>) =
    ResolverAAImpl.instance.incrementalContext.recordLookupForGetAll(supers) {
        it is KaPropertySymbol || it is KaJavaFieldSymbol
    }

internal fun recordLookupForGetAllFunctions(supers: List<KaType>) =
    ResolverAAImpl.instance.incrementalContext.recordLookupForGetAll(supers) {
        it is KaFunctionSymbol
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

internal class RelativeFileToPathConverter(val baseDir: File) : FileToPathConverter {
    override fun toPath(file: File): String = file.path
    override fun toFile(path: String): File = File(path).relativeTo(baseDir)
}
