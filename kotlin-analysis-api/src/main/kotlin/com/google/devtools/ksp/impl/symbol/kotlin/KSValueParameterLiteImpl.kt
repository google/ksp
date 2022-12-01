package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.IdKeyPair
import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KtType

class KSValueParameterLiteImpl private constructor(private val ktType: KtType, override val parent: KSNode) :
    KSValueParameter {
    companion object : KSObjectCache<IdKeyPair<KtType, KSNode>, KSValueParameter>() {
        fun getCached(ktType: KtType, parent: KSNode): KSValueParameter = cache.getOrPut(IdKeyPair(ktType, parent)) {
            KSValueParameterLiteImpl(ktType, parent)
        }
    }

    // preferably maybe use empty name to match compiler, but use underscore to match FE1.0 implementation.
    override val name: KSName = KSNameImpl.getCached("_")

    override val type: KSTypeReference = KSTypeReferenceImpl.getCached(ktType)

    override val isVararg: Boolean = false

    override val isNoInline: Boolean = false

    override val isCrossInline: Boolean = false

    override val isVal: Boolean = false

    override val isVar: Boolean = false

    override val hasDefault: Boolean = false

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val origin: Origin = parent.origin

    override val location: Location = parent.location

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }
}
