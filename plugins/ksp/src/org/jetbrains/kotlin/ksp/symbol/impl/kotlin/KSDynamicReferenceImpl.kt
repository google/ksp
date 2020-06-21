/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.KSDynamicReference
import org.jetbrains.kotlin.ksp.symbol.KSTypeArgument
import org.jetbrains.kotlin.ksp.symbol.KSVisitor
import org.jetbrains.kotlin.ksp.symbol.Origin

class KSDynamicReferenceImpl : KSDynamicReference {
    companion object {
        private val cache = KSDynamicReferenceImpl()
        fun getCached() = cache
    }

    override val origin = Origin.KOTLIN

    override val typeArguments: List<KSTypeArgument> = listOf<KSTypeArgument>()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitDynamicReference(this, data)
    }
}