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
    override val receiverType: KSTypeReference? by lazy {
        ktFunctionalType.receiverType?.abbreviationOrSelf?.let {
            KSTypeReferenceResolvedImpl.getCached(it, this, -2)
        }
    }

    override val functionParameters: List<KSValueParameter> by lazy {
        ktFunctionalType.parameterTypes.mapIndexed { index, it ->
            KSValueParameterLiteImpl.getCached(it, this@KSCallableReferenceImpl, index)
        }
    }

    override val returnType: KSTypeReference by lazy {
        KSTypeReferenceResolvedImpl.getCached(ktFunctionalType.returnType.abbreviationOrSelf, this, -1)
    }

    override val typeArguments: List<KSTypeArgument> by lazy {
        ktFunctionalType.typeArguments().map { KSTypeArgumentResolvedImpl.getCached(it, this) }
    }

    override val origin: Origin
        get() = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location
        get() = parent?.location ?: NonExistLocation
}
