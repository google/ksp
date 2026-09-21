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
package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.common.lazyMemoizedSequence
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSContextParameter
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.KSVisitorNext
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.psi.KtContextReceiver

@OptIn(KaExperimentalApi::class)
class KSContextParameterImpl private constructor(val kaContextParameterSymbol: KaContextParameterSymbol) :
    KSContextParameter {

    companion object : KSObjectCache<KaContextParameterSymbol, KSContextParameterImpl>() {
        fun getCached(kaContextParameterSymbol: KaContextParameterSymbol) =
            cache.getOrPut(kaContextParameterSymbol) { KSContextParameterImpl(kaContextParameterSymbol) }
    }

    override val name: KSName? by lazy {
        KSNameImpl.getCached(kaContextParameterSymbol.name.asString())
    }

    override val type: KSTypeReference by lazy {
        // Try to get the PSI type reference to avoid doing anything expensive but fall back to using AA.
        val psiTypeReference = (kaContextParameterSymbol.psiIfSource() as? KtContextReceiver)?.typeReference()
        if (psiTypeReference != null) {
            KSTypeReferenceImpl.getCached(psiTypeReference)
        } else {
            KSTypeReferenceResolvedImpl.getCached(type = kaContextParameterSymbol.returnType, parent = parent)
        }
    }

    override val annotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        kaContextParameterSymbol.annotations(parent = parent)
    }

    override val origin: Origin by lazy {
        mapAAOrigin(kaContextParameterSymbol)
    }

    override val location: Location by lazy {
        kaContextParameterSymbol.psi.toLocation()
    }

    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = when (visitor) {
        is KSVisitorNext -> visitor.visitContextParameter(this, data)
        else -> visitor.visitAnnotated(this, data)
    }
}
