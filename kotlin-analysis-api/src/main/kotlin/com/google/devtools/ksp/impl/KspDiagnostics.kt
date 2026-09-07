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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.processing.KSPLogger
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.psi.KtFile

/**
 * Object that holds typeChecking utils
 */
object KspDiagnostics {

    /**
     * Run typeChecking on a list of Kotlin Files
     *
     * Some of the APIs in use could throw exceptions so you need to handler errors accordingly.
     *
     * @param ktFiles List of files to run typecheck
     * @param logger If analysis finds type errors it's going to log them as errors
     */
    fun runTypeCheck(ktFiles: Collection<KtFile>, logger: KSPLogger) {
        if (ktFiles.isEmpty()) return

        analyze {
            for (file in ktFiles) {
                val diagnostics = file.collectDiagnostics(KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
                for (diagnostic in diagnostics) {
                    if (diagnostic.severity == KaSeverity.ERROR) {
                        val psi = diagnostic.psi
                        val fileLoc = psi.containingFile?.virtualFile?.path ?: psi.containingFile?.name
                        val line = psi.containingFile?.viewProvider?.document?.getLineNumber(psi.textOffset)?.plus(1)
                        val prefix = if (fileLoc != null && line != null) {
                            "$fileLoc:$line: "
                        } else if (fileLoc != null) {
                            "$fileLoc: "
                        } else {
                            ""
                        }
                        logger.error("$prefix${diagnostic.defaultMessage}", null)
                    }
                }
            }
        }
    }
}
