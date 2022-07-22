package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance

class KSTypeArgumentLiteImpl(override val type: KSTypeReference, override val variance: Variance) : KSTypeArgument {
    companion object : KSObjectCache<Pair<KSTypeReference, Variance>, KSTypeArgument>() {
        fun getCached(type: KSTypeReference, variance: Variance) = cache.getOrPut(Pair(type, variance)) {
            KSTypeArgumentLiteImpl(type, variance)
        }
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val origin: Origin = Origin.SYNTHETIC

    override val location: Location = NonExistLocation

    override val parent: KSNode? = null

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeArgument(this, data)
    }
}
