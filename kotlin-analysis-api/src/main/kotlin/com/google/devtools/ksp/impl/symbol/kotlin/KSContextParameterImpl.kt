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
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSContextParameter
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.KSVisitorNext
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Implementation of [KSContextParameter] backed by Analysis API's [KaContextParameterSymbol].
 *
 * The [parent] property is implemented as a constructor parameter because there is no easy way
 * to obtain the parent from the [KaContextParameterSymbol], but it always originates from a
 * function or property declaration which serves as the parent.
 *
 * @param kaContextParameterSymbol The underlying Analysis API [KaContextParameterSymbol].
 * @param parent The parent declaration (such as a function or property declaration) where this
 * context parameter is declared.
 */
@OptIn(KaExperimentalApi::class)
class KSContextParameterImpl private constructor(
    val kaContextParameterSymbol: KaContextParameterSymbol,
    override val parent: KSDeclaration,
) : KSContextParameter, Deferrable {

    companion object : KSObjectCache<Pair<KaContextParameterSymbol, KSDeclaration>, KSContextParameterImpl>() {

        /**
         * Returns a cached instance of [KSContextParameterImpl] for the given [KaContextParameterSymbol]
         * and [parent] node, creating one if it does not already exist in the cache.
         *
         * N.B.: The cache key must use *structural* equality on [kaContextParameterSymbol], not reference
         * identity. The Analysis API builds a brand new [KaContextParameterSymbol] on every access
         * (`KaFirSymbolProvider.KtParameter.symbol` and `KaCallableSymbol.contextParameters` are plain
         * getters without interning), so the same source context parameter is reached through several
         * producers with different instances:
         *  - [KSFunctionDeclarationImpl.contextParameters] / [KSPropertyDeclarationImpl.contextParameters],
         *  - `KaContextParameterSymbol.toKSContextParameter` (used by `PsiResolutionStrategy`), and
         *  - [defer]/[Restorable.restore] via a `KaSymbolPointer`.
         * An identity-based key would hand out a distinct [KSContextParameterImpl] per producer, which
         * breaks the invariant that a KS node is canonical for its underlying declaration and defeats
         * the `Set`-based deduplication in the annotation resolution strategies.
         * [KaContextParameterSymbol] implements structural `equals`/`hashCode`, so a plain [Pair] is
         * sufficient; [parent] is already canonical because the declaration caches are structural too.
         *
         * @param kaContextParameterSymbol The Analysis API [KaContextParameterSymbol].
         * @param parent The parent declaration (such as a function or property declaration) where this
         * context parameter is declared.
         * @return The cached or newly created [KSContextParameterImpl] instance.
         */
        fun getCached(kaContextParameterSymbol: KaContextParameterSymbol, parent: KSDeclaration) =
            cache.getOrPut(kaContextParameterSymbol to parent) {
                KSContextParameterImpl(
                    kaContextParameterSymbol,
                    parent
                )
            }
    }

    /**
     * The [KSName] for this [KSContextParameter].
     *
     * [name] is null when context parameters are unnamed such as `context(_: T)` or legacy context receivers `context(T)`.
     */
    override val name: KSName? by lazy {
        // N.B.: it.isSpecial denotes an unused/unnamed var.
        kaContextParameterSymbol.name.takeUnless { it.isSpecial }?.let { KSNameImpl.getCached(it.asString()) }
    }

    override val type: KSTypeReference by lazy {
        // Try to get the PSI type reference to avoid doing anything expensive but fall back to using AA.
        // N.B.: the two supported syntaxes are backed by different PSI, and expose the type
        // reference differently: `KtParameter.typeReference` is a property whereas
        // `KtContextReceiver.typeReference()` is a function.
        // Additionally, the type reference is stored in a variable since all three branches
        // of the when-expression are nullable, so it's more readable to just do a single null check later.
        val psiTypeReference = when (val psi = kaContextParameterSymbol.psiIfSource()) {
            // A context parameter, `context(name: Type)`, is backed by a KtParameter.
            is KtParameter -> psi.typeReference
            // A context receiver, `context(Type)`, is backed by a KtContextReceiver. This syntax is
            // deprecated in favor of context parameters, but is still resolvable.
            is KtContextReceiver -> psi.typeReference()
            else -> null
        }

        if (psiTypeReference != null) {
            KSTypeReferenceImpl.getCached(ktTypeReference = psiTypeReference, parent = this)
        } else {
            // Use abbreviationOrSelf to preserve type aliases.
            KSTypeReferenceResolvedImpl.getCached(
                type = kaContextParameterSymbol.returnType.abbreviationOrSelf,
                parent = this
            )
        }
    }

    override val annotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        kaContextParameterSymbol.annotations(this)
    }

    override val origin: Origin by lazy {
        mapAAOrigin(kaContextParameterSymbol)
    }

    override val location: Location by lazy {
        kaContextParameterSymbol.psi.toLocation()
    }

    override fun toString(): String =
        name?.asString() ?: "_"

    override fun defer(): Restorable? {
        val other = (parent as? Deferrable)?.defer() ?: return null
        return kaContextParameterSymbol.defer inner@{
            getCached(it, other.restore() as? KSDeclaration ?: return@inner null)
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = when (visitor) {
        is KSVisitorNext -> visitor.visitContextParameter(this, data)
        else -> visitor.visitAnnotated(this, data)
    }
}
