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

import java.security.MessageDigest
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer.SourceLocation

internal open class ProblemsKspDiagnosticReporter @Inject constructor(
    private val problems: Problems,
    private val delegate: KspDiagnosticReporter,
    private val suppressConsole: Boolean,
    private val taskPath: String,
) : KspDiagnosticReporter {
    private data class Pending(
        val kind: Diagnostic.Kind,
        val message: String,
        val location: SourceLocation?,
    )

    private val pending = ConcurrentLinkedQueue<Pending>()

    override fun report(diagnostic: Diagnostic<out JavaFileObject>) {
        when (diagnostic.kind) {
            Diagnostic.Kind.ERROR, Diagnostic.Kind.WARNING, Diagnostic.Kind.MANDATORY_WARNING -> Unit
            else -> return
        }
        pending.add(Pending(diagnostic.kind, diagnostic.getMessage(null), diagnostic.toBtaSourceLocation()))
        if (!suppressConsole) delegate.report(diagnostic)
    }

    override fun flush() {
        delegate.flush()
        // Compiler callbacks may use other threads. Report Problems in the worker's build operation.
        while (true) {
            val diagnostic = pending.poll() ?: return
            val isError = diagnostic.kind == Diagnostic.Kind.ERROR
            val severity = if (isError) Severity.ERROR else Severity.WARNING
            val location = diagnostic.location
            val identity = listOf(
                taskPath,
                severity.name,
                diagnostic.message,
                location?.path.orEmpty(),
                location?.line?.toString().orEmpty(),
                location?.column?.toString().orEmpty(),
            ).joinToString("") { "${it.length}:$it" }
            val fingerprint = MessageDigest.getInstance("SHA-256")
                .digest(identity.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val id = ProblemId.create(
                "${if (isError) "error" else "warning"}-$fingerprint",
                if (isError) "KSP error" else "KSP warning",
                ProblemGroup.create("ksp", "Kotlin Symbol Processing"),
            )
            problems.reporter.report(id) { spec ->
                spec.contextualLabel(diagnostic.message)
                spec.severity(severity)
                location?.let {
                    when {
                        it.line > 0 && it.column > 0 -> spec.lineInFileLocation(it.path, it.line, it.column)
                        it.line > 0 -> spec.lineInFileLocation(it.path, it.line)
                        else -> spec.fileLocation(it.path)
                    }
                }
            }
        }
    }
}
