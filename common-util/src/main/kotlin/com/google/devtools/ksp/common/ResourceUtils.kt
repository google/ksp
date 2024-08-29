package com.google.devtools.ksp.common

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

fun Iterable<File>.getResource(path: String): InputStream? = firstNotNullOfOrNull { root ->
    val file = root.resolve(path)
    try {
        if (file.toPath().normalize().startsWith(root.toPath().normalize())) {
            FileInputStream(file)
        } else {
            null
        }
    } catch (e: FileNotFoundException) {
        null
    }
}

fun Iterable<File>.getAllResources(): Sequence<String> =
    asSequence().flatMap { root ->
        println(root)
        root
            .walkTopDown()
            .filter { it.isFile }
            .map { it.toRelativeString(root) }
    }
