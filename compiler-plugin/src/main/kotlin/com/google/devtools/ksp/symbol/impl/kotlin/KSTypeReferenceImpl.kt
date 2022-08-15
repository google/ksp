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
import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.memoized
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.toLocation
import com.google.devtools.ksp.toKSModifiers
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDynamicType
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

class KSTypeReferenceImpl private constructor(val ktTypeReference: KtTypeReference) : KSTypeReference {
    companion object : KSObjectCache<KtTypeReference, KSTypeReferenceImpl>() {
        fun getCached(ktTypeReference: KtTypeReference) = cache.getOrPut(ktTypeReference) {
            KSTypeReferenceImpl(ktTypeReference)
        }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktTypeReference.toLocation()
    }
    override val parent: KSNode? by lazy {
        var parentPsi = ktTypeReference.parent
        while (
            parentPsi != null && parentPsi !is KtAnnotationEntry && parentPsi !is KtFunctionType &&
            parentPsi !is KtClassOrObject && parentPsi !is KtFunction && parentPsi !is KtUserType &&
            parentPsi !is KtProperty && parentPsi !is KtTypeAlias && parentPsi !is KtTypeProjection &&
            parentPsi !is KtTypeParameter && parentPsi !is KtParameter
        ) {
            parentPsi = parentPsi.parent
        }
        when (parentPsi) {
            is KtAnnotationEntry -> KSAnnotationImpl.getCached(parentPsi)
            is KtFunctionType -> KSCallableReferenceImpl.getCached(parentPsi)
            is KtClassOrObject -> KSClassDeclarationImpl.getCached(parentPsi)
            is KtFunction -> KSFunctionDeclarationImpl.getCached(parentPsi)
            is KtUserType -> KSClassifierReferenceImpl.getCached(parentPsi)
            is KtProperty -> KSPropertyDeclarationImpl.getCached(parentPsi)
            is KtTypeAlias -> KSTypeAliasImpl.getCached(parentPsi)
            is KtTypeProjection -> KSTypeArgumentKtImpl.getCached(parentPsi)
            is KtTypeParameter -> KSTypeParameterImpl.getCached(parentPsi)
            is KtParameter -> KSValueParameterImpl.getCached(parentPsi)
            else -> null
        }
    }

    // Parenthesized type in grammar seems to be implemented as KtNullableType.
    private fun visitNullableType(visit: (KtNullableType) -> Unit) {
        var typeElement = ktTypeReference.typeElement
        while (typeElement is KtNullableType) {
            visit(typeElement)
            typeElement = typeElement.innerType
        }
    }

    // Annotations and modifiers are only allowed in one of the parenthesized type.
    // https://github.com/JetBrains/kotlin/blob/50e12239ef8141a45c4dca2bf0544be6191ecfb6/compiler/frontend/src/org/jetbrains/kotlin/diagnostics/rendering/DefaultErrorMessages.java#L608
    override val annotations: Sequence<KSAnnotation> by lazy {
        fun List<KtAnnotationEntry>.toKSAnnotations(): Sequence<KSAnnotation> =
            asSequence().map {
                KSAnnotationImpl.getCached(it)
            }

        val innerAnnotations = mutableListOf<Sequence<KSAnnotation>>()
        visitNullableType {
            innerAnnotations.add(it.annotationEntries.toKSAnnotations())
        }

        (ktTypeReference.annotationEntries.toKSAnnotations() + innerAnnotations.asSequence().flatten()).memoized()
    }

    override val modifiers: Set<Modifier> by lazy {
        val innerModifiers = mutableSetOf<Modifier>()
        visitNullableType {
            innerModifiers.addAll(it.modifierList.toKSModifiers())
        }
        ktTypeReference.toKSModifiers() + innerModifiers
    }

    override val element: KSReferenceElement by lazy {
        var typeElement = ktTypeReference.typeElement
        while (typeElement is KtNullableType)
            typeElement = typeElement.innerType
        when (typeElement) {
            is KtFunctionType -> KSCallableReferenceImpl.getCached(typeElement)
            is KtUserType -> KSClassifierReferenceImpl.getCached(typeElement)
            is KtDynamicType -> KSDynamicReferenceImpl.getCached(this)
            else -> throw IllegalStateException("Unexpected type element ${typeElement?.javaClass}, $ExceptionMessage")
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override fun resolve(): KSType = ResolverImpl.instance!!.resolveUserType(this)

    override fun toString(): String {
        return element.toString()
    }
}
