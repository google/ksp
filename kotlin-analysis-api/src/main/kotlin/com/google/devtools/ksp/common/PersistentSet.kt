/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import java.io.BufferedInputStream
import java.io.File
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

private val stringSetSerializer = SetSerializer(String.serializer())

abstract class PersistentSet<T>(
    private val serializer: KSerializer<Set<T>>,
    private val storage: File,
    private val s: MutableSet<T>,
) : MutableSet<T> by s {
    val existsOnDisk: Boolean
        get() = storage.exists()

    @OptIn(ExperimentalSerializationApi::class)
    fun flush() {
        storage.parentFile?.mkdirs()
        storage.outputStream().use {
            Json.encodeToStream(serializer, s.toSet(), it)
        }
    }

    override fun toString() = s.toString()

    companion object {
        @JvmStatic
        @OptIn(ExperimentalSerializationApi::class)
        protected fun <T> deserialize(serializer: KSerializer<Set<T>>, storage: File): MutableSet<T> {
            return if (storage.exists()) {
                BufferedInputStream(storage.inputStream()).use {
                    Json.decodeFromStream(serializer, it).toMutableSet()
                }
            } else {
                mutableSetOf()
            }
        }
    }
}

internal fun dotPrefixesOf(fqns: Iterable<String>): Set<String> = buildSet {
    for (fqn in fqns) {
        var dot = fqn.indexOf('.')
        while (dot != -1) {
            add(fqn.substring(0, dot))
            dot = fqn.indexOf('.', dot + 1)
        }
    }
}

internal fun hasDotPrefixIn(fqn: String, candidates: Set<String>): Boolean {
    var dot = fqn.indexOf('.')
    while (dot != -1) {
        if (fqn.substring(0, dot) in candidates) return true
        dot = fqn.indexOf('.', dot + 1)
    }
    return false
}

internal fun isClassAffectedBy(
    targetFqn: String,
    changedClasses: Set<String>,
    changedEnclosingPrefixes: Set<String>,
): Boolean =
    targetFqn in changedClasses ||
        targetFqn in changedEnclosingPrefixes ||
        hasDotPrefixIn(targetFqn, changedClasses)

/**
 * Class FQNs queried by processors via `Resolver` APIs (e.g. `getClassDeclarationByName`, member lookups in
 * `getFunctionDeclarationsByName` / `getPropertyDeclarationByName`, and library classes yielded by
 * `getDeclarationsFromPackage`). Persisted across incremental runs.
 *
 * A classpath change `fqn` affects these queries when:
 * 1. `fqn` was queried directly (`fqn in this`),
 * 2. `fqn` is a nested class of a queried outer class (`hasDotPrefixIn(fqn, this)`), or
 * 3. `fqn` is an enclosing class of a queried nested class or enum entry (`fqn in enclosingPrefixes`).
 */
class ClassQueries(
    storage: File,
) : PersistentSet<String>(stringSetSerializer, storage, deserialize(stringSetSerializer, storage)) {
    fun isAffectedBy(changedClasses: Collection<String>): Boolean {
        if (isEmpty() || changedClasses.isEmpty()) return false
        val enclosingPrefixes by lazy(LazyThreadSafetyMode.NONE) {
            dotPrefixesOf(this@ClassQueries)
        }
        return changedClasses.any { rawFqn ->
            val fqn = rawFqn.replace('$', '.')
            fqn in this || hasDotPrefixIn(fqn, this) || fqn in enclosingPrefixes
        }
    }
}

/**
 * Package FQNs queried by processors via `Resolver` APIs (e.g. `getDeclarationsFromPackage`, and top-level lookups in
 * `getFunctionDeclarationsByName` / `getPropertyDeclarationByName` with `includeTopLevel = true`).
 * Persisted across incremental runs.
 *
 * Entries in `changedClasses` use `.` for package separators while preserving `$` inside class and file-facade names.
 * Thus `rawFqn.substringBeforeLast('.', "")` extracts the exact package without confusing `$`-containing class names
 * (`hints.Bar$Generated`, `hints.My$UtilKt`) with subpackages (`hints.Bar.Generated`). Also checks `dotNormalizedFqn`
 * and `enclosingPrefixes` so `includeTopLevel = true` callable lookups on a previously missing owner or enclosing
 * subclass match when that class is added, without matching subpackages or sibling classes.
 */
class PackageQueries(
    storage: File,
) : PersistentSet<String>(stringSetSerializer, storage, deserialize(stringSetSerializer, storage)) {
    fun isAffectedBy(changedClasses: Collection<String>): Boolean {
        if (isEmpty() || changedClasses.isEmpty()) return false
        val enclosingPrefixes by lazy(LazyThreadSafetyMode.NONE) {
            dotPrefixesOf(this@PackageQueries)
        }
        return changedClasses.any { rawFqn ->
            val pkg = rawFqn.substringBeforeLast('.', "").replace('$', '.')
            val dotNormalizedFqn = rawFqn.replace('$', '.')
            pkg in this || dotNormalizedFqn in this || dotNormalizedFqn in enclosingPrefixes
        }
    }
}
