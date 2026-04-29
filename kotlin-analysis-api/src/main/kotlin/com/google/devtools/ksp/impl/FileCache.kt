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

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.common.visitor.collectClassifierNames
import org.jetbrains.kotlin.psi.KtFile

/**
 * A cache for information extracted from a [KtFile].
 *
 * This class pre-calculates and stores frequently accessed information from a Kotlin PSI file,
 * such as its package name, imports, and local declarations, to avoid redundant PSI traversals.
 *
 * @param file The [KtFile] from which information is extracted.
 */
class FileCache(file: KtFile) {

    /**
     * The fully qualified package name of the file.
     */
    val packageName = file.packageFqName

    /**
     * A map of explicit (non-star) imports in the file.
     *
     * The keys are the names by which the imported symbols are referred to in the code
     * (either an alias or the short name), and the values are their fully qualified names.
     */
    val explicitImports = file.importDirectives
        .filter { !it.isAllUnder && it.importedFqName != null }
        .associate { (it.aliasName ?: it.importedFqName!!.shortName().asString()) to it.importedFqName!! }

    /**
     * A list of fully qualified names of the star imports in the file.
     */
    val starImports = file.importDirectives
        .filter { it.isAllUnder }
        .mapNotNull { it.importedFqName }

    /**
     * A collection of names for all local declarations within the file.
     */
    val localDeclarations = file.collectClassifierNames()

    companion object {
        // TODO: Figure out if there is a better way to obtain default imports than hard coded list
        // TODO: Move this list somewhere appropriate
        @JvmStatic
        val DEFAULT_IMPORTS = listOf(
            "kotlin",
            "kotlin.annotation",
            "kotlin.collections",
            "kotlin.comparisons",
            "kotlin.io",
            "kotlin.ranges",
            "kotlin.sequences",
            "kotlin.text",
            "kotlin.jvm",
            "java.lang"
        )
    }
}
