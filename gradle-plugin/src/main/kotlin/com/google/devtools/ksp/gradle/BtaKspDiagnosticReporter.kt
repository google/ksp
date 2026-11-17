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

package com.google.devtools.ksp.gradle

import com.google.devtools.ksp.processing.KspGradleLogger
import java.io.File
import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer.SourceLocation

internal class BtaKspDiagnosticReporter(private val logLevel: Int) : KspDiagnosticReporter {
    private val output = System.out
    private val renderer = object : CompilerMessageRenderer {
        override fun render(
            severity: CompilerMessageRenderer.Severity,
            message: String,
            location: SourceLocation?,
        ): String = "${if (severity == CompilerMessageRenderer.Severity.ERROR) "e" else "w"}: $message"
    }

    override fun report(diagnostic: Diagnostic<out JavaFileObject>) {
        val severity = when (diagnostic.kind) {
            Diagnostic.Kind.ERROR -> CompilerMessageRenderer.Severity.ERROR
            Diagnostic.Kind.WARNING, Diagnostic.Kind.MANDATORY_WARNING -> CompilerMessageRenderer.Severity.WARNING
            else -> return
        }
        val rendered = renderer.render(severity, diagnostic.getMessage(null), diagnostic.toBtaSourceLocation())
        val messageLevel = if (severity == CompilerMessageRenderer.Severity.ERROR) {
            KspGradleLogger.LOGGING_LEVEL_ERROR
        } else {
            KspGradleLogger.LOGGING_LEVEL_WARN
        }
        if (logLevel <= messageLevel) output.println(rendered)
    }
}

internal fun Diagnostic<out JavaFileObject>.toBtaSourceLocation(): SourceLocation? = source?.let {
    SourceLocation(
        path = File(it.toUri()).path,
        line = lineNumber.toInt(),
        column = columnNumber.toInt(),
        lineEnd = -1,
        columnEnd = -1,
        lineContent = null,
    )
}
