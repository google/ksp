package com.google.devtools.ksp.test.utils

import java.io.File

/**
 * Pretty print the directory tree and its file names.
 *
 */
fun printDirectoryTree(folder: File): String {
    require(folder.isDirectory) { "folder is not a Directory" }
    val indent = 0
    val sb = StringBuilder()
    printDirectoryTree(folder, indent, sb)
    return sb.toString()
}

private fun printDirectoryTree(
    folder: File,
    indent: Int,
    sb: StringBuilder
) {
    require(folder.isDirectory) { "folder is not a Directory" }
    sb.append(getIndentString(indent))
    sb.append("+--")
    sb.append(folder.name)
    sb.append("/")
    sb.append("\n")
    for (file in folder.listFiles()) {
        if (file.isDirectory) {
            printDirectoryTree(file, indent + 1, sb)
        } else {
            printFile(file, indent + 1, sb)
        }
    }
}

private fun printFile(file: File, indent: Int, sb: StringBuilder) {
    sb.append(getIndentString(indent))
    sb.append("+--")
    sb.append(file.name)
    sb.append("\n")
}

private fun getIndentString(indent: Int): String? {
    val sb = StringBuilder()
    for (i in 0 until indent) {
        sb.append("|  ")
    }
    return sb.toString()
}
