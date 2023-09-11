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
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierListOwner

val jvmModifierMap = mapOf(
    JvmModifier.PUBLIC to Modifier.PUBLIC,
    JvmModifier.PRIVATE to Modifier.PRIVATE,
    JvmModifier.ABSTRACT to Modifier.ABSTRACT,
    JvmModifier.FINAL to Modifier.FINAL,
    JvmModifier.PROTECTED to Modifier.PROTECTED,
    JvmModifier.STATIC to Modifier.JAVA_STATIC,
    JvmModifier.STRICTFP to Modifier.JAVA_STRICT,
    JvmModifier.NATIVE to Modifier.JAVA_NATIVE,
    JvmModifier.SYNCHRONIZED to Modifier.JAVA_SYNCHRONIZED,
    JvmModifier.TRANSIENT to Modifier.JAVA_TRANSIENT,
    JvmModifier.VOLATILE to Modifier.JAVA_VOLATILE
)

val javaModifiers = setOf(
    Modifier.ABSTRACT,
    Modifier.FINAL,
    Modifier.JAVA_DEFAULT,
    Modifier.JAVA_NATIVE,
    Modifier.JAVA_STATIC,
    Modifier.JAVA_STRICT,
    Modifier.JAVA_SYNCHRONIZED,
    Modifier.JAVA_TRANSIENT,
    Modifier.JAVA_VOLATILE,
    Modifier.PRIVATE,
    Modifier.PROTECTED,
    Modifier.PUBLIC,
)

fun PsiModifierListOwner.toKSModifiers(): Set<Modifier> {
    val modifiers = mutableSetOf<Modifier>()
    modifiers.addAll(
        jvmModifierMap.entries.filter { this.hasModifier(it.key) }
            .map { it.value }
            .toSet()
    )
    if (this.modifierList?.hasExplicitModifier("default") == true) {
        modifiers.add(Modifier.JAVA_DEFAULT)
    }
    return modifiers
}

fun Project.findLocationString(file: PsiFile, offset: Int): String {
    val psiDocumentManager = PsiDocumentManager.getInstance(this)
    val document = psiDocumentManager.getDocument(file) ?: return "<unknown>"
    val lineNumber = document.getLineNumber(offset)
    val offsetInLine = offset - document.getLineStartOffset(lineNumber)
    return "${file.virtualFile.path}: (${lineNumber + 1}, ${offsetInLine + 1})"
}

private fun parseDocString(raw: String): String? {
    val t1 = raw.trim()
    if (!t1.startsWith("/**") || !t1.endsWith("*/"))
        return null
    val lineSep = t1.findAnyOf(listOf("\r\n", "\n", "\r"))?.second ?: ""
    return t1.trim('/').trim('*').lines().joinToString(lineSep) {
        it.trimStart().trimStart('*')
    }
}

inline fun <reified T> PsiElement.findParentOfType(): T? {
    var parent = this.parent
    while (parent != null && parent !is T) {
        parent = parent.parent
    }
    return parent as? T
}

fun <T> Sequence<T>.memoized() = MemoizedSequence(this)
