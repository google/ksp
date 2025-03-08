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

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.lazyMemoizedSequence
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSAnnotationResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.impl.symbol.util.toKSModifiers
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor

abstract class KSPropertyAccessorImpl(
    internal val ktPropertyAccessorSymbol: KaPropertyAccessorSymbol,
    override val receiver: KSPropertyDeclaration
) : KSPropertyAccessor, Deferrable {

    override val annotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        // (ktPropertyAccessorSymbol.psi as? KtPropertyAccessor)?.annotations(ktPropertyAccessorSymbol, this) ?:
        ktPropertyAccessorSymbol.annotations.asSequence()
            .filter { it.useSiteTarget != AnnotationUseSiteTarget.SETTER_PARAMETER }
            .map { KSAnnotationResolvedImpl.getCached(it, this) }
            .plus(findAnnotationFromUseSiteTarget())
    }

    internal val originalAnnotations: Sequence<KSAnnotation> by lazyMemoizedSequence {
        // (ktPropertyAccessorSymbol.psi as? KtPropertyAccessor)?.annotations(ktPropertyAccessorSymbol, this) ?:
        ktPropertyAccessorSymbol.annotations(this)
    }

    override val location: Location by lazy {
        ktPropertyAccessorSymbol.psi?.toLocation() ?: NonExistLocation
    }

    override val modifiers: Set<Modifier> by lazy {
        (
            if (origin == Origin.JAVA_LIB || origin == Origin.KOTLIN_LIB || origin == Origin.SYNTHETIC) {
                (ktPropertyAccessorSymbol.toModifiers())
            } else {
                (ktPropertyAccessorSymbol.psi as? KtModifierListOwner)?.toKSModifiers() ?: emptySet()
            }
            ).let {
            if (origin == Origin.SYNTHETIC &&
                (receiver.parentDeclaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE
            ) {
                it + Modifier.ABSTRACT
            } else {
                it
            }
        }
    }

    override val origin: Origin by lazy {
        val symbolOrigin = mapAAOrigin(ktPropertyAccessorSymbol)
        if (symbolOrigin == Origin.KOTLIN && ktPropertyAccessorSymbol.psi == null) {
            Origin.SYNTHETIC
        } else {
            symbolOrigin
        }
    }

    override val parent: KSNode?
        get() = ktPropertyAccessorSymbol.getContainingKSSymbol()

    override val declarations: Sequence<KSDeclaration> by lazyMemoizedSequence {
        val psi = ktPropertyAccessorSymbol.psi as? KtPropertyAccessor ?: return@lazyMemoizedSequence emptySequence()
        if (!psi.hasBlockBody()) {
            emptySequence()
        } else {
            psi.bodyBlockExpression?.statements?.asSequence()?.filterIsInstance<KtDeclaration>()?.mapNotNull {
                analyze {
                    it.symbol.toKSDeclaration()
                }
            } ?: emptySequence()
        }
    }
}

class KSPropertySetterImpl private constructor(
    owner: KSPropertyDeclaration,
    setter: KaPropertySetterSymbol
) : KSPropertyAccessorImpl(setter, owner), KSPropertySetter {
    companion object : KSObjectCache<Pair<KSPropertyDeclaration, KaPropertySetterSymbol>, KSPropertySetterImpl>() {
        fun getCached(owner: KSPropertyDeclaration, setter: KaPropertySetterSymbol) =
            cache.getOrPut(Pair(owner, setter)) { KSPropertySetterImpl(owner, setter) }
    }

    override val parameter: KSValueParameter by lazy {
        KSValueParameterImpl.getCached(setter.parameter, this)
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertySetter(this, data)
    }

    override fun toString(): String {
        return "$receiver.setter()"
    }

    override fun defer(): Restorable? {
        val other = (receiver as Deferrable).defer() ?: return null
        return ktPropertyAccessorSymbol.defer inner@{
            val owner = other.restore() ?: return@inner null
            getCached(owner as KSPropertyDeclaration, it as KaPropertySetterSymbol)
        }
    }
}

class KSPropertyGetterImpl private constructor(
    owner: KSPropertyDeclaration,
    getter: KaPropertyGetterSymbol
) : KSPropertyAccessorImpl(getter, owner), KSPropertyGetter {
    companion object : KSObjectCache<Pair<KSPropertyDeclaration, KaPropertyGetterSymbol>, KSPropertyGetterImpl>() {
        fun getCached(owner: KSPropertyDeclaration, getter: KaPropertyGetterSymbol) =
            cache.getOrPut(Pair(owner, getter)) { KSPropertyGetterImpl(owner, getter) }
    }

    override val returnType: KSTypeReference? by lazy {
        ((owner as? KSPropertyDeclarationImpl)?.ktPropertySymbol?.psiIfSource() as? KtProperty)?.typeReference
            ?.let { KSTypeReferenceImpl.getCached(it, this) }
            ?: KSTypeReferenceResolvedImpl.getCached(getter.returnType.abbreviationOrSelf, this@KSPropertyGetterImpl)
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyGetter(this, data)
    }

    override fun toString(): String {
        return "$receiver.getter()"
    }

    override fun defer(): Restorable? {
        val other = (receiver as Deferrable).defer() ?: return null
        return ktPropertyAccessorSymbol.defer inner@{
            val owner = other.restore() ?: return@inner null
            getCached(owner as KSPropertyDeclaration, it as KaPropertyGetterSymbol)
        }
    }
}
