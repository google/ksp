/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.binary

import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.Modifier
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin.getKSTypeCached
import org.jetbrains.kotlin.types.*

class KSTypeReferenceDescriptorImpl private constructor(val kotlinType: KotlinType) : KSTypeReference {
    companion object : KSObjectCache<KotlinType, KSTypeReferenceDescriptorImpl>() {
        fun getCached(kotlinType: KotlinType) = cache.getOrPut(kotlinType) { KSTypeReferenceDescriptorImpl(kotlinType) }
    }

    override val origin = Origin.CLASS

    override val location: Location = NonExistLocation

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
        return getKSTypeCached(kotlinType)
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override fun toString(): String {
        return element.toString()
    }
}