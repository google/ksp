/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.findParentDeclaration
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.*

class KSTypeAliasImpl private constructor(val ktTypeAlias: KtTypeAlias) : KSTypeAlias, KSDeclarationImpl(ktTypeAlias),
    KSExpectActual by KSExpectActualImpl(ktTypeAlias) {
    companion object : KSObjectCache<KtTypeAlias, KSTypeAliasImpl>() {
        fun getCached(ktTypeAlias: KtTypeAlias) = cache.getOrPut(ktTypeAlias) { KSTypeAliasImpl(ktTypeAlias) }
    }

    override val name: KSName by lazy {
        KSNameImpl.getCached(ktTypeAlias.name!!)
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktTypeAlias.getTypeReference()!!)
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeAlias(this, data)
    }
}

