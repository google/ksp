package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KtDefinitelyNotNullType

class KSDefNonNullReferenceImpl private constructor(
    val ktDefinitelyNotNullType: KtDefinitelyNotNullType,
    override val parent: KSTypeReference?
) : KSDefNonNullReference {
    companion object : KSObjectCache<IdKeyPair<KtDefinitelyNotNullType, KSTypeReference?>, KSDefNonNullReference>() {
        fun getCached(ktType: KtDefinitelyNotNullType, parent: KSTypeReference?) =
            KSDefNonNullReferenceImpl.cache
                .getOrPut(IdKeyPair(ktType, parent)) { KSDefNonNullReferenceImpl(ktType, parent) }
    }
    override val enclosedType: KSClassifierReference by lazy {
        ktDefinitelyNotNullType.original.toClassifierReference(parent) as KSClassifierReference
    }
    override val typeArguments: List<KSTypeArgument>
        get() = emptyList()

    override val origin: Origin = Origin.KOTLIN

    override val location: Location
        get() = parent?.location ?: NonExistLocation

    override fun toString() = "${enclosedType.referencedName()} & Any"
}
