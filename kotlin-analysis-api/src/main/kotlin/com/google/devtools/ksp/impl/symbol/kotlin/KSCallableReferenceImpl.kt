package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.IdKeyPair
import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtType

class KSCallableReferenceImpl private constructor(
    private val ktFunctionalType: KtFunctionalType,
    override val parent: KSNode
) : KSCallableReference {
    companion object : KSObjectCache<IdKeyPair<KtType, KSNode>, KSCallableReference>() {
        fun getCached(ktFunctionalType: KtFunctionalType, parent: KSNode): KSCallableReference =
            cache.getOrPut(IdKeyPair(ktFunctionalType, parent)) { KSCallableReferenceImpl(ktFunctionalType, parent) }
    }
    override val receiverType: KSTypeReference?
        get() = ktFunctionalType.receiverType?.let { KSTypeReferenceImpl.getCached(it) }

    override val functionParameters: List<KSValueParameter>
        get() = ktFunctionalType.parameterTypes.map {
            KSValueParameterLiteImpl.getCached(it, this@KSCallableReferenceImpl)
        }

    override val returnType: KSTypeReference
        get() = KSTypeReferenceImpl.getCached(ktFunctionalType.returnType)

    override val typeArguments: List<KSTypeArgument>
        get() = ktFunctionalType.typeArguments.map { KSTypeArgumentImpl.getCached(it, this) }

    override val origin: Origin
        get() = parent.origin

    override val location: Location
        get() = parent.location
}
