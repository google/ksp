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
package com.google.devtools.ksp.symbol.impl

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.siblings

private fun parseDocString(raw: String): String? {
    val t1 = raw.trim()
    if (!t1.startsWith("/**") || !t1.endsWith("*/"))
        return null
    val lineSep = t1.findAnyOf(listOf("\r\n", "\n", "\r"))?.second ?: ""
    return t1.trim('/').trim('*').lines().joinToString(lineSep) {
        it.trimStart().trimStart('*')
    }
}

fun PsiElement.getDocString(): String? =
    this.firstChild.siblings().firstOrNull { it is PsiComment }?.let {
        parseDocString(it.text)
    }

fun KtModifierListOwner.toKSModifiers(): Set<Modifier> {
    val modifierList = this.modifierList
    return modifierList.toKSModifiers()
}

fun KtModifierList?.toKSModifiers(): Set<Modifier> {
    if (this == null)
        return emptySet()
    val modifiers = mutableSetOf<Modifier>()
    modifiers.addAll(
        modifierMap.entries
            .filter { hasModifier(it.key) }
            .map { it.value }
    )
    return modifiers
}

val modifierMap = mapOf(
    KtTokens.PUBLIC_KEYWORD to Modifier.PUBLIC,
    KtTokens.PRIVATE_KEYWORD to Modifier.PRIVATE,
    KtTokens.INTERNAL_KEYWORD to Modifier.INTERNAL,
    KtTokens.PROTECTED_KEYWORD to Modifier.PROTECTED,
    KtTokens.IN_KEYWORD to Modifier.IN,
    KtTokens.OUT_KEYWORD to Modifier.OUT,
    KtTokens.OVERRIDE_KEYWORD to Modifier.OVERRIDE,
    KtTokens.LATEINIT_KEYWORD to Modifier.LATEINIT,
    KtTokens.ENUM_KEYWORD to Modifier.ENUM,
    KtTokens.SEALED_KEYWORD to Modifier.SEALED,
    KtTokens.ANNOTATION_KEYWORD to Modifier.ANNOTATION,
    KtTokens.DATA_KEYWORD to Modifier.DATA,
    KtTokens.INNER_KEYWORD to Modifier.INNER,
    KtTokens.FUN_KEYWORD to Modifier.FUN,
    KtTokens.VALUE_KEYWORD to Modifier.VALUE,
    KtTokens.SUSPEND_KEYWORD to Modifier.SUSPEND,
    KtTokens.TAILREC_KEYWORD to Modifier.TAILREC,
    KtTokens.OPERATOR_KEYWORD to Modifier.OPERATOR,
    KtTokens.INFIX_KEYWORD to Modifier.INFIX,
    KtTokens.INLINE_KEYWORD to Modifier.INLINE,
    KtTokens.EXTERNAL_KEYWORD to Modifier.EXTERNAL,
    KtTokens.ABSTRACT_KEYWORD to Modifier.ABSTRACT,
    KtTokens.FINAL_KEYWORD to Modifier.FINAL,
    KtTokens.OPEN_KEYWORD to Modifier.OPEN,
    KtTokens.VARARG_KEYWORD to Modifier.VARARG,
    KtTokens.NOINLINE_KEYWORD to Modifier.NOINLINE,
    KtTokens.CROSSINLINE_KEYWORD to Modifier.CROSSINLINE,
    KtTokens.REIFIED_KEYWORD to Modifier.REIFIED,
    KtTokens.EXPECT_KEYWORD to Modifier.EXPECT,
    KtTokens.ACTUAL_KEYWORD to Modifier.ACTUAL,
    KtTokens.CONST_KEYWORD to Modifier.CONST
)

fun KtClassOrObject.getClassType(): ClassKind {
    return when (this) {
        is KtObjectDeclaration -> ClassKind.OBJECT
        is KtEnumEntry -> ClassKind.ENUM_ENTRY
        is KtClass -> when {
            this.isEnum() -> ClassKind.ENUM_CLASS
            this.isInterface() -> ClassKind.INTERFACE
            this.isAnnotation() -> ClassKind.ANNOTATION_CLASS
            else -> ClassKind.CLASS
        }
        else -> throw IllegalStateException("Unexpected psi type ${this.javaClass}, $ExceptionMessage")
    }
}
