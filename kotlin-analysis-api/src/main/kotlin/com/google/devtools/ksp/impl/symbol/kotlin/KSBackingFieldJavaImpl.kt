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
import com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationJavaImpl.Companion.getCached
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.KSBackingField
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol

class KSBackingFieldJavaImpl private constructor(
    val ktJavaFieldSymbol: KaJavaFieldSymbol,
    override val property: KSPropertyDeclaration
) : KSBackingField, AbstractKSDeclarationImpl() {

    companion object : KSObjectCache<Pair<KaJavaFieldSymbol, KSPropertyDeclarationJavaImpl>, KSBackingFieldJavaImpl>() {
        fun getCached(symbolAndProperty: Pair<KaJavaFieldSymbol, KSPropertyDeclarationJavaImpl>) =
            cache.getOrPut(symbolAndProperty) {
                KSBackingFieldJavaImpl(symbolAndProperty.first, symbolAndProperty.second)
            }

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

    override val ktDeclarationSymbol: KaDeclarationSymbol
        get() = ktJavaFieldSymbol

    override val type: KSTypeReference by lazy {
        KSTypeReferenceResolvedImpl.getCached(ktJavaFieldSymbol.returnType, this)
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached("field")
    }

    override val qualifiedName: KSName? by lazy {
        ktJavaFieldSymbol.callableId?.asSingleFqName()?.asString()?.let { propName ->
            KSNameImpl.getCached(getFieldNameFrom(propName))
        }
    }

    // Manual delegation for KSExpectActual to avoid eager evaluation in the class header
    private val expectActualImpl by lazy { KSExpectActualImpl(ktJavaFieldSymbol) }
    override val isActual: Boolean get() = expectActualImpl.isActual
    override val isExpect: Boolean get() = expectActualImpl.isExpect
    override fun findActuals(): Sequence<KSDeclaration> = expectActualImpl.findActuals()
    override fun findExpects(): Sequence<KSDeclaration> = expectActualImpl.findExpects()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R =
        visitor.visitBackingField(this, data)

    override fun defer(): Restorable = ktJavaFieldSymbol.defer(::getCached)
}
