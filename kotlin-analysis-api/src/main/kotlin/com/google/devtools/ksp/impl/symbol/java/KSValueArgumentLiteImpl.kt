package com.google.devtools.ksp.impl.symbol.java

import com.google.devtools.ksp.IdKeyPair
import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin

class KSValueArgumentLiteImpl private constructor(
    override val name: KSName?,
    override val value: Any?,
    override val origin: Origin
) : KSValueArgument {
    companion object : KSObjectCache<IdKeyPair<KSName?, Any?>, KSValueArgumentLiteImpl>() {
        fun getCached(name: KSName?, value: Any?, origin: Origin) =
            KSValueArgumentLiteImpl.cache
                .getOrPut(IdKeyPair(name, value)) { KSValueArgumentLiteImpl(name, value, origin) }
    }
    override val isSpread: Boolean = false

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val location: Location
        get() = TODO("Not yet implemented")

    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueArgument(this, data)
    }
}
