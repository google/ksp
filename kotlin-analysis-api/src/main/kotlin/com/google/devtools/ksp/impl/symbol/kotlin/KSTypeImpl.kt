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
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class KSTypeImpl private constructor(internal val type: KaType) : KSType {
    companion object : KSObjectCache<IdKey<KaType>, KSTypeImpl>() {
        fun getCached(type: KaType): KSTypeImpl = cache.getOrPut(IdKey(type)) {
            KSTypeImpl(type)
        }
    }

    /**
     * Workaround for an issue where Kotlin Analysis API fails to resolve nested type aliases.
     *
     * [KaSession.findTypeAlias] fails to resolve type aliases nested inside classes
     * (e.g., `OuterClass.NestedAlias`) when provided with the corresponding [ClassId], returning `null`.
     * Consequently, references to such aliases result in a [KaClassErrorType].
     *
     * This workaround attempts to recover the correct symbol by:
     * 1.  Iterating through the segments of the qualified name to find the deepest resolvable container class.
     * 2.  Once the container class is found (e.g., `OuterClass`), manually inspecting its member scopes
     *     (both declared and static) to find the nested classifier (e.g., `NestedAlias`).
     * 3.  If a [KaTypeAliasSymbol] is found, it is returned as a valid [KSDeclaration].
     */
    private fun KaSession.tryResolveNestedTypeAlias(type: KaClassErrorType): KSDeclaration? {
        val qualifierNames = type.qualifiers.map { it.name.asString() }
        for (i in 0..qualifierNames.size) {
            val packageName = qualifierNames.subList(0, i).joinToString(".")
            val className = qualifierNames.subList(i, qualifierNames.size).joinToString(".")
            if (className.isEmpty()) continue

            val packageFqName = FqName(packageName)
            val classFqName = FqName(className)
            val classId = ClassId(packageFqName, classFqName, false)

            val alias = findTypeAlias(classId)
            if (alias != null) return KSTypeAliasImpl.getCached(alias)

            val clazz = findClass(classId)
            if (clazz != null) return KSClassDeclarationImpl.getCached(clazz)

            // Try to resolve container class and find nested type alias manually
            val segments = classFqName.pathSegments()
            for (j in 1 until segments.size) {
                val containerName = segments.subList(0, j).joinToString(".")
                val nestedName = segments.subList(j, segments.size).joinToString(".")
                val containerClassId = ClassId(packageFqName, FqName(containerName), false)
                val containerClass = findClass(containerClassId) ?: continue

                // Check simple nested name
                if (segments.size - j == 1) {
                    val nestedId = Name.identifier(nestedName)
                    val found = containerClass.declaredMemberScope.classifiers { it == nestedId }.firstOrNull()
                        ?: containerClass.staticDeclaredMemberScope.classifiers { it == nestedId }.firstOrNull()

                    if (found is KaTypeAliasSymbol) {
                        return KSTypeAliasImpl.getCached(found)
                    }
                }
            }
        }
        return null
    }

    private fun KaType.toDeclaration(): KSDeclaration {
        return analyze {
            when (this@toDeclaration) {
                is KaClassErrorType -> tryResolveNestedTypeAlias(this@toDeclaration)
                    ?: KSErrorTypeClassDeclaration(this@KSTypeImpl)

                is KaClassType -> {
                    when (val symbol = this@toDeclaration.symbol) {
                        is KaTypeAliasSymbol -> KSTypeAliasImpl.getCached(symbol)
                        is KaClassSymbol -> KSClassDeclarationImpl.getCached(symbol)
                    }
                }

                is KaTypeParameterType -> KSTypeParameterImpl.getCached(symbol)

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
            type is KaFlexibleType && analyze {
                !type.lowerBound.isMarkedNullable && type.upperBound.isMarkedNullable
            } -> Nullability.PLATFORM
            analyze { type.isNullable } -> Nullability.NULLABLE
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
            getCached(type.withNullability(isMarkedNullable = true))
        }
    }

    override fun makeNotNullable(): KSType {
        return analyze {
            getCached(type.withNullability(isMarkedNullable = false))
        }
    }

    override val isMarkedNullable: Boolean = analyze { type.isMarkedNullable }

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
