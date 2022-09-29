package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.*

class KSDynamicReferenceImpl private constructor(override val parent: KSNode?) : KSDynamicReference {
    companion object : KSObjectCache<KSTypeReference, KSDynamicReferenceImpl>() {
        fun getCached(parent: KSTypeReference) = cache.getOrPut(parent) { KSDynamicReferenceImpl(parent) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        NonExistLocation
    }

    override val typeArguments: List<KSTypeArgument> = emptyList()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitDynamicReference(this, data)
    }

    override fun toString(): String {
        return "<dynamic type>"
    }
}
