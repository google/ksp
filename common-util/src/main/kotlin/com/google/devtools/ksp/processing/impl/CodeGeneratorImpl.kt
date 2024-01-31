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

import com.google.devtools.ksp.NoSourceFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class CodeGeneratorImpl(
    private val classDir: File,
    private val javaDir: () -> File,
    private val kotlinDir: File,
    private val resourcesDir: File,
    private val projectBase: File,
    private val anyChangesWildcard: KSFile,
    private val allSources: List<KSFile>,
    private val isIncremental: Boolean
) : CodeGenerator {
    private val fileMap = mutableMapOf<String, File>()
    private val fileOutputStreamMap = mutableMapOf<String, FileOutputStream>()

    private val separator = File.separator

    val sourceToOutputs: MutableMap<File, MutableSet<File>> = mutableMapOf()

    // This function will also clear `fileOutputStreamMap` which will change the result of `generatedFile`
    fun closeFiles() {
        fileOutputStreamMap.keys.forEach {
            fileOutputStreamMap[it]!!.close()
        }
        fileOutputStreamMap.clear()
    }

    fun pathOf(packageName: String, fileName: String, extensionName: String): String {
        val packageDirs = if (packageName != "") "${packageName.split(".").joinToString(separator)}$separator" else ""
        val extension = if (extensionName != "") ".$extensionName" else ""
        return "$packageDirs$fileName$extension"
    }

    override fun createNewFile(
        dependencies: Dependencies,
        packageName: String,
        fileName: String,
        extensionName: String
    ): OutputStream {
        return createNewFile(
            dependencies,
            pathOf(packageName, fileName, extensionName),
            extensionToDirectory(extensionName)
        )
    }

    override fun createNewFileByPath(dependencies: Dependencies, path: String, extensionName: String): OutputStream {
        val extension = if (extensionName != "") ".$extensionName" else ""
        return createNewFile(dependencies, path + extension, extensionToDirectory(extensionName))
    }

    override fun associate(sources: List<KSFile>, packageName: String, fileName: String, extensionName: String) {
        associate(sources, pathOf(packageName, fileName, extensionName), extensionToDirectory(extensionName))
    }

    override fun associateByPath(sources: List<KSFile>, path: String, extensionName: String) {
        val extension = if (extensionName != "") ".$extensionName" else ""
        associate(sources, path + extension, extensionToDirectory(extensionName))
    }

    override fun associateWithClasses(
        classes: List<KSClassDeclaration>,
        packageName: String,
        fileName: String,
        extensionName: String
    ) {
        val path = pathOf(packageName, fileName, extensionName)
        val files = classes.map {
            it.containingFile ?: NoSourceFile(projectBase, it.qualifiedName?.asString().toString())
        }
        associate(files, path, extensionToDirectory(extensionName))
    }

    // FIXME: associate with the containing FileKt.
    // The containing FileKt is unfortunately a backend thing and is not available in Kotlin metadata.
    // Therefore, the only way to get it seems to be traversing bytecode and find the functions of interest.
    //
    // This implementation associates anyChangesWildcard, which is an overkill.
    override fun associateWithFunctions(
        functions: List<KSFunctionDeclaration>,
        packageName: String,
        fileName: String,
        extensionName: String
    ) {
        val path = pathOf(packageName, fileName, extensionName)
        val files = functions.map {
            it.containingFile ?: anyChangesWildcard
        }
        associate(files, path, extensionToDirectory(extensionName))
    }

    // FIXME: associate with the containing FileKt.
    // The containing FileKt is unfortunately a backend thing and is not available in Kotlin metadata.
    // Therefore, the only way to get it seems to be traversing bytecode and find the functions of interest.
    //
    // This implementation associates anyChangesWildcard, which is an overkill.
    override fun associateWithProperties(
        properties: List<KSPropertyDeclaration>,
        packageName: String,
        fileName: String,
        extensionName: String
    ) {
        val path = pathOf(packageName, fileName, extensionName)
        val files = properties.map {
            it.containingFile ?: anyChangesWildcard
        }
        associate(files, path, extensionToDirectory(extensionName))
    }

    private fun extensionToDirectory(extensionName: String): File {
        return when (extensionName) {
            "class" -> classDir
            "java" -> javaDir()
            "kt" -> kotlinDir
            else -> resourcesDir
        }
    }

    private fun createNewFile(dependencies: Dependencies, path: String, baseDir: File): OutputStream {
        val file = File(baseDir, path)
        if (!isWithinBaseDir(baseDir, file)) {
            throw IllegalStateException("requested path is outside the bounds of the required directory")
        }
        if (path in fileMap) {
            throw FileAlreadyExistsException(file)
        }
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
        associate(sources, file)
        fileOutputStreamMap[path] = fileMap[path]!!.outputStream()
        return fileOutputStreamMap[path]!!
    }

    private fun isWithinBaseDir(baseDir: File, file: File): Boolean {
        val base = baseDir.toPath().normalize()
        return try {
            val relativePath = file.toPath().normalize()
            relativePath.startsWith(base)
        } catch (e: IOException) {
            false
        }
    }

    private fun associate(sources: List<KSFile>, path: String, baseDir: File) {
        val file = File(baseDir, path)
        if (!isWithinBaseDir(baseDir, file)) {
            throw IllegalStateException("requested path is outside the bounds of the required directory")
        }
        associate(sources, file)
    }

    private fun associate(sources: List<KSFile>, outputPath: File) {
        if (!isIncremental)
            return

        val output = outputPath.relativeTo(projectBase)
        sources.forEach { source ->
            sourceToOutputs.getOrPut(File(source.filePath).relativeTo(projectBase)) { mutableSetOf() }.add(output)
        }
    }

    val outputs: Set<File>
        get() = fileMap.values.mapTo(mutableSetOf()) { it.relativeTo(projectBase) }

    override val generatedFile: Collection<File>
        get() = fileOutputStreamMap.keys.map { fileMap[it]!! }
}
