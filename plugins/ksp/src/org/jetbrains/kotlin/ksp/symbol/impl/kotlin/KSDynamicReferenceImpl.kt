/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.KSDynamicReference
import org.jetbrains.kotlin.ksp.symbol.KSTypeArgument
import org.jetbrains.kotlin.ksp.symbol.KSVisitor
import org.jetbrains.kotlin.ksp.symbol.Origin
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.psi.KtUserType

class KSDynamicReferenceImpl private constructor() : KSDynamicReference {
    companion object : KSObjectCache<Unit, KSDynamicReferenceImpl>() {
        fun getCached(unused: Unit) = cache.getOrPut(unused) { KSDynamicReferenceImpl() }
    }

    override val origin = Origin.KOTLIN

    override val typeArguments: List<KSTypeArgument> = listOf<KSTypeArgument>()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitDynamicReference(this, data)
    }
}