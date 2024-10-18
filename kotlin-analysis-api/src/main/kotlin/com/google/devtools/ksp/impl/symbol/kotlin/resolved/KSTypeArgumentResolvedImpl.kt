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

package com.google.devtools.ksp.impl.symbol.kotlin.resolved

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.impl.symbol.kotlin.Deferrable
import com.google.devtools.ksp.impl.symbol.kotlin.Restorable
import com.google.devtools.ksp.impl.symbol.kotlin.annotations
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection

class KSTypeArgumentResolvedImpl private constructor(
    private val ktTypeProjection: KaTypeProjection,
    override val parent: KSNode?
) : KSTypeArgument, Deferrable {
    companion object : KSObjectCache<IdKeyPair<KaTypeProjection, KSNode?>, KSTypeArgumentResolvedImpl>() {
        fun getCached(ktTypeProjection: KaTypeProjection, parent: KSNode? = null) =
            cache.getOrPut(IdKeyPair(ktTypeProjection, parent)) { KSTypeArgumentResolvedImpl(ktTypeProjection, parent) }
    }

    override val variance: Variance by lazy {
        when (ktTypeProjection) {
            is KaStarTypeProjection -> Variance.STAR
            is KaTypeArgumentWithVariance -> {
                when (ktTypeProjection.variance) {
                    org.jetbrains.kotlin.types.Variance.INVARIANT -> Variance.INVARIANT
                    org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Variance.CONTRAVARIANT
                    org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Variance.COVARIANT
                    else -> throw IllegalStateException("Unexpected variance")
                }
            }
        }
    }

    private val kaType: KaType? by lazy {
        ktTypeProjection.type?.abbreviation ?: ktTypeProjection.type
    }

    override val type: KSTypeReference? by lazy {
        kaType?.let { KSTypeReferenceResolvedImpl.getCached(it, this@KSTypeArgumentResolvedImpl) }
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        kaType?.annotations(this) ?: emptySequence()
    }

    override val origin: Origin = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location = NonExistLocation

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeArgument(this, data)
    }

    override fun toString(): String {
        return "$variance $type"
    }

    override fun defer(): Restorable? {
        TODO("Not yet implemented")
    }
}
