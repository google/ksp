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

package com.google.devtools.ksp.common.visitor

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtTypeAlias

/**
 * Collects classifier names in the [KtFile] using
 * [CollectClassifierNameVisitor].
 */
fun KtFile.collectClassifierNames(): Set<String> {
    val visitor = CollectClassifierNameVisitor()
    accept(visitor)
    return visitor.result
}

/**
 * A [PsiRecursiveElementWalkingVisitor] that collects the names of classes, objects and type aliases.
 */
private class CollectClassifierNameVisitor : PsiRecursiveElementWalkingVisitor() {

    val result = mutableSetOf<String>()

    override fun visitElement(element: PsiElement) {
        if (element.isSkippable()) {
            return
        }
        when (element) {
            is KtClassOrObject -> element.name?.let { result.add(it) }
            is KtTypeAlias -> element.name?.let { result.add(it) }
        }
        super.visitElement(element)
    }

    /**
     * A [PsiElement] may be skippable to avoid deep traversal of the AST.
     *
     * Returns `true` if the [PsiElement] is one of the following:
     *   - An expression that is not a declaration
     *   - An import list
     *   - A package directive
     *   - A doc comment
     *   - A comment
     */
    private fun PsiElement.isSkippable(): Boolean = when (this) {
        is KtExpression -> this !is KtDeclaration
        is KtImportList,
        is KtPackageDirective,
        is KDoc,
        is PsiComment,
        is PsiWhiteSpace -> true

        else -> false
    }
}
