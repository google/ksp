/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.impl.recordLookup
import com.google.devtools.ksp.impl.symbol.util.toKSModifiers
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDynamicType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

class KSTypeReferenceImpl(
    private val ktTypeReference: KtTypeReference,
    override val parent: KSNode?,
    private val additionalAnnotations: List<KaAnnotation>
) : KSTypeReference {
    companion object : KSObjectCache<IdKeyPair<KtTypeReference, KSNode?>, KSTypeReference>() {
        fun getCached(
            ktTypeReference: KtTypeReference,
            parent: KSNode? = null,
            additionalAnnotations: List<KaAnnotation> = emptyList()
        ): KSTypeReference {
            return cache.getOrPut(IdKeyPair(ktTypeReference, parent)) {
                KSTypeReferenceImpl(ktTypeReference, parent, additionalAnnotations)
            }
        }
    }

    // Remember to recordLookup if the usage is beyond a type reference.
    private val ktType: KtType by lazy {
        analyze {
            ktTypeReference.getKtType().let { it.abbreviatedType ?: it }
        }
    }
    override val element: KSReferenceElement? by lazy {
        var typeElement = ktTypeReference.typeElement
        while (typeElement is KtNullableType)
            typeElement = typeElement.innerType
        when (typeElement) {
            is KtUserType -> KSClassifierReferenceImpl.getCached(typeElement, this)
            is KtDynamicType -> KSDynamicReferenceImpl.getCached(this)
            else -> (resolve() as KSTypeImpl).type.toClassifierReference(this)
        }
    }

    override fun resolve(): KSType {
        analyze { recordLookup(ktType, parent) }
        return KSTypeImpl.getCached(ktType)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        val innerAnnotations = mutableListOf<Sequence<KtAnnotationEntry>>()
        visitNullableType {
            innerAnnotations.add(it.annotationEntries.asSequence())
        }

        (ktTypeReference.annotationEntries.asSequence() + innerAnnotations.asSequence().flatten())
            .map { annotationEntry ->
                KSAnnotationImpl.getCached(annotationEntry, this@KSTypeReferenceImpl) {
                    (ktType.annotations + additionalAnnotations).single {
                        it.psi == annotationEntry
                    }
                }
            }
    }

    override val origin: Origin = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location by lazy {
        ktTypeReference.toLocation()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override val modifiers: Set<Modifier> by lazy {
        val innerModifiers = mutableSetOf<Modifier>()
        visitNullableType {
            innerModifiers.addAll(it.modifierList.toKSModifiers())
        }
        innerModifiers + ktTypeReference.toKSModifiers()
    }

    override fun toString(): String {
        return element.toString()
    }

    private fun visitNullableType(visit: (KtNullableType) -> Unit) {
        var typeElement = ktTypeReference.typeElement
        while (typeElement is KtNullableType) {
            visit(typeElement)
            typeElement = typeElement.innerType
        }
    }
}
