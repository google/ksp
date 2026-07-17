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

package com.google.devtools.ksp.common

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
        get() = "$syntheticFileNamePrefix$name$SYNTHETIC_FILE_NAME_SUFFIX"

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

    abstract val syntheticFileNamePrefix: String

    companion object {
        const val SYNTHETIC_FILE_NAME_SUFFIX: String = " is a virtual file; DO NOT USE.>"
    }
}

/** Used when an output potentially depends on new information. */
class AnyChanges(baseDir: File) : KSVirtualFile(baseDir, "AnyChanges") {
    override val syntheticFileNamePrefix: String = SYNTHETIC_FILE_NAME_PREFIX

    companion object {
        fun isSyntheticFile(name: String, baseDir: File? = null): Boolean {
            val anyChangesFile = baseDir?.let(::AnyChanges)
            val processedName = name.removeSuffix(SYNTHETIC_FILE_NAME_SUFFIX)
            return name == anyChangesFile?.fileName ||
                name == anyChangesFile?.filePath ||
                processedName.removePrefix(SYNTHETIC_FILE_NAME_PREFIX) == "AnyChanges"
        }

        const val SYNTHETIC_FILE_NAME_PREFIX: String = "<"
    }
}

/** Used for classes from classpath, i.e., classes without source files. */
class NoSourceFile(baseDir: File, val fqn: String) : KSVirtualFile(baseDir, fqn) {
    override val syntheticFileNamePrefix: String = SYNTHETIC_FILE_NAME_PREFIX

    companion object {
        fun isSyntheticFile(name: String, baseDir: File? = null): Boolean {

            val noSourceFile = baseDir?.let { NoSourceFile(it, name) }

            val processedName = name.removeSuffix(SYNTHETIC_FILE_NAME_SUFFIX)
            return name == noSourceFile?.fileName ||
                name == noSourceFile?.filePath ||
                (name.startsWith(SYNTHETIC_FILE_NAME_PREFIX) &&
                    name.endsWith(SYNTHETIC_FILE_NAME_SUFFIX))
        }

        const val SYNTHETIC_FILE_NAME_PREFIX: String = "<NoSourceFile for "
    }
}

fun isSyntheticFileName(name: String, baseDir: File? = null): Boolean {
    return AnyChanges.isSyntheticFile(name, baseDir) || NoSourceFile.isSyntheticFile(name, baseDir)
}

/** Returns `a.b.c.MyClass` if `name` is a synthetic [NoSourceFile] file for `a.b.c.MyClass`. */
fun stripSyntheticFileNameModifiers(name: String, baseDir: File? = null): String {
    require(
        NoSourceFile.isSyntheticFile(name, baseDir),
        { "Name '$name' must be recognized by NoSourceFile.isSyntheticFile" },
    )
    return name
        .removePrefix(NoSourceFile.SYNTHETIC_FILE_NAME_PREFIX)
        .removeSuffix(KSVirtualFile.SYNTHETIC_FILE_NAME_SUFFIX)
}

// Copy recursively, including last-modified-time of file and its parent dirs.
//
// `java.nio.file.Files.copy(path1, path2, options...)` keeps last-modified-time (if supported)
// according to
// https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html
fun copyWithTimestamp(src: File, dst: File, overwrite: Boolean) {
    if (!dst.parentFile.exists()) copyWithTimestamp(src.parentFile, dst.parentFile, false)
    if (overwrite) {
        Files.copy(
            src.toPath(),
            dst.toPath(),
            StandardCopyOption.COPY_ATTRIBUTES,
            StandardCopyOption.REPLACE_EXISTING,
        )
    } else {
        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
    }
}
