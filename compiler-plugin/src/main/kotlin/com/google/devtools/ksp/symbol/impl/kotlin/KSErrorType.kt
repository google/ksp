/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.synthetic.KSErrorTypeClassDeclaration

object KSErrorType : KSType {
    override val annotations: List<KSAnnotation> = emptyList()

    override val arguments: List<KSTypeArgument> = emptyList()

    override val declaration: KSDeclaration = KSErrorTypeClassDeclaration

    override val isError: Boolean = true

    override val nullability: Nullability = Nullability.NULLABLE

    override fun isAssignableFrom(that: KSType): Boolean {
        return false
    }

    override fun isCovarianceFlexible(): Boolean {
        return false
    }

    override fun isMutabilityFlexible(): Boolean {
        return false
    }

    override fun makeNotNullable(): KSType {
        return this
    }

    override fun makeNullable(): KSType {
        return this
    }

    override fun replace(arguments: List<KSTypeArgument>): KSType {
        return this
    }

    override fun starProjection(): KSType {
        return this
    }

    override fun toString(): String {
        return "<ERROR TYPE>"
    }
}