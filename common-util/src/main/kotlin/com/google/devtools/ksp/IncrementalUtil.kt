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
class NoSourceFile(baseDir: File, val fqn: String) : KSVirtualFile(baseDir, "NoSourceFile") {
    override val fileName: String
        get() = "<NoSourceFile for $fqn is a virtual file; DO NOT USE.>"
}
