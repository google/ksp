/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.memoized
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.processing.impl.KSObjectCache
import com.google.devtools.ksp.processing.impl.KSTypeReferenceSyntheticImpl
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.KSExpectActual
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Variance
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner

class KSTypeParameterImpl private constructor(val ktTypeParameter: KtTypeParameter) :
    KSTypeParameter,
    KSDeclarationImpl(ktTypeParameter),
    KSExpectActual by KSExpectActualNoImpl() {
    companion object : KSObjectCache<KtTypeParameter, KSTypeParameterImpl>() {
        fun getCached(ktTypeParameter: KtTypeParameter) =
            cache.getOrPut(ktTypeParameter) { KSTypeParameterImpl(ktTypeParameter) }
    }

    override val name: KSName by lazy {
        KSNameImpl.getCached(ktTypeParameter.name!!)
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

    private val owner: KtTypeParameterListOwner by lazy {
        (parentDeclaration as KSDeclarationImpl).ktDeclaration as KtTypeParameterListOwner
    }

    override val bounds: Sequence<KSTypeReference> by lazy {
        val list = sequenceOf(ktTypeParameter.extendsBound)
        list.plus(
            owner.typeConstraints
                .filter {
                    it.subjectTypeParameterName!!.getReferencedName() == ktTypeParameter.nameAsSafeName.asString()
                }
                .map { it.boundTypeReference }
        ).filterNotNull().map { KSTypeReferenceImpl.getCached(it) }.ifEmpty {
            sequenceOf(
                KSTypeReferenceSyntheticImpl.getCached(ResolverImpl.instance!!.builtIns.anyType.makeNullable(), this)
            )
        }.memoized()
    }

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached("${this.parentDeclaration!!.qualifiedName!!.asString()}.${simpleName.asString()}")
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktTypeParameter.name ?: "_")
    }

    override val typeParameters: List<KSTypeParameter> = emptyList()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeParameter(this, data)
    }
}
