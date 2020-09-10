/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processing.impl

import org.jetbrains.kotlin.ksp.processing.CodeGenerator
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
            throw IllegalStateException()
        }
        file.writeText("")
        fileMap[path] = file
        return file.outputStream()
    }
}