/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSTypeImpl
import org.jetbrains.kotlin.ksp.symbol.Modifier
import org.jetbrains.kotlin.types.*

class KSTypeReferenceDescriptorImpl(val kotlinType: KotlinType) : KSTypeReference {
    companion object {
        private val cache = mutableMapOf<KotlinType, KSTypeReferenceDescriptorImpl>()

        fun getCached(kotlinType: KotlinType) = cache.getOrPut(kotlinType) { KSTypeReferenceDescriptorImpl(kotlinType) }
    }

    override val origin = Origin.CLASS

    override val element: KSReferenceElement by lazy {
        when {
            kotlinType.constructor.declarationDescriptor is ClassDescriptor -> KSClassifierReferenceDescriptorImpl.getCached(kotlinType)
            kotlinType.constructor.declarationDescriptor is TypeParameterDescriptor -> {
                val upperBound = TypeUtils.getTypeParameterDescriptorOrNull(kotlinType)!!.upperBounds.first()
                when (upperBound) {
                    is FlexibleType -> KSClassifierReferenceDescriptorImpl.getCached(upperBound.upperBound)
                    is SimpleType -> KSClassifierReferenceDescriptorImpl.getCached(upperBound)
                    else -> throw IllegalStateException()
                }
            }
            else -> throw IllegalStateException()
        }
    }

    override val annotations: List<KSAnnotation> by lazy {
        kotlinType.annotations.map { KSAnnotationDescriptorImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        if (kotlinType.isSuspendFunctionTypeOrSubtype) {
            setOf(Modifier.SUSPEND)
        } else {
            emptySet<Modifier>()
        }
    }

    override fun resolve(): KSType {
        return KSTypeImpl.getCached(kotlinType)
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }
}