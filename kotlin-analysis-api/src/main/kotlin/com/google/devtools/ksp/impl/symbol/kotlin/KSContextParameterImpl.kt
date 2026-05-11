/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import com.google.devtools.ksp.common.lazyMemoizedSequence
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

class KSContextParameterImpl @OptIn(KaExperimentalApi::class) private constructor(
    private val ktContextParameterSymbol: KaContextParameterSymbol,
    override val parent: KSAnnotated
) : KSValueParameter, Deferrable {
    @OptIn(KaExperimentalApi::class)
    companion object : KSObjectCache<KaContextParameterSymbol, KSContextParameterImpl>() {
        fun getCached(ktContextParameterSymbol: KaContextParameterSymbol, parent: KSAnnotated) =
            cache.getOrPut(ktContextParameterSymbol) { KSContextParameterImpl(ktContextParameterSymbol, parent) }
    }

    @OptIn(KaExperimentalApi::class)
    override val name: KSName? by lazy {
        KSNameImpl.getCached(ktContextParameterSymbol.name.asString())
    }

    @OptIn(SymbolInternals::class, KaExperimentalApi::class)
    override val type: KSTypeReference by lazy {
        // TODO: avoid eager resolution by using PSI.
        // KaFirValueParameterSymbol extracts and returns the element type of a vararg.
        // That logic needs to be replicated if we resolve the PSI via
        // analyze { KtTypeReference.type }.
        KSTypeReferenceResolvedImpl.getCached(
            ktContextParameterSymbol.returnType.abbreviationOrSelf,
            this@KSContextParameterImpl
        )
    }

    override val isVararg: Boolean = false

    @OptIn(KaExperimentalApi::class)
    override val isNoInline: Boolean = false

    @OptIn(KaExperimentalApi::class)
    override val isCrossInline: Boolean = false

    override val isVal: Boolean = false

    override val isVar: Boolean = false

    override val hasDefault: Boolean = false

    @OptIn(KaExperimentalApi::class)
    override val annotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        ktContextParameterSymbol.annotations(this)
    }

    @OptIn(KaExperimentalApi::class)
    override val origin: Origin by lazy {
        val symbolOrigin = mapAAOrigin(ktContextParameterSymbol)
        if (symbolOrigin == Origin.KOTLIN && ktContextParameterSymbol.psi == null) {
            Origin.SYNTHETIC
        } else {
            symbolOrigin
        }
    }

    @OptIn(KaExperimentalApi::class)
    override val location: Location by lazy {
        ktContextParameterSymbol.psi?.toLocation() ?: NonExistLocation
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }

    override fun toString(): String {
        return name?.asString() ?: "_"
    }

    @OptIn(KaExperimentalApi::class)
    override fun defer(): Restorable? {
        val other = (parent as Deferrable).defer() ?: return null
        return ktContextParameterSymbol.defer inner@{
            getCached(it, other.restore() ?: return@inner null)
        }
    }
}
