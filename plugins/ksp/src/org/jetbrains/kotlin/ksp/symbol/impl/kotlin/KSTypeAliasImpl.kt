/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.findParentDeclaration
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.psi.*

class KSTypeAliasImpl(val ktTypeAlias: KtTypeAlias) : KSTypeAlias {
    companion object {
        private val cache = mutableMapOf<KtTypeAlias, KSTypeAliasImpl>()

        fun getCached(ktTypeAlias: KtTypeAlias) = cache.getOrPut(ktTypeAlias) { KSTypeAliasImpl(ktTypeAlias) }
    }

    override val containingFile: KSFile by lazy {
        KSFileImpl.getCached(ktTypeAlias.containingKtFile)
    }

    override val name: KSName by lazy {
        KSNameImpl.getCached(ktTypeAlias.name!!)
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        ktTypeAlias.findParentDeclaration()
    }

    override val qualifiedName: KSName by lazy {
        KSNameImpl.getCached(ktTypeAlias.fqName!!.asString())
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktTypeAlias.name!!)
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktTypeAlias.getTypeReference()!!)
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        ktTypeAlias.typeParameters.map {
            KSTypeParameterImpl.getCached(
                it,
                ktTypeAlias
            )
        }
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktTypeAlias.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        ktTypeAlias.toKSModifiers()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeAlias(this, data)
    }
}

