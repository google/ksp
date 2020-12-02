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


package com.google.devtools.ksp.processing

import java.io.File
import java.io.OutputStream

/**
 * [CodeGenerator] creates and manages files.
 *
 * Files created by [CodeGenerator] are considered in incremental processing.
 * Kotlin and Java files will be compiled together with other source files in the module.
 * Files created without using this API, will not participate in incremental processing nor subsequent compilations.
 */
interface CodeGenerator {
    /**
     * Creates a file which is managed by [CodeGenerator]
     *
     * @param packageName corresponds to the relative path of the generated file; using either '.'or '/' as separator.
     * @param fileName file name
     * @param extensionName If "kt" or "java", this file will participate in subsequent compilation.
     *                      Otherwise its creation is only considered in incremental processing.
     * @return OutputStream for writing into files.
     * @see [CodeGenerator] for more details.
     */
    fun createNewFile(packageName: String, fileName: String, extensionName: String = "kt"): OutputStream

    val generatedFile: Collection<File>
}