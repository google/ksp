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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.psi.KtParameter

class KSValueParameterImpl private constructor(
    private val ktValueParameterSymbol: KaValueParameterSymbol,
    override val parent: KSAnnotated
) : KSValueParameter, Deferrable {
    companion object : KSObjectCache<KaValueParameterSymbol, KSValueParameterImpl>() {
        fun getCached(ktValueParameterSymbol: KaValueParameterSymbol, parent: KSAnnotated) =
            cache.getOrPut(ktValueParameterSymbol) { KSValueParameterImpl(ktValueParameterSymbol, parent) }
    }

    override val name: KSName? by lazy {
        if (origin == Origin.SYNTHETIC && parent is KSPropertySetter) {
            KSNameImpl.getCached("value")
        } else {
            KSNameImpl.getCached(ktValueParameterSymbol.name.asString())
        }
    }

    @OptIn(SymbolInternals::class)
    override val type: KSTypeReference by lazy {
        // FIXME: temporary workaround before upstream fixes java type refs.
        if (origin == Origin.JAVA || origin == Origin.JAVA_LIB) {
            ((ktValueParameterSymbol as KaFirValueParameterSymbol).firSymbol.fir as? FirJavaValueParameter)?.let {
                // can't get containing class for FirJavaValueParameter, using empty stack for now.
                it.returnTypeRef =
                    it.returnTypeRef.resolveIfJavaType(it.moduleData.session, JavaTypeParameterStack.EMPTY, null)
            }
        }
        (ktValueParameterSymbol.psiIfSource() as? KtParameter)?.typeReference
            ?.let { KSTypeReferenceImpl.getCached(it, this) }
            ?: KSTypeReferenceResolvedImpl.getCached(
                ktValueParameterSymbol.returnType.abbreviationOrSelf,
                this@KSValueParameterImpl
            )
    }

    override val isVararg: Boolean by lazy {
        ktValueParameterSymbol.isVararg
    }

    override val isNoInline: Boolean
        get() = ktValueParameterSymbol.isNoinline

    override val isCrossInline: Boolean
        get() = ktValueParameterSymbol.isCrossinline

    override val isVal: Boolean
        get() = (ktValueParameterSymbol.psi as? KtParameter)?.let { it.hasValOrVar() && !it.isMutable } ?: false

    override val isVar: Boolean
        get() = (ktValueParameterSymbol.psi as? KtParameter)?.let { it.hasValOrVar() && it.isMutable } ?: false

    override val hasDefault: Boolean by lazy {
        ktValueParameterSymbol.hasDefaultValue
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktValueParameterSymbol.annotations(this).plus(findAnnotationFromUseSiteTarget())
    }
    override val origin: Origin by lazy {
        val symbolOrigin = mapAAOrigin(ktValueParameterSymbol)
        if (symbolOrigin == Origin.KOTLIN && ktValueParameterSymbol.psi == null) {
            Origin.SYNTHETIC
        } else {
            symbolOrigin
        }
    }

    override val location: Location by lazy {
        ktValueParameterSymbol.psi?.toLocation() ?: NonExistLocation
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }

    override fun toString(): String {
        return name?.asString() ?: "_"
    }

    override fun defer(): Restorable? {
        val other = (parent as Deferrable).defer() ?: return null
        return ktValueParameterSymbol.defer inner@{
            getCached(it, other.restore() ?: return@inner null)
        }
    }
}
