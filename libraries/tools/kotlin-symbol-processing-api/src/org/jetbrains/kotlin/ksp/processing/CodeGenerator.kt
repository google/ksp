/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processing

import java.io.File

/**
 * [CodeGenerator] creates and manages files.
 *
 * Files created by [CodeGenerator] are considered in incremental processing.
 * Kotlin and Java files will be compiled together with other source files in the module.
 * Files created without using this API, will not participate in incremental processing nor subsequent compilations.
 */
interface CodeGenerator {
    /*
     * Creates a file which is managed by [CodeGenerator]
     *
     * @param packageName corresponds to the relative path of the generated file; using either '.'or '/' as separator.
     * @param fileName file name
     * @param extensionName If "kt" or "java", this file will participate in subsequent compilation.
     *                      Otherwise its creation is only considered in incremental processing.
     * @see [CodeGenerator] for more details.
     */
    fun createNewFile(packageName: String, fileName: String, extensionName: String = "kt"): File
}