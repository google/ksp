/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import com.google.devtools.ksp.symbol.KSNode
import javax.tools.Diagnostic
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject

class KspGradleLogger @JvmOverloads constructor(
    val loglevel: Int,
    private val diagnosticListener: DiagnosticListener<JavaFileObject>? = null
) : KSPLogger {
    private val messager = System.out

    private fun decorateMessage(message: String, symbol: KSNode?): String =
        when {
            symbol is KspDiagnosticNode -> message
            symbol?.location is FileLocation -> {
                val location = symbol.location as FileLocation
                "${location.filePath}:${location.lineNumber}: $message"
            }
            else -> message
        }

    override fun logging(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_LOGGING)
            messager.println("v: [ksp] ${decorateMessage(message, symbol)}")
    }

    override fun info(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_INFO)
            messager.println("i: [ksp] ${decorateMessage(message, symbol)}")
    }

    override fun warn(message: String, symbol: KSNode?) {
        if (diagnosticListener != null || loglevel <= LOGGING_LEVEL_WARN)
            report(Diagnostic.Kind.WARNING, "w", "[ksp] ${decorateMessage(message, symbol)}", symbol)
    }

    override fun error(message: String, symbol: KSNode?) {
        if (diagnosticListener != null || loglevel <= LOGGING_LEVEL_ERROR)
            report(Diagnostic.Kind.ERROR, "e", "[ksp] ${decorateMessage(message, symbol)}", symbol)
    }

    override fun exception(e: Throwable) {
        if (diagnosticListener != null || loglevel <= LOGGING_LEVEL_ERROR)
            report(Diagnostic.Kind.ERROR, "e", "[ksp] $e", null)
    }

    private fun report(kind: Diagnostic.Kind, prefix: String, message: String, symbol: KSNode?) {
        if (diagnosticListener != null) {
            diagnosticListener.report(KspDiagnostic(kind, message, symbol?.location))
        } else {
            messager.println("$prefix: $message")
        }
    }

    companion object {
        const val LOGGING_LEVEL_LOGGING = 0
        const val LOGGING_LEVEL_INFO = 1
        const val LOGGING_LEVEL_WARN = 3
        const val LOGGING_LEVEL_ERROR = 5
    }
}
