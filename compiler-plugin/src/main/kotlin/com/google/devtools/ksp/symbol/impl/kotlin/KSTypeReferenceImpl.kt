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

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSModifiers
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.*

class KSTypeReferenceImpl private constructor(val ktTypeReference: KtTypeReference) : KSTypeReference {
    companion object : KSObjectCache<KtTypeReference, KSTypeReferenceImpl>() {
        fun getCached(ktTypeReference: KtTypeReference) = cache.getOrPut(ktTypeReference) { KSTypeReferenceImpl(ktTypeReference) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktTypeReference.toLocation()
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktTypeReference.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        ktTypeReference.toKSModifiers()
    }

    override val element: KSReferenceElement by lazy {
        var typeElement = ktTypeReference.typeElement
        while (typeElement is KtNullableType)
            typeElement = typeElement.innerType
        when (typeElement) {
            is KtFunctionType -> KSCallableReferenceImpl.getCached(typeElement)
            is KtUserType -> KSClassifierReferenceImpl.getCached(typeElement)
            is KtDynamicType -> KSDynamicReferenceImpl.getCached(Unit)
            else -> throw IllegalStateException("Unexpected type element ${typeElement?.javaClass}, $ExceptionMessage")
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override fun resolve(): KSType = ResolverImpl.instance.resolveUserType(this)

    override fun toString(): String {
        return element.toString()
    }
}