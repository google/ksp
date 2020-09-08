/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.findParentDeclaration
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.*

abstract class KSDeclarationImpl(ktDeclaration: KtDeclaration) : KSDeclaration {
    override val origin: Origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktDeclaration.toLocation()
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktDeclaration.name!!)
    }

    override val qualifiedName: KSName? by lazy {
        (ktDeclaration as? KtNamedDeclaration)?.fqName?.let { KSNameImpl.getCached(it.asString()) }
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktDeclaration.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        ktDeclaration.toKSModifiers()
    }
    override val containingFile: KSFile? by lazy {
        KSFileImpl.getCached(ktDeclaration.containingKtFile)
    }

    override val packageName: KSName by lazy {
        this.containingFile!!.packageName
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        (ktDeclaration as? KtTypeParameterListOwner)?.let {
            it.typeParameters.map { KSTypeParameterImpl.getCached(it, ktDeclaration) }
        } ?: emptyList()
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        ktDeclaration.findParentDeclaration()
    }

    override fun toString(): String {
        return this.simpleName.asString()
    }
}