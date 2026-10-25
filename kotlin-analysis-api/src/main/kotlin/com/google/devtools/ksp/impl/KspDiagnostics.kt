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
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil
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
    fun runTypeCheck(psiJavaFiles: Collection<PsiJavaFile>, ktFiles: Collection<KtFile>, logger: KSPLogger) {
        if (psiJavaFiles.isEmpty() && ktFiles.isEmpty()) return

        runJava(psiJavaFiles, logger)
        runKotlin(ktFiles, logger)
    }

    fun runKotlin(ktFiles: Collection<KtFile>, logger: KSPLogger) {
        analyze {
            for (file in ktFiles) {
                val diagnostics = file.collectDiagnostics(KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
                for (diagnostic in diagnostics) {
                    if (diagnostic.severity == KaSeverity.ERROR) {
                        logger.error(diagnostic.psi.withLocation(diagnostic.defaultMessage), null)
                    }
                }
            }
        }
    }

    fun runJava(psiJavaFiles: Collection<PsiJavaFile>, logger: KSPLogger) {
        for (file in psiJavaFiles) {
            val messages = linkedSetOf<String>()

            val syntaxErrors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
            if (syntaxErrors.isNotEmpty()) {
                syntaxErrors.mapTo(messages) { it.withLocation(it.errorDescription) }
            } else {
                file.accept(UnresolveReferenceCollector(messages))
            }

            messages.forEach { logger.error(it, null) }
        }
    }
}

class UnresolveReferenceCollector(
    private val messages: MutableSet<String>
) : JavaRecursiveElementWalkingVisitor() {
    // Javadoc references are not resolved by the compiler, don't descend inot them.
    override fun visitDocComment(comment: PsiDocComment) = Unit

    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        super.visitReferenceElement(reference)
        reference.collectIfUnresolved()
    }

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        super.visitReferenceExpression(expression)
        expression.collectIfUnresolved()
    }

    private fun PsiJavaCodeReferenceElement.isResolved(): Boolean =
        this is PsiMethodReferenceExpression || resolve() != null || hasUnresolvedQualifier() ||
            this.getName() == null

    private fun PsiJavaCodeReferenceElement.getName(): String? = referenceName ?: text

    private fun PsiJavaCodeReferenceElement.collectIfUnresolved() = if (!this.isResolved()) {
        messages.add((referenceNameElement ?: this).withLocation("Unresolved reference '${this.getName()}'"))
    } else null

    private fun PsiJavaCodeReferenceElement.hasUnresolvedQualifier(): Boolean =
        when (val qualifier = qualifier) {
            null -> false
            is PsiJavaCodeReferenceElement -> qualifier.resolve() == null
            is PsiExpression -> qualifier.type == null
            else -> false
        }
}

private fun PsiElement.withLocation(message: String): String {
    val fileLoc = containingFile?.name
    val line = containingFile?.viewProvider?.document?.getLineNumber(textOffset)?.plus(1)
    val prefix = if (fileLoc != null && line != null) {
        "$fileLoc:$line: "
    } else if (fileLoc != null) {
        "$fileLoc: "
    } else {
        ""
    }

    return "$prefix$message"
}
