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
import com.google.devtools.ksp.getDocString
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.toKSModifiers
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KSPropertyDeclarationImpl private constructor(
    private val ktPropertySymbol: KtPropertySymbol
) : KSPropertyDeclaration {
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
        ktPropertySymbol.receiverType?.let { KSTypeReferenceImpl(it) }
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl(ktPropertySymbol.returnType)
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

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktPropertySymbol.name.asString())
    }

    override val qualifiedName: KSName? by lazy {
        (ktPropertySymbol.psi as? KtProperty)?.fqName?.asString()?.let { KSNameImpl.getCached(it) }
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        ktPropertySymbol.typeParameters.map { KSTypeParameterImpl.getCached(it) }
    }

    override val packageName: KSName by lazy {
        KSNameImpl.getCached(this.containingFile?.packageName?.asString() ?: "")
    }

    override val parentDeclaration: KSDeclaration?
        get() = TODO("Not yet implemented")

    override val containingFile: KSFile? by lazy {
        ktPropertySymbol.toContainingFile()
    }

    override val docString: String? by lazy {
        ktPropertySymbol.psi?.getDocString()
    }

    override val modifiers: Set<Modifier> by lazy {
        ktPropertySymbol.psi?.safeAs<KtProperty>()?.toKSModifiers() ?: emptySet()
    }

    override val origin: Origin by lazy {
        mapAAOrigin(ktPropertySymbol.origin)
    }

    override val location: Location by lazy {
        ktPropertySymbol.psi?.toLocation() ?: NonExistLocation
    }

    override val parent: KSNode? by lazy {
        analyze {
            ktPropertySymbol.getContainingSymbol()?.let {
                KSClassDeclarationImpl.getCached(it as KtNamedClassOrObjectSymbol)
            } ?: ktPropertySymbol.toContainingFile()
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktPropertySymbol.annotations.asSequence().map { KSAnnotationImpl.getCached(it) }
    }

    override val isActual: Boolean
        get() = TODO("Not yet implemented")

    override val isExpect: Boolean
        get() = TODO("Not yet implemented")

    override fun findActuals(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }

    override fun findExpects(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }
}
