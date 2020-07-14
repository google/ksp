/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSTypeArgumentDescriptorImpl
import org.jetbrains.kotlin.ksp.symbol.impl.replaceTypeArguments
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.*

class KSTypeImpl private constructor(
    val kotlinType: KotlinType,
    private val ksTypeArguments: List<KSTypeArgument>? = null,
    override val annotations: List<KSAnnotation> = listOf()
) : KSType {
    companion object : KSObjectCache<KotlinType, KSTypeImpl>() {
        fun getCached(
            kotlinType: KotlinType,
            ksTypeArguments: List<KSTypeArgument>? = null,
            annotations: List<KSAnnotation> = listOf()
        ) =
            cache.getOrPut(kotlinType) { KSTypeImpl(kotlinType, ksTypeArguments, annotations) }
    }

    override val declaration: KSDeclaration = ResolverImpl.instance.findDeclaration(kotlinType)

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

    override fun isAssignableFrom(that: KSType): Boolean = (that as KSTypeImpl).kotlinType.isSubtypeOf(kotlinType)

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

    override fun equals(other: Any?): Boolean {
        if (other !is KSTypeImpl)
            return false
        return kotlinType.equals(other.kotlinType)
    }

    override fun hashCode(): Int = kotlinType.hashCode()

    override fun toString(): String = kotlinType.toString()
}