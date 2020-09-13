/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.synthetic

import com.google.devtools.kotlin.symbol.processing.symbol.*

abstract class KSPropertyAccessorSyntheticImpl(ksPropertyDeclaration: KSPropertyDeclaration) : KSPropertyAccessor {
    override val annotations: List<KSAnnotation> = emptyList()

    override val location: Location by lazy {
        ksPropertyDeclaration.location
    }

    override val modifiers: Set<Modifier> = emptySet()

    override val origin: Origin = Origin.SYNTHETIC

    override val receiver: KSPropertyDeclaration = ksPropertyDeclaration

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyAccessor(this, data)
    }
}