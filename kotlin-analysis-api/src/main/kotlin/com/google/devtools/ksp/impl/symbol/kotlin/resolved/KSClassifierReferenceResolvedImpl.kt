package com.google.devtools.ksp.impl.symbol.kotlin.resolved

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.IdKeyTriple
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType

class KSClassifierReferenceResolvedImpl private constructor(
    internal val ktType: KtClassType,
    internal val index: Int,
    override val parent: KSTypeReference?
) : KSClassifierReference {
    companion object :
        KSObjectCache<IdKeyTriple<KtClassType, Int, KSTypeReference?>, KSClassifierReferenceResolvedImpl>() {
        fun getCached(ktType: KtClassType, index: Int, parent: KSTypeReference?) =
            cache.getOrPut(IdKeyTriple(ktType, index, parent)) {
                KSClassifierReferenceResolvedImpl(ktType, index, parent)
            }
    }

    private val classifierReference: KtClassTypeQualifier
        get() = ktType.qualifiers[index]

    override val qualifier: KSClassifierReference? by lazy {
        if (index == 0) {
            null
        } else {
            getCached(ktType, index - 1, parent)
        }
    }

    override fun referencedName(): String {
        return classifierReference.name.asString()
    }

    override val typeArguments: List<KSTypeArgument> by lazy {
        classifierReference.typeArguments.map { KSTypeArgumentResolvedImpl.getCached(it, this) }
    }

    override val origin: Origin = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location
        get() = parent?.location ?: NonExistLocation

    override fun toString(): String {
        return referencedName() + if (typeArguments.isNotEmpty()) "<${
        typeArguments.map { it.toString() }.joinToString(", ")
        }>" else ""
    }
}

class KSClassifierParameterImpl private constructor(
    internal val ktType: KtTypeParameterType,
    override val parent: KSTypeReference?
) : KSClassifierReference {
    companion object : KSObjectCache<IdKeyPair<KtTypeParameterType, KSTypeReference?>, KSClassifierParameterImpl>() {
        fun getCached(ktType: KtTypeParameterType, parent: KSTypeReference?) =
            KSClassifierParameterImpl.cache.getOrPut(IdKeyPair(ktType, parent)) {
                KSClassifierParameterImpl(ktType, parent)
            }
    }

    override val qualifier: KSClassifierReference? = null

    override fun referencedName(): String {
        return ktType.name.asString()
    }

    override val typeArguments: List<KSTypeArgument>
        get() = emptyList()
    override val origin: Origin
        get() = parent?.origin ?: Origin.SYNTHETIC
    override val location: Location
        get() = parent?.location ?: NonExistLocation

    override fun toString(): String {
        return referencedName()
    }
}
