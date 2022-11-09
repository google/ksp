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
package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.toKSModifiers
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.psi.KtModifierListOwner

abstract class AbstractKSDeclarationImpl(val ktDeclarationSymbol: KtDeclarationSymbol) : KSDeclaration {
    override val origin: Origin = mapAAOrigin(ktDeclarationSymbol.origin)

    override val location: Location by lazy {
        ktDeclarationSymbol.psi.toLocation()
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached((ktDeclarationSymbol as? KtNamedSymbol)?.name?.asString() ?: "")
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        originalAnnotations
    }

    override val modifiers: Set<Modifier> by lazy {
        when (val psi = ktDeclarationSymbol.psi) {
            is KtModifierListOwner -> psi.toKSModifiers()
            is PsiModifierListOwner -> psi.toKSModifiers()
            null -> when (ktDeclarationSymbol) {
                is KtPropertySymbol -> ktDeclarationSymbol.toModifiers()
                is KtClassOrObjectSymbol -> ktDeclarationSymbol.toModifiers()
                is KtFunctionLikeSymbol -> ktDeclarationSymbol.toModifiers()
                else -> throw IllegalStateException("Unexpected symbol type ${ktDeclarationSymbol.javaClass}")
            }
            else -> emptySet()
        }
    }

    override val containingFile: KSFile? by lazy {
        ktDeclarationSymbol.toContainingFile()
    }

    override val packageName: KSName by lazy {
        ((containingFile?.packageName ?: ktDeclarationSymbol.getContainingKSSymbol()?.packageName)?.asString() ?: "")
            .let { KSNameImpl.getCached(it) }
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        ktDeclarationSymbol.typeParameters.map { KSTypeParameterImpl.getCached(it) }
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        parent as? KSDeclaration
    }

    override val parent: KSNode? by lazy {
        analyze {
            ktDeclarationSymbol.getContainingSymbol()?.let {
                KSClassDeclarationImpl.getCached(it as KtNamedClassOrObjectSymbol)
            } ?: ktDeclarationSymbol.toContainingFile()
        }
    }

    override fun toString(): String {
        return simpleName.asString()
    }

    override val docString: String?
        get() = ktDeclarationSymbol.toDocString()

    internal val originalAnnotations = ktDeclarationSymbol.annotations()
}
