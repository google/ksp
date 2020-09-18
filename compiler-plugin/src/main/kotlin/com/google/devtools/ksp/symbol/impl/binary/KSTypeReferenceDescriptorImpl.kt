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


package com.google.devtools.ksp.symbol.impl.binary

import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.kotlin.getKSTypeCached
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