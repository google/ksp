/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class KSTypeParameterImpl private constructor(val ktTypeParameter: KtTypeParameter, val owner: KtTypeParameterListOwner) : KSTypeParameter {
    companion object : KSObjectCache<Pair<KtTypeParameter, KtTypeParameterListOwner>, KSTypeParameterImpl>() {
        fun getCached(ktTypeParameter: KtTypeParameter, owner: KtTypeParameterListOwner) = cache.getOrPut(Pair(ktTypeParameter, owner)) { KSTypeParameterImpl(ktTypeParameter, owner) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktTypeParameter.toLocation()
    }

    override val name: KSName by lazy {
        KSNameImpl.getCached(ktTypeParameter.name!!)
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktTypeParameter.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val isReified: Boolean by lazy {
        ktTypeParameter.modifierList?.hasModifier(KtTokens.REIFIED_KEYWORD) ?: false
    }

    override val variance: Variance by lazy {
        when {
            ktTypeParameter.modifierList == null -> Variance.INVARIANT
            ktTypeParameter.modifierList!!.hasModifier(KtTokens.OUT_KEYWORD) -> Variance.COVARIANT
            ktTypeParameter.modifierList!!.hasModifier(KtTokens.IN_KEYWORD) -> Variance.CONTRAVARIANT
            else -> Variance.INVARIANT
        }
    }

    override val bounds: List<KSTypeReference> by lazy {
        val list = mutableListOf(ktTypeParameter.extendsBound)
        list.addAll(
            owner.typeConstraints
                .filter { it.subjectTypeParameterName!!.getReferencedName() == ktTypeParameter.nameAsSafeName.asString() }
                .map { it.boundTypeReference }
        )
        list.filterNotNull().map { KSTypeReferenceImpl.getCached(it) }
    }
    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktTypeParameter.name ?: "_")
    }

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached(ktTypeParameter.containingClassOrObject?.fqName?.asString() ?: "" + "." + simpleName.asString())
    }
    override val typeParameters: List<KSTypeParameter> = emptyList()

    override val parentDeclaration: KSDeclaration? by lazy {
        when (owner) {
            is KtClassOrObject -> KSClassDeclarationImpl.getCached(owner)
            is KtFunction -> KSFunctionDeclarationImpl.getCached(owner)
            is KtProperty -> KSPropertyDeclarationImpl.getCached(owner)
            else -> throw IllegalStateException()
        }
    }

    override val containingFile: KSFile? by lazy {
        KSFileImpl.getCached(ktTypeParameter.containingKtFile)
    }

    override val modifiers: Set<Modifier> by lazy {
        ktTypeParameter.toKSModifiers()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeParameter(this, data)
    }
}