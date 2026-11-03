/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.Location
import java.io.File
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

/**
 * Carries the decorated console body and source metadata through the JDK [javax.tools.DiagnosticListener].
 * The JDK interface crosses the engine's isolated classloader without Gradle or BTA dependencies.
 *
 * KSP runs on the JVM for every target. [JavaFileObject] supplies Kotlin or Java source URIs without
 * reading source contents. Current producers supply line numbers and use [Diagnostic.NOPOS] for
 * columns. This adapter accepts a column when one is available.
 */
internal class KspDiagnostic(
    private val diagnosticKind: Diagnostic.Kind,
    private val message: String,
    location: Location?,
    private val column: Long = Diagnostic.NOPOS
) : Diagnostic<JavaFileObject> {
    private val fileLocation = location as? FileLocation
    private val sourceFile = fileLocation?.let {
        object : SimpleJavaFileObject(File(it.filePath).toURI(), JavaFileObject.Kind.SOURCE) {}
    }

    override fun getKind(): Diagnostic.Kind = diagnosticKind
    override fun getMessage(locale: Locale?): String = message
    override fun getSource(): JavaFileObject? = sourceFile
    override fun getLineNumber(): Long = fileLocation?.lineNumber?.takeIf { it > 0 }?.toLong() ?: Diagnostic.NOPOS
    override fun getColumnNumber(): Long = column
    override fun getPosition(): Long = Diagnostic.NOPOS
    override fun getStartPosition(): Long = Diagnostic.NOPOS
    override fun getEndPosition(): Long = Diagnostic.NOPOS
    override fun getCode(): String = "ksp"
}
