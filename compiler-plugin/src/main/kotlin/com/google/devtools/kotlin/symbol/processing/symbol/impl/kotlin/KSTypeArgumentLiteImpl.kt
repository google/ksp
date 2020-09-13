/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin

import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.psi.KtTypeReference

class KSTypeArgumentLiteImpl private constructor(override val type: KSTypeReference, override val variance: Variance) : KSTypeArgumentImpl() {
    companion object : KSObjectCache<Pair<KSTypeReference, Variance>, KSTypeArgumentLiteImpl>() {
        fun getCached(type: KSTypeReference, variance: Variance) = cache.getOrPut(Pair(type, variance)) {
            KSTypeArgumentLiteImpl(type, variance)
        }

        fun getCached(type: KtTypeReference) = cache.getOrPut(Pair(KSTypeReferenceImpl.getCached(type), Variance.INVARIANT)) {
            KSTypeArgumentLiteImpl(KSTypeReferenceImpl.getCached(type), Variance.INVARIANT)
        }
    }

    override val origin = Origin.KOTLIN

    override val location: Location = NonExistLocation

    override val annotations: List<KSAnnotation> = type.annotations
}