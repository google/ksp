package com.google.devtools.ksp

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.siblings

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

fun KtModifierListOwner.toKSModifiers(): Set<Modifier> {
    val modifierList = this.modifierList
    return modifierList.toKSModifiers()
}

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

fun PsiElement.getDocString(): String? =
    this.firstChild.siblings().firstOrNull { it is PsiComment }?.let {
        parseDocString(it.text)
    }

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

inline fun <reified T> PsiElement.findParentOfType(): T? {
    var parent = this.parent
    while (parent != null && parent !is T) {
        parent = parent.parent
    }
    return parent as? T
}

fun <T> Sequence<T>.memoized() = MemoizedSequence(this)
