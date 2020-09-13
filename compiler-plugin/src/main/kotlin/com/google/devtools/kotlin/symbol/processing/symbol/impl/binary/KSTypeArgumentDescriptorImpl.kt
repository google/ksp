/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.binary

import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin.KSTypeArgumentImpl
import org.jetbrains.kotlin.types.TypeProjection

class KSTypeArgumentDescriptorImpl private constructor(val descriptor: TypeProjection) : KSTypeArgumentImpl() {
    companion object : KSObjectCache<TypeProjection, KSTypeArgumentDescriptorImpl>() {
        fun getCached(descriptor: TypeProjection) = cache.getOrPut(descriptor) { KSTypeArgumentDescriptorImpl(descriptor) }
    }

    override val origin = Origin.CLASS

    override val location: Location = NonExistLocation

    override val type: KSTypeReference by lazy {
        KSTypeReferenceDescriptorImpl.getCached(descriptor.type)
    }

    override val variance: Variance by lazy {
        if (descriptor.isStarProjection)
            Variance.STAR
        else {
            when (descriptor.projectionKind) {
                org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Variance.CONTRAVARIANT
                org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Variance.COVARIANT
                else -> Variance.INVARIANT
            }
        }
    }

    override val annotations: List<KSAnnotation> = emptyList()
}