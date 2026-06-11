/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.common.lazyMemoizedSequence
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSAnnotationResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSBackingField
import com.google.devtools.ksp.symbol.KSExpectActual
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.FIELD as KaFieldTarget
import org.jetbrains.kotlin.psi.KtBackingField
import kotlin.sequences.map

class KSBackingFieldImpl private constructor(val kaBackingFieldSymbol: KaBackingFieldSymbol) :
    KSBackingField,
    AbstractKSDeclarationImpl(),
    KSExpectActual by KSExpectActualImpl(kaBackingFieldSymbol) {
    override val ktDeclarationSymbol: KaDeclarationSymbol get() = kaBackingFieldSymbol

    companion object : KSObjectCache<KaBackingFieldSymbol, KSBackingFieldImpl>() {
        fun getCached(kaBackingFieldSymbol: KaBackingFieldSymbol) =
            cache.getOrPut(kaBackingFieldSymbol) { KSBackingFieldImpl(kaBackingFieldSymbol) }

        @JvmStatic
        private fun getFieldNameFrom(parentName: String): String {
            val suffix = ".field"
            val length = parentName.length + suffix.length
            return buildString(length) {
                append(parentName)
                append(suffix)
            }
        }
    }

    override val type: KSTypeReference by lazy {
        (
            kaBackingFieldSymbol.psiIfSource() as? KtBackingField
            )
            ?.typeReference?.let {
                KSTypeReferenceImpl.getCached(it, this)
            } ?: KSTypeReferenceResolvedImpl.getCached(
            kaBackingFieldSymbol.returnType.abbreviationOrSelf,
            this
        )
    }

    override val property: KSPropertyDeclaration by lazy {
        KSPropertyDeclarationImpl.getCached(kaBackingFieldSymbol.owningProperty)
    }

    override val qualifiedName: KSName? by lazy {
        // N.B.: kaBackingFieldSymbol.callableId is always null so we use the fqn of the property instead
        kaBackingFieldSymbol.owningProperty.callableId?.asSingleFqName()?.asString()?.let { propName ->
            KSNameImpl.getCached(getFieldNameFrom(propName))
        }
    }

    override val annotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        originalAnnotations
    }

    override val originalAnnotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        kaBackingFieldSymbol.annotations.asSequence()
            .map { KSAnnotationResolvedImpl.getCached(it, this, definitionOrigin) }
    }

    override val location by lazy {
        // N.B.: Use psi?.toLocation() instead of psi.toLocation() even though toLocation handles a nullable receiver.
        //       This is done to override its nullable receiver case and fall back to the property location.
        kaBackingFieldSymbol.psi?.toLocation() ?: property.location
    }

    override fun defer(): Restorable =
        kaBackingFieldSymbol.defer(::getCached)

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R =
        visitor.visitBackingField(this, data)
}
