package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.IdKeyPair
import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.types.KtUsualClassType

class KSClassifierReferenceImpl private constructor(
    internal val ktType: KtUsualClassType,
    override val parent: KSTypeReference?
) : KSClassifierReference {
    companion object : KSObjectCache<IdKeyPair<KtUsualClassType, KSTypeReference?>, KSClassifierReferenceImpl>() {
        fun getCached(ktType: KtUsualClassType, parent: KSTypeReference?) =
            cache.getOrPut(IdKeyPair(ktType, parent)) { KSClassifierReferenceImpl(ktType, parent) }
    }
    override val qualifier: KSClassifierReference? by lazy {
        ktType.classId.outerClassId?.let {
            analyze {
                buildClassType(it)
            }
        }?.let { getCached(it as KtUsualClassType, parent) }
    }

    override fun referencedName(): String {
        return ktType.classId.asFqNameString()
    }

    override val typeArguments: List<KSTypeArgument> by lazy {
        ktType.typeArguments.map { KSTypeArgumentImpl.getCached(it, this) }
    }

    override val origin: Origin = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location
        get() = parent?.location ?: NonExistLocation
}
