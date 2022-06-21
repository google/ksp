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
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.descriptors.Modality

class KSFunctionDeclarationImpl private constructor(private val ktFunctionSymbol: KtFunctionLikeSymbol) :
    KSFunctionDeclaration,
    AbstractKSDeclarationImpl(ktFunctionSymbol),
    KSExpectActual by KSExpectActualImpl(ktFunctionSymbol) {
    companion object : KSObjectCache<KtFunctionLikeSymbol, KSFunctionDeclarationImpl>() {
        fun getCached(ktFunctionSymbol: KtFunctionLikeSymbol) =
            cache.getOrPut(ktFunctionSymbol) { KSFunctionDeclarationImpl(ktFunctionSymbol) }
    }

    override val functionKind: FunctionKind by lazy {
        when (ktFunctionSymbol.symbolKind) {
            KtSymbolKind.CLASS_MEMBER -> FunctionKind.MEMBER
            KtSymbolKind.TOP_LEVEL -> FunctionKind.TOP_LEVEL
            KtSymbolKind.SAM_CONSTRUCTOR -> FunctionKind.LAMBDA
            else -> throw IllegalStateException("Unexpected symbol kind ${ktFunctionSymbol.symbolKind}")
        }
    }

    override val isAbstract: Boolean by lazy {
        (ktFunctionSymbol as? KtFunctionSymbol)?.modality == Modality.ABSTRACT
    }

    override val extensionReceiver: KSTypeReference? by lazy {
        analyze {
            if (!ktFunctionSymbol.isExtension) {
                null
            } else {
                ktFunctionSymbol.receiverType?.let { KSTypeReferenceImpl(it) }
            }
        }
    }

    override val returnType: KSTypeReference? by lazy {
        analyze {
            KSTypeReferenceImpl(ktFunctionSymbol.returnType)
        }
    }

    override val parameters: List<KSValueParameter> by lazy {
        ktFunctionSymbol.valueParameters.map { KSValueParameterImpl.getCached(it) }
    }

    override fun findOverridee(): KSDeclaration? {
        TODO("Not yet implemented")
    }

    override fun asMemberOf(containing: KSType): KSFunction {
        TODO("Not yet implemented")
    }

    override val simpleName: KSName by lazy {
        if (ktFunctionSymbol is KtFunctionSymbol) {
            KSNameImpl.getCached(ktFunctionSymbol.name.asString())
        } else {
            KSNameImpl.getCached("<init>")
        }
    }

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached("${parentDeclaration?.qualifiedName?.asString()}.${this.simpleName.asString()}")
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFunctionDeclaration(this, data)
    }

    // TODO: Implement with PSI
    override val declarations: Sequence<KSDeclaration> = emptySequence()

    override fun toString(): String {
        // TODO: fix origin for implicit Java constructor in AA
        // TODO: should we change the toString() behavior for synthetic constructors?
        return if (origin == Origin.SYNTHETIC || (origin == Origin.JAVA && ktFunctionSymbol.psi == null)) {
            "synthetic constructor for ${this.parentDeclaration}"
        } else {
            this.simpleName.asString()
        }
    }
}
