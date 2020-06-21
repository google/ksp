/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.KtTypeProjection

abstract class KSTypeArgumentImpl : KSTypeArgument {
    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeArgument(this, data)
    }

    override fun hashCode(): Int {
        return type.hashCode() * 31 + variance.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KSTypeArgument)
            return false

        return variance == other.variance && type == other.type
    }
}

class KSTypeArgumentKtImpl(val ktTypeArgument: KtTypeProjection) : KSTypeArgumentImpl() {
    companion object {
        private val cache = mutableMapOf<KtTypeProjection, KSTypeArgumentKtImpl>()

        fun getCached(ktTypeArgument: KtTypeProjection) = cache.getOrPut(ktTypeArgument) { KSTypeArgumentKtImpl(ktTypeArgument) }
    }

    override val origin = Origin.KOTLIN

    override val variance: Variance by lazy {
        when (ktTypeArgument.projectionKind) {
            KtProjectionKind.STAR -> Variance.STAR
            KtProjectionKind.IN -> Variance.COVARIANT
            KtProjectionKind.NONE -> Variance.INVARIANT
            KtProjectionKind.OUT -> Variance.CONTRAVARIANT
        }
    }

    override val type: KSTypeReference? by lazy {
        if (ktTypeArgument.typeReference != null) {
            KSTypeReferenceImpl.getCached(ktTypeArgument.typeReference!!)
        } else {
            null
        }
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktTypeArgument.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }
}