/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp

import com.google.devtools.ksp.symbol.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class KSVirtualFile(val baseDir: File, val name: String) : KSFile {
    override val annotations: Sequence<KSAnnotation>
        get() = throw Exception("$name should not be used.")

    override val declarations: Sequence<KSDeclaration>
        get() = throw Exception("$name should not be used.")

    override val fileName: String
        get() = "<$name is a virtual file; DO NOT USE.>"

    override val filePath: String
        get() = File(baseDir, fileName).path

    override val packageName: KSName
        get() = throw Exception("$name should not be used.")

    override val origin: Origin
        get() = throw Exception("$name should not be used.")

    override val location: Location
        get() = throw Exception("$name should not be used.")

    override val parent: KSNode?
        get() = throw Exception("$name should not be used.")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        throw Exception("$name should not be used.")
    }
}

/**
 * Used when an output potentially depends on new information.
 */
class AnyChanges(baseDir: File) : KSVirtualFile(baseDir, "AnyChanges")

/**
 * Used for classes from classpath, i.e., classes without source files.
 */
class NoSourceFile(baseDir: File, val fqn: String) : KSVirtualFile(baseDir, "NoSourceFile for $fqn")

// Copy recursively, including last-modified-time of file and its parent dirs.
//
// `java.nio.file.Files.copy(path1, path2, options...)` keeps last-modified-time (if supported) according to
// https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html
fun copyWithTimestamp(src: File, dst: File, overwrite: Boolean) {
    if (!dst.parentFile.exists())
        copyWithTimestamp(src.parentFile, dst.parentFile, false)
    if (overwrite) {
        Files.copy(
            src.toPath(),
            dst.toPath(),
            StandardCopyOption.COPY_ATTRIBUTES,
            StandardCopyOption.REPLACE_EXISTING
        )
    } else {
        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
    }
}
