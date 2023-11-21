/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.IdKey
import com.google.devtools.ksp.processing.impl.KSObjectCache
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.binary.KSAnnotationDescriptorImpl
import com.google.devtools.ksp.symbol.impl.binary.KSTypeArgumentDescriptorImpl
import com.google.devtools.ksp.symbol.impl.convertKotlinType
import com.google.devtools.ksp.symbol.impl.replaceTypeArguments
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isKFunctionType
import org.jetbrains.kotlin.builtins.isKSuspendFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.types.typeUtil.nullability
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections

class KSTypeImpl private constructor(
    val kotlinType: KotlinType,
    private val ksTypeArguments: List<KSTypeArgument>? = null,
    override val annotations: Sequence<KSAnnotation> = sequenceOf()
) : KSType {
    companion object : KSObjectCache<IdKey<KotlinType>, KSTypeImpl>() {
        fun getCached(
            kotlinType: KotlinType,
            ksTypeArguments: List<KSTypeArgument>? = null,
            annotations: Sequence<KSAnnotation> = sequenceOf()
        ): KSTypeImpl {
            return cache.getOrPut(IdKey(kotlinType)) { KSTypeImpl(kotlinType, ksTypeArguments, annotations) }
        }
    }

    override val declaration: KSDeclaration by lazy {
        ResolverImpl.instance!!.findDeclaration(kotlinType.getAbbreviation() ?: kotlinType)
    }

    override val nullability: Nullability by lazy {
        when (kotlinType.nullability()) {
            TypeNullability.NULLABLE -> Nullability.NULLABLE
            TypeNullability.NOT_NULL -> Nullability.NOT_NULL
            TypeNullability.FLEXIBLE -> Nullability.PLATFORM
        }
    }

    // TODO: fix calls to getKSTypeCached and use ksTypeArguments when available.
    override val arguments: List<KSTypeArgument> by lazy {
        (kotlinType.getAbbreviation() ?: kotlinType).arguments.map {
            KSTypeArgumentDescriptorImpl.getCached(it, Origin.SYNTHETIC, null)
        }
    }

    override fun isAssignableFrom(that: KSType): Boolean {
        val subType = (that as? KSTypeImpl)?.kotlinType?.convertKotlinType() ?: return false
        ResolverImpl.instance!!.incrementalContext.recordLookupWithSupertypes(subType)
        val thisType = (this as? KSTypeImpl)?.kotlinType?.convertKotlinType() ?: return false
        return subType.isSubtypeOf(thisType)
    }

    // TODO: find a better way to reuse the logic in [DescriptorRendererImpl.renderFlexibleType].
    override fun isMutabilityFlexible(): Boolean {
        return kotlinType.toString().startsWith("(Mutable)")
    }

    // TODO: find a better way to reuse the logic in [DescriptorRendererImpl.renderFlexibleType].
    override fun isCovarianceFlexible(): Boolean {
        return kotlinType.toString().startsWith("Array<(out) ")
    }

    override fun replace(arguments: List<KSTypeArgument>): KSType {
        return kotlinType.replaceTypeArguments(arguments)?.let {
            getKSTypeCached(it, arguments, annotations)
        } ?: KSErrorType
    }

    override fun starProjection(): KSType {
        return getKSTypeCached(kotlinType.replaceArgumentsWithStarProjections(), annotations = annotations)
    }

    private val meNullable: KSType by lazy { getKSTypeCached(kotlinType.makeNullable()) }
    override fun makeNullable(): KSType = meNullable

    private val meNotNullable: KSType by lazy { getKSTypeCached(kotlinType.makeNotNullable()) }
    override fun makeNotNullable(): KSType = meNotNullable

    override val isMarkedNullable: Boolean = kotlinType.isMarkedNullable

    override val isError: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (other !is KSTypeImpl)
            return false
        return kotlinType.equals(other.kotlinType)
    }

    override fun hashCode(): Int = kotlinType.hashCode()

    override fun toString(): String = (kotlinType.getAbbreviation() ?: kotlinType).toString()

    override val isFunctionType: Boolean
        get() = kotlinType.isFunctionType || kotlinType.isKFunctionType

    override val isSuspendFunctionType: Boolean
        get() = kotlinType.isSuspendFunctionType || kotlinType.isKSuspendFunctionType
}

fun getKSTypeCached(
    kotlinType: KotlinType,
    ksTypeArguments: List<KSTypeArgument>? = null,
    annotations: Sequence<KSAnnotation> = sequenceOf()
): KSType {
    return if (kotlinType.isError ||
        kotlinType.constructor.declarationDescriptor is NotFoundClasses.MockClassDescriptor
    ) {
        KSErrorType
    } else {
        KSTypeImpl.getCached(
            kotlinType,
            ksTypeArguments,
            annotations + kotlinType.annotations
                .filter { it.source == SourceElement.NO_SOURCE }
                .map { KSAnnotationDescriptorImpl.getCached(it, null) }
                .asSequence()
        )
    }
}
