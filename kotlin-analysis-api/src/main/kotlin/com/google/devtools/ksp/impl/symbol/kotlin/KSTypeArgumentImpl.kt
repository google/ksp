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

import com.google.devtools.ksp.IdKeyPair
import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KtTypeProjection

class KSTypeArgumentImpl private constructor(
    private val ktTypeProjection: KtTypeProjection,
    override val parent: KSNode?
) : KSTypeArgument {
    companion object : KSObjectCache<IdKeyPair<KtTypeProjection, KSNode?>, KSTypeArgumentImpl>() {
        fun getCached(ktTypeProjection: KtTypeProjection, parent: KSNode? = null) =
            cache.getOrPut(IdKeyPair(ktTypeProjection, parent)) { KSTypeArgumentImpl(ktTypeProjection, parent) }
    }

    override val variance: Variance by lazy {
        when (ktTypeProjection) {
            is KtStarTypeProjection -> Variance.STAR
            is KtTypeArgumentWithVariance -> {
                when (ktTypeProjection.variance) {
                    org.jetbrains.kotlin.types.Variance.INVARIANT -> Variance.INVARIANT
                    org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Variance.CONTRAVARIANT
                    org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Variance.COVARIANT
                    else -> throw IllegalStateException("Unexpected variance")
                }
            }
        }
    }

    override val type: KSTypeReference? by lazy {
        ktTypeProjection.type?.let { KSTypeReferenceImpl.getCached(it, this@KSTypeArgumentImpl) }
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktTypeProjection.type?.annotations() ?: emptySequence()
    }

    override val origin: Origin = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeArgument(this, data)
    }

    override fun toString(): String {
        return "$variance $type"
    }
}
