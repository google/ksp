package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.IdKeyTriple
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KaType

class KSValueParameterLiteImpl private constructor(ktType: KaType, override val parent: KSNode) :
    KSValueParameter, Deferrable {
    companion object : KSObjectCache<IdKeyTriple<KaType, KSNode, Int>, KSValueParameter>() {
        fun getCached(ktType: KaType, parent: KSNode, index: Int = -1): KSValueParameter =
            cache.getOrPut(IdKeyTriple(ktType, parent, index)) {
                KSValueParameterLiteImpl(ktType, parent)
            }
    }

    // preferably maybe use empty name to match compiler, but use underscore to match FE1.0 implementation.
    override val name: KSName = KSNameImpl.getCached("_")

    override val type: KSTypeReference = KSTypeReferenceResolvedImpl.getCached(ktType, this)

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

    override fun defer(): Restorable? {
        val callable = parent as? KSCallableReference ?: return null
        val typeRef = callable.parent as? KSTypeReference ?: return null
        val typeRefRestorable = (typeRef as? Deferrable)?.defer() ?: return null
        val paramIndex = callable.functionParameters.indexOfFirst { it === this }
        if (paramIndex < 0) return null
        return Restorable {
            val restoredTypeRef = typeRefRestorable.restore() as? KSTypeReference ?: return@Restorable null
            (restoredTypeRef.element as? KSCallableReference)?.functionParameters?.getOrNull(paramIndex)
        }
    }
}
