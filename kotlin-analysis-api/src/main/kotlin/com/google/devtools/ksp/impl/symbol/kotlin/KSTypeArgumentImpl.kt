package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance
import org.jetbrains.kotlin.analysis.api.KtStarProjectionTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance

class KSTypeArgumentImpl(private val ktTypeArgument: KtTypeArgument) : KSTypeArgument {
    override val variance: Variance by lazy {
        when (ktTypeArgument) {
            is KtStarProjectionTypeArgument -> Variance.STAR
            is KtTypeArgumentWithVariance -> {
                when (ktTypeArgument.variance) {
                    org.jetbrains.kotlin.types.Variance.INVARIANT -> Variance.INVARIANT
                    org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Variance.COVARIANT
                    org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Variance.CONTRAVARIANT
                    else -> throw IllegalStateException("Unexpected variance")
                }
            }
        }
    }

    override val type: KSTypeReference?
        get() = TODO("Not yet implemented")
    override val annotations: Sequence<KSAnnotation>
        get() = TODO("Not yet implemented")
    override val origin: Origin = Origin.KOTLIN
    override val location: Location
        get() = TODO("Not yet implemented")
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeArgument(this, data)
    }
}
