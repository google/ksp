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
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.toKSModifiers
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KSTypeAliasImpl private constructor(private val ktTypeAliasSymbol: KtTypeAliasSymbol): KSTypeAlias {
    companion object : KSObjectCache<KtTypeAliasSymbol, KSTypeAliasImpl>() {
       fun getCached(ktTypeAliasSymbol: KtTypeAliasSymbol) =
            cache.getOrPut(ktTypeAliasSymbol) { KSTypeAliasImpl(ktTypeAliasSymbol) }
    }

    override val name: KSName by lazy {
        KSNameImpl.getCached(ktTypeAliasSymbol.nameOrAnonymous.asString())
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktTypeAliasSymbol.expandedType)
    }

    override val simpleName: KSName
        get() = name

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached(ktTypeAliasSymbol.psi?.safeAs<KtTypeAlias>()?.fqName?.asString() ?: "")
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        ktTypeAliasSymbol.typeParameters.map { KSTypeParameterImpl.getCached(it) }
    }

    override val packageName: KSName
        get() = KSNameImpl.getCached(this.containingFile?.packageName?.asString() ?: "")

    override val parentDeclaration: KSDeclaration?
        get() = TODO("Not yet implemented")

    override val containingFile: KSFile? by lazy {
        ktTypeAliasSymbol.toContainingFile()
    }

    override val docString: String? by lazy {
        ktTypeAliasSymbol.toDocString()
    }

    override val modifiers: Set<Modifier> by lazy {
        ktTypeAliasSymbol.psi?.safeAs<KtTypeAlias>()?.toKSModifiers() ?: emptySet()
    }

    override val origin: Origin by lazy {
        mapAAOrigin(ktTypeAliasSymbol.origin)
    }

    override val location: Location by lazy {
        ktTypeAliasSymbol.psi.toLocation()
    }

    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeAlias(this, data)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktTypeAliasSymbol.annotations()
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
