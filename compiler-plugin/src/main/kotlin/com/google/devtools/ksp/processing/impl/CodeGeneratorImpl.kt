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


package com.google.devtools.ksp.processing.impl

import com.google.devtools.ksp.processing.CodeGenerator
import java.io.File
import java.io.OutputStream

class CodeGeneratorImpl(
    private val classDir: File,
    private val javaDir: File,
    private val kotlinDir: File,
    private val resourcesDir: File
) : CodeGenerator {
    private val fileMap = mutableMapOf<String, File>()

    private val separator = File.separator

    override fun createNewFile(packageName: String, fileName: String, extensionName: String): OutputStream {
        val packageDirs = if (packageName != "") "${packageName.split(".").joinToString(separator)}$separator" else ""
        val extension = if (extensionName != "") ".${extensionName}" else ""
        val typeRoot = when (extensionName) {
            "class" -> classDir
            "java" -> javaDir
            "kt" -> kotlinDir
            else -> resourcesDir
        }.path
        val path = "$typeRoot$separator$packageDirs$fileName${extension}"
        if (fileMap[path] != null) return fileMap[path]!!.outputStream()
        val file = File(path)
        val parentFile = file.parentFile
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw IllegalStateException("failed to make parent directories.")
        }
        file.writeText("")
        fileMap[path] = file
        return file.outputStream()
    }

    override val generatedFile: Collection<File> = fileMap.values
}