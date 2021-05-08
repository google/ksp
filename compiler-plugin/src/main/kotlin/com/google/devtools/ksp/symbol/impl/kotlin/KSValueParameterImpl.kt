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

import com.google.devtools.ksp.processing.impl.findAnnotationFromUseSiteTarget
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.synthetic.KSTypeReferenceSyntheticImpl
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.lexer.KtTokens.CROSSINLINE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.NOINLINE_KEYWORD
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

class KSValueParameterImpl private constructor(val ktParameter: KtParameter) : KSValueParameter {
    companion object : KSObjectCache<KtParameter, KSValueParameterImpl>() {
        fun getCached(ktParameter: KtParameter) = cache.getOrPut(ktParameter) { KSValueParameterImpl(ktParameter) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktParameter.toLocation()
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktParameter.filterUseSiteTargetAnnotations().map { KSAnnotationImpl.getCached(it) }.plus(this.findAnnotationFromUseSiteTarget())
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

    override val type: KSTypeReference by lazy {
        ktParameter.typeReference?.let { KSTypeReferenceImpl.getCached(it) } ?: findPropertyForAccessor()?.type ?: KSTypeReferenceSyntheticImpl.getCached(KSErrorType)
    }

    override val hasDefault: Boolean = ktParameter.hasDefaultValue()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }

    override fun toString(): String {
        return name?.asString() ?: "_"
    }

    private fun findPropertyForAccessor(): KSPropertyDeclaration? {
        return (ktParameter.parent?.parent?.parent as? KtProperty)?.let { KSPropertyDeclarationImpl.getCached(it) }
    }
}
