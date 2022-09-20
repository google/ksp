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
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol

class KSPropertyDeclarationImpl private constructor(private val ktPropertySymbol: KtPropertySymbol) :
    KSPropertyDeclaration,
    AbstractKSDeclarationImpl(ktPropertySymbol),
    KSExpectActual by KSExpectActualImpl(ktPropertySymbol) {
    companion object : KSObjectCache<KtPropertySymbol, KSPropertyDeclarationImpl>() {
        fun getCached(ktPropertySymbol: KtPropertySymbol) =
            cache.getOrPut(ktPropertySymbol) { KSPropertyDeclarationImpl(ktPropertySymbol) }
    }

    override val getter: KSPropertyGetter? by lazy {
        ktPropertySymbol.getter?.let { KSPropertyGetterImpl.getCached(this, it) }
    }

    override val setter: KSPropertySetter? by lazy {
        ktPropertySymbol.setter?.let { KSPropertySetterImpl.getCached(this, it) }
    }

    override val extensionReceiver: KSTypeReference? by lazy {
        ktPropertySymbol.receiverType?.let { KSTypeReferenceImpl.getCached(it, this@KSPropertyDeclarationImpl) }
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktPropertySymbol.returnType, this@KSPropertyDeclarationImpl)
    }

    override val isMutable: Boolean by lazy {
        !ktPropertySymbol.isVal
    }

    override val hasBackingField: Boolean by lazy {
        ktPropertySymbol.hasBackingField
    }

    override fun isDelegated(): Boolean {
        return ktPropertySymbol.isDelegatedProperty
    }

    override fun findOverridee(): KSPropertyDeclaration? {
        TODO("Not yet implemented")
    }

    override fun asMemberOf(containing: KSType): KSType {
        TODO("Not yet implemented")
    }

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached("${parentDeclaration?.qualifiedName?.asString()}.${this.simpleName.asString()}")
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }
}
