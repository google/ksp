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

import com.google.devtools.ksp.IdKeyPair
import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.recordLookupWithSupertypes
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeArgumentResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.synthetic.getExtensionFunctionTypeAnnotation
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Nullability
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*

class KSTypeImpl private constructor(internal val type: KtType) : KSType {
    companion object : KSObjectCache<IdKeyPair<KtType, KtAnnotationsList>, KSTypeImpl>() {
        fun getCached(type: KtType): KSTypeImpl = cache.getOrPut(IdKeyPair(type, type.annotationsList)) {
            KSTypeImpl(type)
        }
    }

    private fun KtType.toDeclaration(): KSDeclaration {
        return analyze {
            when (this@toDeclaration) {
                is KtNonErrorClassType -> {
                    when (val symbol = this@toDeclaration.classSymbol) {
                        is KtTypeAliasSymbol -> KSTypeAliasImpl.getCached(symbol)
                        is KtClassOrObjectSymbol -> KSClassDeclarationImpl.getCached(symbol)
                    }
                }
                is KtTypeParameterType -> KSTypeParameterImpl.getCached(symbol)
                is KtClassErrorType -> KSErrorTypeClassDeclaration
                is KtFlexibleType ->
                    type.lowerBoundIfFlexible().toDeclaration()
                is KtDefinitelyNotNullType -> this@toDeclaration.original.toDeclaration()
                else -> KSErrorTypeClassDeclaration
            }
        }
    }

    override val declaration: KSDeclaration by lazy {
        type.toDeclaration()
    }

    override val nullability: Nullability by lazy {
        when {
            type is KtFlexibleType && type.lowerBound.nullability != type.upperBound.nullability -> Nullability.PLATFORM
            analyze { type.canBeNull } -> Nullability.NULLABLE
            else -> Nullability.NOT_NULL
        }
    }

    override val arguments: List<KSTypeArgument> by lazy {
        if (ResolverAAImpl.instance.isJavaRawType(this)) {
            emptyList()
        } else {
            if (type is KtFlexibleType) {
                type.upperBound.typeArguments().map { KSTypeArgumentResolvedImpl.getCached(it) }
            } else {
                (type as? KtNonErrorClassType)?.typeArguments()?.map { KSTypeArgumentResolvedImpl.getCached(it) }
                    ?: emptyList()
            }
        }
    }

    override val annotations: Sequence<KSAnnotation>
        get() = type.annotations() +
            if (type is KtFunctionalType && type.receiverType != null) {
                sequenceOf(KSAnnotationImpl.getCached(getExtensionFunctionTypeAnnotation(type.annotations.size)))
            } else {
                emptySequence<KSAnnotation>()
            }

    override fun isAssignableFrom(that: KSType): Boolean {
        if (that.isError || this.isError) {
            return false
        }
        recordLookupWithSupertypes((that as KSTypeImpl).type)
        return type.isAssignableFrom((that as KSTypeImpl).type)
    }

    override fun isMutabilityFlexible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCovarianceFlexible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun replace(arguments: List<KSTypeArgument>): KSType {
        return analyze {
            analysisSession.buildClassType((type as KtNonErrorClassType).classSymbol) {
                arguments.forEach { arg -> argument(arg.toKtTypeProjection()) }
            }.let { getCached(it) }
        }
    }

    override fun starProjection(): KSType {
        return analyze {
            analysisSession.buildClassType((type as KtNonErrorClassType).classSymbol) {
                type.typeArguments().forEach {
                    argument(KtStarTypeProjection(type.token))
                }
            }.let { getCached(it) }
        }
    }

    override fun makeNullable(): KSType {
        return analyze {
            getCached(type.withNullability(KtTypeNullability.NULLABLE))
        }
    }

    override fun makeNotNullable(): KSType {
        return analyze {
            getCached(type.withNullability(KtTypeNullability.NON_NULLABLE))
        }
    }

    override val isMarkedNullable: Boolean
        get() = type.nullability == KtTypeNullability.NULLABLE

    override val isError: Boolean
        get() = type is KtClassErrorType

    override val isFunctionType: Boolean
        get() = type is KtFunctionalType && !type.isSuspend

    override val isSuspendFunctionType: Boolean
        get() = type is KtFunctionalType && type.isSuspend

    override fun hashCode(): Int {
        return type.toAbbreviatedType().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KSTypeImpl) {
            return false
        }
        return analyze {
            type.isEqualTo(other.type)
        }
    }

    override fun toString(): String {
        return type.render()
    }
}

internal fun KtType.toAbbreviatedType(): KtType {
    val symbol = this.classifierSymbol()
    return when (symbol) {
        is KtTypeAliasSymbol -> symbol.expandedType.toAbbreviatedType()
        else -> this
    }
}
