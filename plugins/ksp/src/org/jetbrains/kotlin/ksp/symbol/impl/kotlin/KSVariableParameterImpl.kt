/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.lexer.KtTokens.CROSSINLINE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.NOINLINE_KEYWORD
import org.jetbrains.kotlin.psi.KtParameter

class KSVariableParameterImpl private constructor(val ktParameter: KtParameter) : KSVariableParameter {
    companion object : KSObjectCache<KtParameter, KSVariableParameterImpl>() {
        fun getCached(ktParameter: KtParameter) = cache.getOrPut(ktParameter) { KSVariableParameterImpl(ktParameter) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktParameter.toLocation()
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktParameter.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val isCrossInline: Boolean = ktParameter.hasModifier(CROSSINLINE_KEYWORD)

    override val isNoInline: Boolean = ktParameter.hasModifier(NOINLINE_KEYWORD)

    override val isVararg: Boolean = ktParameter.isVarArg

    override val isVal = ktParameter.hasValOrVar() && !ktParameter.isMutable

    override val isVar = ktParameter.hasValOrVar() && ktParameter.isMutable

    override val name: KSName? by lazy {
        if (ktParameter.name == null) {
            null
        } else {
            KSNameImpl.getCached(ktParameter.name!!)
        }
    }

    override val type: KSTypeReference? by lazy {
        ktParameter.typeReference?.let { KSTypeReferenceImpl.getCached(it) }
    }

    override val hasDefault: Boolean = ktParameter.hasDefaultValue()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitVariableParameter(this, data)
    }
}