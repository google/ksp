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

package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol

class KSValueParameterImpl private constructor(
    private val ktValueParameterSymbol: KtValueParameterSymbol
) : KSValueParameter {
    companion object : KSObjectCache<KtValueParameterSymbol, KSValueParameterImpl>() {
        fun getCached(ktValueParameterSymbol: KtValueParameterSymbol) =
            cache.getOrPut(ktValueParameterSymbol) { KSValueParameterImpl(ktValueParameterSymbol) }
    }

    override val name: KSName? by lazy {
        KSNameImpl.getCached(ktValueParameterSymbol.name.asString())
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl(ktValueParameterSymbol.returnType)
    }

    override val isVararg: Boolean by lazy {
        ktValueParameterSymbol.isVararg
    }

    override val isNoInline: Boolean
        get() = TODO("Not yet implemented")

    override val isCrossInline: Boolean
        get() = TODO("Not yet implemented")

    override val isVal: Boolean
        get() = TODO("Not yet implemented")

    override val isVar: Boolean
        get() = TODO("Not yet implemented")

    override val hasDefault: Boolean by lazy {
        ktValueParameterSymbol.hasDefaultValue
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktValueParameterSymbol.annotations.asSequence().map { KSAnnotationImpl.getCached(it) }
    }
    override val origin: Origin by lazy {
        mapAAOrigin(ktValueParameterSymbol.origin)
    }

    override val location: Location by lazy {
        ktValueParameterSymbol.psi?.toLocation() ?: NonExistLocation
    }
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }
}
