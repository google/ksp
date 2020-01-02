/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.findParentDeclaration
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl

class KSTypeParameterJavaImpl(val psi: PsiTypeParameter) : KSTypeParameter {
    companion object {
        private val cache = mutableMapOf<PsiTypeParameter, KSTypeParameterJavaImpl>()

        fun getCached(psi: PsiTypeParameter) = cache.getOrPut(psi) { KSTypeParameterJavaImpl(psi) }
    }

    override val annotations: List<KSAnnotation> by lazy {
        psi.annotations.map { KSAnnotationJavaImpl.getCached(it) }
    }

    override val bounds: List<KSTypeReference> by lazy {
        psi.extendsListTypes.map { KSTypeReferenceJavaImpl.getCached(it) }
    }
    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(psi.name ?: "_")
    }

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached(parentDeclaration?.qualifiedName?.asString() ?: "" + "." + simpleName.asString())
    }

    override val typeParameters: List<KSTypeParameter> = emptyList()

    override val parentDeclaration: KSDeclaration? by lazy {
        psi.findParentDeclaration()
    }

    override val containingFile: KSFile? by lazy {
        KSFileJavaImpl.getCached(psi.containingFile as PsiJavaFile)
    }

    override val modifiers: Set<Modifier> = emptySet()

    override val isReified: Boolean = false

    override val name: KSName by lazy {
        KSNameImpl.getCached(psi.name!!)
    }

    override val variance: Variance = Variance.INVARIANT

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeParameter(this, data)
    }
}