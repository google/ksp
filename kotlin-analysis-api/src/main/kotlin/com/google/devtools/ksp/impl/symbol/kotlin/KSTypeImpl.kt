/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.common.IdKey
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.errorTypeOnInconsistentArguments
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.recordLookupWithSupertypes
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSAnnotationResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeArgumentResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.synthetic.getExtensionFunctionTypeAnnotation
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Nullability
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*

class KSTypeImpl private constructor(internal val type: KaType) : KSType {
    companion object : KSObjectCache<IdKey<KaType>, KSTypeImpl>() {
        fun getCached(type: KaType): KSTypeImpl = cache.getOrPut(IdKey(type)) {
            KSTypeImpl(type)
        }
    }

    private fun KaType.toDeclaration(): KSDeclaration {
        return analyze {
            when (this@toDeclaration) {
                is KaClassType -> {
                    when (val symbol = this@toDeclaration.symbol) {
                        is KaTypeAliasSymbol -> KSTypeAliasImpl.getCached(symbol)
                        is KaClassSymbol -> KSClassDeclarationImpl.getCached(symbol)
                    }
                }

                is KaTypeParameterType -> KSTypeParameterImpl.getCached(symbol)
                is KaClassErrorType -> KSErrorTypeClassDeclaration(this@KSTypeImpl)
                is KaFlexibleType ->
                    type.lowerBoundIfFlexible().toDeclaration()

                is KaDefinitelyNotNullType -> this@toDeclaration.original.toDeclaration()
                else -> KSErrorTypeClassDeclaration(this@KSTypeImpl)
            }
        }
    }

    override val declaration: KSDeclaration by lazy {
        (type as? KaFunctionType)?.abbreviatedSymbol()?.toKSDeclaration()
            ?: type.abbreviation?.toDeclaration() ?: type.toDeclaration()
    }

    override val nullability: Nullability by lazy {
        when {
            type is KaFlexibleType && type.lowerBound.nullability != type.upperBound.nullability -> Nullability.PLATFORM
            analyze { type.canBeNull } -> Nullability.NULLABLE
            else -> Nullability.NOT_NULL
        }
    }

    override val arguments: List<KSTypeArgument> by lazy {
        if (ResolverAAImpl.instance.isJavaRawType(this)) {
            emptyList()
        } else {
            if (type is KaFlexibleType) {
                type.upperBound.typeArguments().map { KSTypeArgumentResolvedImpl.getCached(it) }
            } else {
                (type as? KaClassType)?.typeArguments()?.map { KSTypeArgumentResolvedImpl.getCached(it) }
                    ?: emptyList()
            }
        }
    }

    @OptIn(KaImplementationDetail::class)
    override val annotations: Sequence<KSAnnotation>
        get() = type.annotations() +
            if (type is KaFunctionType && type.receiverType != null) {
                sequenceOf(
                    KSAnnotationResolvedImpl.getCached(getExtensionFunctionTypeAnnotation())
                )
            } else {
                emptySequence()
            }

    override fun isAssignableFrom(that: KSType): Boolean {
        if (that.isError || this.isError) {
            return false
        }
        recordLookupWithSupertypes((that as KSTypeImpl).type)
        return type.isAssignableFrom(that.type)
    }

    override fun isMutabilityFlexible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCovarianceFlexible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun replace(arguments: List<KSTypeArgument>): KSType {
        // Do not replace for already error types.
        if (isError) {
            return this
        }
        errorTypeOnInconsistentArguments(
            arguments = arguments,
            placeholdersProvider = { type.typeArguments().map { KSTypeArgumentResolvedImpl.getCached(it) } },
            withCorrectedArguments = ::replace,
            errorType = ::KSErrorType,
        )?.let { error -> return error }
        return getCached(type.replace(arguments.map { it.toKtTypeProjection() }))
    }

    @OptIn(KaImplementationDetail::class)
    override fun starProjection(): KSType {
        return getCached(type.replace(List(type.typeArguments().size) { KaBaseStarTypeProjection(type.token) }))
    }

    override fun makeNullable(): KSType {
        return analyze {
            getCached(type.withNullability(KaTypeNullability.NULLABLE))
        }
    }

    override fun makeNotNullable(): KSType {
        return analyze {
            getCached(type.withNullability(KaTypeNullability.NON_NULLABLE))
        }
    }

    override val isMarkedNullable: Boolean
        get() = type.nullability == KaTypeNullability.NULLABLE

    override val isError: Boolean
        // TODO: non exist type returns KtNonErrorClassType, check upstream for KtClassErrorType usage.
        get() = type is KaErrorType || type.classifierSymbol() == null

    override val isFunctionType: Boolean
        get() = type is KaFunctionType && !type.isSuspend

    override val isSuspendFunctionType: Boolean
        get() = type is KaFunctionType && type.isSuspend

    override fun hashCode(): Int {
        return type.fullyExpand().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KSTypeImpl) {
            return false
        }
        return analyze {
            type.semanticallyEquals(other.type)
        }
    }

    override fun toString(): String {
        return type.render()
    }
}

internal fun KaType.fullyExpand(): KaType = analyze { fullyExpandedType }
