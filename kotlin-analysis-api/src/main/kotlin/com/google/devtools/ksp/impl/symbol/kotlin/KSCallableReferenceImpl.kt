package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeArgumentResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtType

// TODO: implement a psi based version, rename this class to resolved Impl.
class KSCallableReferenceImpl private constructor(
    private val ktFunctionalType: KtFunctionalType,
    override val parent: KSNode?
) : KSCallableReference {
    companion object : KSObjectCache<IdKeyPair<KtType, KSNode?>, KSCallableReference>() {
        fun getCached(ktFunctionalType: KtFunctionalType, parent: KSNode?): KSCallableReference =
            cache.getOrPut(IdKeyPair(ktFunctionalType, parent)) { KSCallableReferenceImpl(ktFunctionalType, parent) }
    }
    override val receiverType: KSTypeReference?
        get() = ktFunctionalType.receiverType?.let { KSTypeReferenceResolvedImpl.getCached(it) }

    override val functionParameters: List<KSValueParameter>
        get() = ktFunctionalType.parameterTypes.map {
            KSValueParameterLiteImpl.getCached(it, this@KSCallableReferenceImpl)
        }

    override val returnType: KSTypeReference
        get() = KSTypeReferenceResolvedImpl.getCached(ktFunctionalType.returnType)

    override val typeArguments: List<KSTypeArgument>
        get() = ktFunctionalType.typeArguments().map { KSTypeArgumentResolvedImpl.getCached(it, this) }

    override val origin: Origin
        get() = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location
        get() = parent?.location ?: NonExistLocation
}
