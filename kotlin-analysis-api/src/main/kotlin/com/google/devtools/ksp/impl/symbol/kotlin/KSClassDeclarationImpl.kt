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

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.KtObjectDeclaration

class KSClassDeclarationImpl private constructor(private val ktNamedClassOrObjectSymbol: KtNamedClassOrObjectSymbol) :
    KSClassDeclaration,
    AbstractKSDeclarationImpl(ktNamedClassOrObjectSymbol),
    KSExpectActual by KSExpectActualImpl(ktNamedClassOrObjectSymbol) {
    companion object : KSObjectCache<KtNamedClassOrObjectSymbol, KSClassDeclarationImpl>() {
        fun getCached(ktNamedClassOrObjectSymbol: KtNamedClassOrObjectSymbol) =
            cache.getOrPut(ktNamedClassOrObjectSymbol) { KSClassDeclarationImpl(ktNamedClassOrObjectSymbol) }
    }

    override val qualifiedName: KSName? by lazy {
        ktNamedClassOrObjectSymbol.classIdIfNonLocal?.asFqNameString()?.let { KSNameImpl.getCached(it) }
    }

    override val classKind: ClassKind by lazy {
        when (ktNamedClassOrObjectSymbol.classKind) {
            KtClassKind.CLASS -> ClassKind.CLASS
            KtClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
            KtClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
            KtClassKind.INTERFACE -> ClassKind.INTERFACE
            KtClassKind.COMPANION_OBJECT, KtClassKind.ANONYMOUS_OBJECT, KtClassKind.OBJECT -> ClassKind.OBJECT
            KtClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
        }
    }

    override val primaryConstructor: KSFunctionDeclaration? by lazy {
        analyze {
            ktNamedClassOrObjectSymbol.getMemberScope().getConstructors().singleOrNull { it.isPrimary }?.let {
                KSFunctionDeclarationImpl.getCached(it)
            }
        }
    }

    override val superTypes: Sequence<KSTypeReference> by lazy {
        analyze {
            ktNamedClassOrObjectSymbol.superTypes.map { KSTypeReferenceImpl(it) }.asSequence()
        }
    }

    override val isCompanionObject: Boolean by lazy {
        (ktNamedClassOrObjectSymbol.psi as? KtObjectDeclaration)?.isCompanion() ?: false
    }

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> {
        return ktNamedClassOrObjectSymbol.getAllFunctions()
    }

    override fun getAllProperties(): Sequence<KSPropertyDeclaration> {
        return ktNamedClassOrObjectSymbol.getAllProperties()
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        TODO("Not yet implemented")
    }

    override fun asStarProjectedType(): KSType {
        return analyze {
            KSTypeImpl.getCached(analysisSession.buildClassType(ktNamedClassOrObjectSymbol))
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }

    override val declarations: Sequence<KSDeclaration> by lazy {
        ktNamedClassOrObjectSymbol.declarations()
    }
}
