/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.toKSModifiers
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor

abstract class KSPropertyAccessorImpl(val ktPropertyAccessor: KtPropertyAccessor) : KSPropertyAccessor {
    override val receiver: KSPropertyDeclaration by lazy {
        KSPropertyDeclarationImpl.getCached(ktPropertyAccessor.property as KtProperty)
    }
    override val annotations: List<KSAnnotation> by lazy {
        ktPropertyAccessor.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val location: Location by lazy {
        ktPropertyAccessor.toLocation()
    }

    override val modifiers: Set<Modifier> by lazy {
        ktPropertyAccessor.toKSModifiers()
    }

    override val origin: Origin = Origin.KOTLIN

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyAccessor(this, data)
    }
}