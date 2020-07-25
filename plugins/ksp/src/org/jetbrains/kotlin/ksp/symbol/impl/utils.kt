/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.ClassKind
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSFunctionDeclarationDescriptorImpl
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSTypeArgumentDescriptorImpl
import org.jetbrains.kotlin.ksp.symbol.impl.java.KSClassDeclarationJavaImpl
import org.jetbrains.kotlin.ksp.symbol.impl.java.KSFunctionDeclarationJavaImpl
import org.jetbrains.kotlin.ksp.symbol.impl.java.KSPropertyDeclarationJavaImpl
import org.jetbrains.kotlin.ksp.symbol.impl.java.KSTypeArgumentJavaImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace

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

fun KtModifierListOwner.toKSModifiers(): Set<Modifier> {
    val modifiers = mutableSetOf<Modifier>()
    val modifierList = this.modifierList ?: return modifiers
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
        KtTokens.ACTUAL_KEYWORD to Modifier.ACTUAL
    )
    modifiers.addAll(
        modifierMap.entries
            .filter { modifierList.hasModifier(it.key) }
            .map { it.value }
    )
    return modifiers
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

fun MemberDescriptor.toKSModifiers(): Set<Modifier> {
    val modifiers = mutableSetOf<Modifier>()
    if (this.isActual) {
        modifiers.add(Modifier.ACTUAL)
    }
    if (this.isExpect) {
        modifiers.add(Modifier.EXPECT)
    }
    if (this.isExternal) {
        modifiers.add(Modifier.EXTERNAL)
    }
    when (this.modality) {
        Modality.SEALED -> modifiers.add(Modifier.SEALED)
        Modality.FINAL -> modifiers.add(Modifier.FINAL)
        Modality.OPEN -> modifiers.add(Modifier.OPEN)
        Modality.ABSTRACT -> modifiers.add(Modifier.ABSTRACT)
    }
    when (this.visibility) {
        Visibilities.PUBLIC -> modifiers.add(Modifier.PUBLIC)
        Visibilities.PROTECTED, JavaVisibilities.PROTECTED_AND_PACKAGE -> modifiers.add(Modifier.PROTECTED)
        Visibilities.PRIVATE -> modifiers.add(Modifier.PRIVATE)
        Visibilities.INTERNAL -> modifiers.add(Modifier.INTERNAL)
        // Since there is no modifier for package-private, use No modifier to tell if a symbol from binary is package private.
        JavaVisibilities.PACKAGE_VISIBILITY, JavaVisibilities.PROTECTED_STATIC_VISIBILITY -> Unit
        else -> throw IllegalStateException("unhandled visibility: ${this.visibility}")
    }
    return modifiers
}

fun FunctionDescriptor.toFunctionKSModifiers(): Set<Modifier> {
    val modifiers = mutableSetOf<Modifier>()
    if (this.isSuspend) {
        modifiers.add(Modifier.SUSPEND)
    }
    if (this.isTailrec) {
        modifiers.add(Modifier.TAILREC)
    }
    if (this.isInline) {
        modifiers.add(Modifier.INLINE)
    }
    if (this.isInfix) {
        modifiers.add(Modifier.INFIX)
    }
    if (this.isOperator) {
        modifiers.add(Modifier.OPERATOR)
    }
    if (this.overriddenDescriptors.isNotEmpty()) {
        modifiers.add(Modifier.OVERRIDE)
    }
    return modifiers
}

fun PsiElement.findParentDeclaration(): KSDeclaration? {
    var parent = this.parent

    while (parent != null && parent !is KtDeclaration && parent !is KtFile && parent !is PsiClass && parent !is PsiMethod && parent !is PsiJavaFile) {
        parent = parent.parent
    }

    return when (parent) {
        is KtClassOrObject -> KSClassDeclarationImpl.getCached(parent)
        is KtFile -> null
        is KtFunction -> KSFunctionDeclarationImpl.getCached(parent)
        is PsiClass -> KSClassDeclarationJavaImpl.getCached(parent)
        is PsiJavaFile -> null
        is PsiMethod -> KSFunctionDeclarationJavaImpl.getCached(parent)
        else -> null
    }
}

// TODO: handle local functions/classes correctly
fun List<KtElement>.getKSDeclarations() =
    this.mapNotNull {
        when (it) {
            is KtFunction -> KSFunctionDeclarationImpl.getCached(it)
            is KtProperty -> KSPropertyDeclarationImpl.getCached(it)
            is KtClassOrObject ->
                if (it.containingClassOrObject?.hasModifier(KtTokens.ENUM_KEYWORD) == true) KSEnumEntryDeclarationImpl.getCached(it)
                else KSClassDeclarationImpl.getCached(it)
            else -> null
        }
    }

fun KtClassOrObject.getClassType(): ClassKind {
    return when (this) {
        is KtObjectDeclaration -> ClassKind.OBJECT
        is KtClass -> when {
            this.isEnum() -> ClassKind.ENUM
            this.isInterface() -> ClassKind.INTERFACE
            else -> ClassKind.CLASS
        }
        else -> throw IllegalStateException()
    }
}

fun List<PsiElement>.getKSJavaDeclarations() =
    this.mapNotNull {
        when (it) {
            is PsiClass -> KSClassDeclarationJavaImpl.getCached(it)
            is PsiMethod -> KSFunctionDeclarationJavaImpl.getCached(it)
            is PsiField -> KSPropertyDeclarationJavaImpl.getCached(it)
            else -> null
        }
    }

fun org.jetbrains.kotlin.types.Variance.toKSVariance(): Variance {
    return when (this) {
        org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Variance.COVARIANT
        org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Variance.CONTRAVARIANT
        org.jetbrains.kotlin.types.Variance.INVARIANT -> Variance.INVARIANT
        else -> throw IllegalStateException()
    }
}

fun KSTypeReference.toKotlinType() = (resolve() as KSTypeImpl).kotlinType

internal fun KotlinType.replaceTypeArguments(newArguments: List<KSTypeArgument>): KotlinType =
    replace(newArguments.mapIndexed { index, ksTypeArgument ->
        val variance = when (ksTypeArgument.variance) {
            Variance.INVARIANT -> org.jetbrains.kotlin.types.Variance.INVARIANT
            Variance.COVARIANT -> org.jetbrains.kotlin.types.Variance.OUT_VARIANCE
            Variance.CONTRAVARIANT -> org.jetbrains.kotlin.types.Variance.IN_VARIANCE
            Variance.STAR -> return@mapIndexed StarProjectionImpl(constructor.parameters[index])
        }

        val type = when (ksTypeArgument) {
            is KSTypeArgumentKtImpl, is KSTypeArgumentJavaImpl, is KSTypeArgumentLiteImpl -> ksTypeArgument.type!!
            is KSTypeArgumentDescriptorImpl -> return@mapIndexed ksTypeArgument.descriptor
            else -> throw IllegalStateException()
        }.toKotlinType()

        TypeProjectionImpl(variance, type)
    })

internal fun FunctionDescriptor.toKSFunctionDeclaration(): KSFunctionDeclaration {
    if (this.kind != CallableMemberDescriptor.Kind.DECLARATION) return KSFunctionDeclarationDescriptorImpl.getCached(this)
    val psi = this.findPsi() ?: return KSFunctionDeclarationDescriptorImpl.getCached(this)
    return when (psi) {
        is KtFunction -> KSFunctionDeclarationImpl.getCached(psi)
        is PsiMethod -> KSFunctionDeclarationJavaImpl.getCached(psi)
        else -> throw IllegalStateException("unexpected psi: ${psi.javaClass}")
    }
}