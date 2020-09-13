/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin

import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.psi.KtUserType

class KSDynamicReferenceImpl private constructor() : KSDynamicReference {
    companion object : KSObjectCache<Unit, KSDynamicReferenceImpl>() {
        fun getCached(unused: Unit) = cache.getOrPut(unused) { KSDynamicReferenceImpl() }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        NonExistLocation
    }

    override val typeArguments: List<KSTypeArgument> = listOf<KSTypeArgument>()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitDynamicReference(this, data)
    }

    override fun toString(): String {
        return "<dynamic type>"
    }
}