package com.google.devtools.ksp.impl.symbol.java

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.symbol.*

class KSValueArgumentLiteImpl private constructor(
    override val name: KSName?,
    override val value: Any?,
    override val parent: KSNode,
    override val origin: Origin
) : KSValueArgument {
    companion object : KSObjectCache<IdKeyPair<KSName?, Any?>, KSValueArgumentLiteImpl>() {
        fun getCached(name: KSName?, value: Any?, parent: KSNode, origin: Origin) =
            KSValueArgumentLiteImpl.cache
                .getOrPut(IdKeyPair(name, value)) { KSValueArgumentLiteImpl(name, value, parent, origin) }
    }
    override val isSpread: Boolean = false

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val location: Location get() = NonExistLocation

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueArgument(this, data)
    }
}
