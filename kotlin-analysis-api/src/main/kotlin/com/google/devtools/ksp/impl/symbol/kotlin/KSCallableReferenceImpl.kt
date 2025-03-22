package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeArgumentResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf

// TODO: implement a psi based version, rename this class to resolved Impl.
class KSCallableReferenceImpl private constructor(
    private val ktFunctionalType: KaFunctionType,
    override val parent: KSNode?
) : KSCallableReference {
    companion object : KSObjectCache<IdKeyPair<KaType, KSNode?>, KSCallableReference>() {
        fun getCached(ktFunctionalType: KaFunctionType, parent: KSNode?): KSCallableReference =
            cache.getOrPut(IdKeyPair(ktFunctionalType, parent)) { KSCallableReferenceImpl(ktFunctionalType, parent) }
    }
    override val receiverType: KSTypeReference?
        get() = ktFunctionalType.receiverType?.abbreviationOrSelf?.let { KSTypeReferenceResolvedImpl.getCached(it) }

    override val functionParameters: List<KSValueParameter>
        get() = ktFunctionalType.parameterTypes.map {
            KSValueParameterLiteImpl.getCached(it, this@KSCallableReferenceImpl)
        }

    override val returnType: KSTypeReference
        get() = KSTypeReferenceResolvedImpl.getCached(ktFunctionalType.returnType.abbreviationOrSelf)

    override val typeArguments: List<KSTypeArgument>
        get() = ktFunctionalType.typeArguments().map { KSTypeArgumentResolvedImpl.getCached(it, this) }

    override val origin: Origin
        get() = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location
        get() = parent?.location ?: NonExistLocation
}
