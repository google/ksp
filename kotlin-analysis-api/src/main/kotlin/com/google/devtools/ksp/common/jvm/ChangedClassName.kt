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

package com.google.devtools.ksp.common.jvm

import com.google.devtools.ksp.common.LookupSymbolWrapper

internal const val ANONYMOUS_ROOT_SCOPE = "<anonymous>"

/**
 * Structured representation of a JVM classpath-change class name where `.` separates package
 * segments (or already-normalized nested segments) and `$` may be either a JVM nested-class
 * separator or a literal `$` character in a class or package name.
 *
 * Identity (`equals`, `hashCode`, `toString`) is determined solely by [raw]; derived views are
 * lazily computed and cached on the instance.
 */
internal data class ChangedClassName(val raw: String) {
    val packageName: String by lazy { raw.substringBeforeLast('.', "") }

    val binarySimpleName: String by lazy { raw.substringAfterLast('.') }

    /** Package name with `$` normalized to `.` (for `PackageQueries`). */
    val normalizedPackageName: String by lazy { packageName.normalizeJvmDollars() }

    /** Fully dot-normalized FQN (for `ClassQueries` and `NoSourceFile` matching). */
    val normalizedFqn: String by lazy { raw.normalizeJvmDollars() }

    /**
     * All candidate [LookupSymbolWrapper] keys that could refer to this class in lookup caches,
     * accounting for:
     * - root-package `""` vs `"<anonymous>"` scope aliases,
     * - `$` inside package names, and
     * - `$` inside [binarySimpleName] being either a literal character or a nested-class separator.
     */
    val lookupSymbols: Set<LookupSymbolWrapper> by lazy {
        buildSet {
            // 1. Top-level interpretation: treat any `$` in binarySimpleName as literal.
            for (scope in packageScopeVariants(packageName)) {
                add(LookupSymbolWrapper(binarySimpleName, scope))
            }

            // 2. Nested-class interpretations: split binarySimpleName at each `$` into (outerPart, innerName).
            val pkgPrefixes = packagePrefixVariants(packageName)
            for ((rawOuterPart, innerName) in splitBinaryNestedNames(binarySimpleName)) {
                for (outerScope in expandNestedOuterScopes(rawOuterPart)) {
                    for (pkg in pkgPrefixes) {
                        val fullScope = if (pkg.isEmpty()) outerScope else "$pkg.$outerScope"
                        add(LookupSymbolWrapper(innerName, fullScope))
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Converts a JVM internal name (`/` package separators, `$` nested or literal class-name
         * characters) into a [ChangedClassName] while keeping `$` intact so package and class
         * boundaries stay distinct.
         */
        fun fromInternalName(internalName: String): ChangedClassName =
            ChangedClassName(internalName.replace('/', '.'))
    }
}

/** Normalizes JVM `$` characters to `.` for dot-normalized FQN comparisons. */
internal fun String.normalizeJvmDollars(): String = replace('$', '.')

/**
 * Returns [scope] together with its root-package alias (`""` ↔ `"<anonymous>"`) when [scope]
 * represents the root package; otherwise returns `listOf(scope)`.
 */
internal fun rootScopeAliases(scope: String): List<String> = when (scope) {
    "" -> listOf("", ANONYMOUS_ROOT_SCOPE)
    ANONYMOUS_ROOT_SCOPE -> listOf(ANONYMOUS_ROOT_SCOPE, "")
    else -> listOf(scope)
}

/**
 * Returns [this] together with its root-package alias (`""` ↔ `"<anonymous>"`) if it resides in
 * the root package; otherwise returns `listOf(this)`.
 */
internal fun LookupSymbolWrapper.withRootScopeAliases(): List<LookupSymbolWrapper> =
    rootScopeAliases(scope).map { aliasScope ->
        if (aliasScope == scope) this else LookupSymbolWrapper(name, aliasScope)
    }

/** Returns all strict dot-separated prefixes of every FQN in [fqns]. */
internal fun dotPrefixesOf(fqns: Iterable<String>): Set<String> = buildSet {
    for (fqn in fqns) {
        var dot = fqn.indexOf('.')
        while (dot != -1) {
            add(fqn.substring(0, dot))
            dot = fqn.indexOf('.', dot + 1)
        }
    }
}

/** Returns `true` if any strict dot-separated prefix of [fqn] is in [candidates]. */
internal fun hasDotPrefixIn(fqn: String, candidates: Set<String>): Boolean {
    var dot = fqn.indexOf('.')
    while (dot != -1) {
        if (fqn.substring(0, dot) in candidates) return true
        dot = fqn.indexOf('.', dot + 1)
    }
    return false
}

/**
 * Returns `true` if [targetFqn] is directly in [changedClasses], is an enclosing class of a
 * changed class (in [changedEnclosingPrefixes]), or is nested inside a changed class.
 */
internal fun isClassAffectedBy(
    targetFqn: String,
    changedClasses: Set<String>,
    changedEnclosingPrefixes: Set<String>,
): Boolean =
    targetFqn in changedClasses ||
        targetFqn in changedEnclosingPrefixes ||
        hasDotPrefixIn(targetFqn, changedClasses)

private fun packagePrefixVariants(packageName: String): List<String> =
    if ('$' in packageName) listOf(packageName, packageName.normalizeJvmDollars()) else listOf(packageName)

private fun packageScopeVariants(packageName: String): List<String> =
    if (packageName.isEmpty()) rootScopeAliases("") else packagePrefixVariants(packageName)

private fun splitBinaryNestedNames(binarySimpleName: String): List<Pair<String, String>> = buildList {
    var dollar = binarySimpleName.indexOf('$')
    while (dollar != -1) {
        val rawOuterPart = binarySimpleName.substring(0, dollar)
        val innerName = binarySimpleName.substring(dollar + 1)
        if (rawOuterPart.isNotEmpty() && innerName.isNotEmpty()) {
            add(rawOuterPart to innerName)
        }
        dollar = binarySimpleName.indexOf('$', dollar + 1)
    }
}

private const val MAX_COMBINATORIAL_DOLLARS = 4

private fun expandNestedOuterScopes(rawOuterPart: String): Set<String> {
    val dollarPositions = buildList {
        var idx = rawOuterPart.indexOf('$')
        while (idx != -1) {
            if (idx > 0 && idx < rawOuterPart.length - 1) add(idx)
            idx = rawOuterPart.indexOf('$', idx + 1)
        }
    }
    val candidates = mutableSetOf(rawOuterPart)
    val allDots = rawOuterPart.normalizeJvmDollars()
    if (!allDots.startsWith('.') && !allDots.endsWith('.') && ".." !in allDots) {
        candidates.add(allDots)
    }
    if (dollarPositions.size in 1..MAX_COMBINATORIAL_DOLLARS) {
        val maskCount = 1 shl dollarPositions.size
        for (mask in 1 until maskCount) {
            val chars = rawOuterPart.toCharArray()
            var valid = true
            for (bit in dollarPositions.indices) {
                if ((mask and (1 shl bit)) != 0) {
                    val pos = dollarPositions[bit]
                    if (chars[pos - 1] == '.') {
                        valid = false
                        break
                    }
                    chars[pos] = '.'
                }
            }
            if (valid) candidates.add(String(chars))
        }
    }
    return candidates
}
