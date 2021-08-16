package com.google.devtools.ksp.symbol.impl.synthetic

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache

class KSTypeReferenceSyntheticImpl(val ksType: KSType) : KSTypeReference {
    companion object : KSObjectCache<KSType, KSTypeReferenceSyntheticImpl>() {
        fun getCached(ksType: KSType) = KSTypeReferenceSyntheticImpl.cache.getOrPut(ksType) {
            KSTypeReferenceSyntheticImpl(ksType)
        }
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val element: KSReferenceElement? = null

    override val location: Location = NonExistLocation

    override val modifiers: Set<Modifier> = emptySet()

    override val origin: Origin = Origin.SYNTHETIC

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override fun resolve(): KSType {
        return ksType
    }

    override fun toString(): String {
        return ksType.toString()
    }
}
