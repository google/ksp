/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin

import com.google.devtools.kotlin.symbol.processing.processing.impl.ResolverImpl
import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import com.google.devtools.kotlin.symbol.processing.symbol.impl.binary.KSTypeArgumentDescriptorImpl
import com.google.devtools.kotlin.symbol.processing.symbol.impl.replaceTypeArguments
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.*

class KSTypeImpl private constructor(
    val kotlinType: KotlinType,
    private val ksTypeArguments: List<KSTypeArgument>? = null,
    override val annotations: List<KSAnnotation> = listOf()
) : KSType {
    companion object : KSObjectCache<IdKey<KotlinType>, KSTypeImpl>() {
        fun getCached(
            kotlinType: KotlinType,
            ksTypeArguments: List<KSTypeArgument>? = null,
            annotations: List<KSAnnotation> = listOf()
        ): KSTypeImpl {
            return cache.getOrPut(IdKey(kotlinType)) { KSTypeImpl(kotlinType, ksTypeArguments, annotations) }
        }
    }

    override val declaration: KSDeclaration by lazy {
        ResolverImpl.instance.findDeclaration(kotlinType.getAbbreviation() ?: kotlinType)
    }

    override val nullability: Nullability by lazy {
        when (kotlinType.nullability()) {
            TypeNullability.NULLABLE -> Nullability.NULLABLE
            TypeNullability.NOT_NULL -> Nullability.NOT_NULL
            TypeNullability.FLEXIBLE -> Nullability.PLATFORM
        }
    }

    /**
     * Even though that [KSTypeArgumentDescriptorImpl] is no heavier than [ksTypeArguments], the former doesn't carry [KSAnnotation].
     */
    override val arguments: List<KSTypeArgument> by lazy {
        ksTypeArguments ?: kotlinType.arguments.map { KSTypeArgumentDescriptorImpl.getCached(it) }
    }

    override fun isAssignableFrom(that: KSType): Boolean = (that as? KSTypeImpl)?.kotlinType?.isSubtypeOf(kotlinType) == true

    // TODO: find a better way to reuse the logic in [DescriptorRendererImpl.renderFlexibleType].
    override fun isMutabilityFlexible(): Boolean {
        return kotlinType.toString().startsWith("(Mutable)")
    }

    // TODO: find a better way to reuse the logic in [DescriptorRendererImpl.renderFlexibleType].
    override fun isCovarianceFlexible(): Boolean {
        return kotlinType.toString().startsWith("Array<(out) ")
    }

    override fun replace(arguments: List<KSTypeArgument>): KSType {
        return KSTypeImpl.getCached(kotlinType.replaceTypeArguments(arguments), arguments)
    }

    override fun starProjection(): KSType {
        return KSTypeImpl.getCached(kotlinType.replaceArgumentsWithStarProjections())
    }

    private val meNullable: KSType by lazy { KSTypeImpl.getCached(kotlinType.makeNullable()) }
    override fun makeNullable(): KSType = meNullable

    private val meNotNullable: KSType by lazy { KSTypeImpl.getCached(kotlinType.makeNotNullable()) }
    override fun makeNotNullable(): KSType = meNotNullable

    override val isError: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (other !is KSTypeImpl)
            return false
        return kotlinType.equals(other.kotlinType)
    }

    override fun hashCode(): Int = kotlinType.hashCode()

    override fun toString(): String = (kotlinType.getAbbreviation() ?: kotlinType).toString()
}

class IdKey<T>(private val k: T) {
    override fun equals(other: Any?): Boolean = if (other is IdKey<*>) k === other.k else false
    override fun hashCode(): Int = k.hashCode()
}

fun getKSTypeCached(
    kotlinType: KotlinType,
    ksTypeArguments: List<KSTypeArgument>? = null,
    annotations: List<KSAnnotation> = listOf()
): KSType {
    return if (kotlinType.isError) {
        KSErrorType
    } else {
        KSTypeImpl.getCached(kotlinType, ksTypeArguments, annotations)
    }
}
