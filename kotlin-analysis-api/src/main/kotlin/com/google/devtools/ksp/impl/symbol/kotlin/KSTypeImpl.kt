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

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Nullability
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KSTypeImpl private constructor(internal val type: KtType) : KSType {
    companion object : KSObjectCache<KtType, KSTypeImpl>() {
        fun getCached(type: KtType): KSTypeImpl = cache.getOrPut(type) { KSTypeImpl(type) }
    }

    override val declaration: KSDeclaration by lazy {
        analyze {
            when (type) {
                is KtNonErrorClassType ->
                    type.classId.getCorrespondingToplevelClassOrObjectSymbol().safeAs<KtNamedClassOrObjectSymbol>()
                        ?.let { KSClassDeclarationImpl.getCached(it) } ?: KSErrorTypeClassDeclaration
                is KtClassErrorType -> KSErrorTypeClassDeclaration
                else -> throw IllegalStateException("Unexpected KtType: $type")
            }
        }
    }

    override val nullability: Nullability by lazy {
        if (type.nullability == KtTypeNullability.NON_NULLABLE) {
            Nullability.NOT_NULL
        } else {
            Nullability.NULLABLE
        }
    }

    override val arguments: List<KSTypeArgument> by lazy {
        type.safeAs<KtNonErrorClassType>()?.typeArguments?.map { KSTypeArgumentImpl.getCached(it) } ?: emptyList()
    }

    override val annotations: Sequence<KSAnnotation>
        get() = TODO("Not yet implemented")

    override fun isAssignableFrom(that: KSType): Boolean {
        if (that.isError || this.isError) {
            return false
        }
        return analyze {
            that.safeAs<KSTypeImpl>()?.type?.isSubTypeOf(type) == true
        }
    }

    override fun isMutabilityFlexible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCovarianceFlexible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun replace(arguments: List<KSTypeArgument>): KSType {
        TODO("Not yet implemented")
    }

    override fun starProjection(): KSType {
        TODO("Not yet implemented")
    }

    override fun makeNullable(): KSType {
        TODO("Not yet implemented")
    }

    override fun makeNotNullable(): KSType {
        TODO("Not yet implemented")
    }

    override val isMarkedNullable: Boolean
        get() = type.nullability == KtTypeNullability.NULLABLE

    override val isError: Boolean
        get() = type is KtClassErrorType

    override val isFunctionType: Boolean
        get() = type is KtFunctionalType

    override val isSuspendFunctionType: Boolean
        get() = type is KtFunctionalType && type.isSuspend

    override fun toString(): String {
        return type.toString()
    }
}
