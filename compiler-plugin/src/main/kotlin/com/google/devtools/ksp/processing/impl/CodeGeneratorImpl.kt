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
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import java.io.File
import java.io.OutputStream

class CodeGeneratorImpl(
    private val classDir: File,
    private val javaDir: File,
    private val kotlinDir: File,
    private val resourcesDir: File,
    private val projectBase: File,
    private val anyChangesWildcard: KSFile,
    private val allSources: List<KSFile>
) : CodeGenerator {
    private val fileMap = mutableMapOf<String, File>()

    private val separator = File.separator

    val sourceToOutputs: MutableMap<File, MutableSet<File>> = mutableMapOf()

    fun pathOf(packageName: String, fileName: String, extensionName: String): String {
        val packageDirs = if (packageName != "") "${packageName.split(".").joinToString(separator)}$separator" else ""
        val extension = if (extensionName != "") ".${extensionName}" else ""
        val typeRoot = when (extensionName) {
            "class" -> classDir
            "java" -> javaDir
            "kt" -> kotlinDir
            else -> resourcesDir
        }.path
        return "$typeRoot$separator$packageDirs$fileName${extension}"
    }

    override fun createNewFile(dependencies: Dependencies, packageName: String, fileName: String, extensionName: String): OutputStream {
        val path = pathOf(packageName, fileName, extensionName)
        if (fileMap[path] != null) return fileMap[path]!!.outputStream()
        val file = File(path)
        val parentFile = file.parentFile
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw IllegalStateException("failed to make parent directories.")
        }
        file.writeText("")
        fileMap[path] = file
        val sources = if (dependencies.isAllSources) {
            allSources + anyChangesWildcard
        } else {
            if (dependencies.aggregating) {
                dependencies.originatingFiles + anyChangesWildcard
            } else {
                dependencies.originatingFiles
            }
        }
        associate(sources, path)
        return file.outputStream()
    }

    override fun associate(sources: List<KSFile>, packageName: String, fileName: String, extensionName: String) {
        val path = pathOf(packageName, fileName, extensionName)
        associate(sources, path)
    }

    private fun associate(sources: List<KSFile>, outputPath: String) {
        val output = File(outputPath).relativeTo(projectBase)
        sources.forEach { source ->
            sourceToOutputs.getOrPut(File(source.filePath).relativeTo(projectBase)) { mutableSetOf() }.add(output)
        }
    }

    val outputs: Set<File>
        get() = fileMap.keys.mapTo(mutableSetOf()) { File(it).relativeTo(projectBase) }
}