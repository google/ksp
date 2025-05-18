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

package com.google.devtools.ksp.impl.symbol.kotlin.resolved

import com.google.devtools.ksp.common.IdKeyTriple
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.lazyMemoizedSequence
import com.google.devtools.ksp.impl.recordLookup
import com.google.devtools.ksp.impl.symbol.kotlin.Deferrable
import com.google.devtools.ksp.impl.symbol.kotlin.KSClassDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeParameterImpl
import com.google.devtools.ksp.impl.symbol.kotlin.Restorable
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.annotations
import com.google.devtools.ksp.impl.symbol.kotlin.render
import com.google.devtools.ksp.impl.symbol.kotlin.toClassifierReference
import com.google.devtools.ksp.impl.symbol.kotlin.toLocation
import com.google.devtools.ksp.symbol.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtTypeParameter

class KSTypeReferenceResolvedImpl private constructor(
    private val ktType: KaType,
    override val parent: KSNode?,
    private val index: Int,
    private val additionalAnnotations: List<KaAnnotation>
) : KSTypeReference, Deferrable {
    companion object : KSObjectCache<IdKeyTriple<KaType, KSNode?, Int>, KSTypeReference>() {
        fun getCached(
            type: KaType,
            parent: KSNode? = null,
            index: Int = -1,
            additionalAnnotations: List<KaAnnotation> = emptyList()
        ): KSTypeReference =
            cache.getOrPut(IdKeyTriple(type, parent, index)) {
                KSTypeReferenceResolvedImpl(type, parent, index, additionalAnnotations)
            }
    }

    override val element: KSReferenceElement? by lazy {
        // FIXME: synthetic elements can have non-synthetic annotations via use-site targets
        if (parent == null || parent.origin == Origin.SYNTHETIC) {
            null
        } else {
            ktType.toClassifierReference(this)
        }
    }

    override fun resolve(): KSType {
        analyze { recordLookup(ktType, parent) }
        return KSTypeImpl.getCached(ktType)
    }

    override val annotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        ktType.annotations(this) +
            additionalAnnotations.asSequence().map { KSAnnotationResolvedImpl.getCached(it, this) }
    }

    override val origin: Origin = parent?.origin ?: Origin.SYNTHETIC

    override val location: Location by lazy {
        if (index <= -1) {
            parent?.location ?: NonExistLocation
        } else {
            when (parent) {
                is KSClassDeclarationImpl -> {
                    when (val psi = parent.ktClassOrObjectSymbol.psi) {
                        is KtClassOrObject -> psi.superTypeListEntries[index].toLocation()
                        is PsiClass -> (psi as? PsiClassReferenceType)?.reference?.toLocation() ?: NonExistLocation
                        else -> NonExistLocation
                    }
                }
                is KSTypeParameterImpl -> {
                    when (val psi = parent.ktTypeParameterSymbol.psi) {
                        is KtTypeParameter -> parent.location
                        is PsiTypeParameter -> (psi.extendsListTypes[index] as? PsiClassReferenceType)
                            ?.reference?.toLocation() ?: NonExistLocation
                        else -> NonExistLocation
                    }
                }
                else -> NonExistLocation
            }
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override val modifiers: Set<Modifier>
        get() = if (ktType is KaFunctionType && ktType.isSuspend) {
            setOf(Modifier.SUSPEND)
        } else {
            emptySet()
        }

    override fun toString(): String {
        return ktType.abbreviationOrSelf.render()
    }

    override fun defer(): Restorable? {
        TODO("Not yet implemented")
    }
}
