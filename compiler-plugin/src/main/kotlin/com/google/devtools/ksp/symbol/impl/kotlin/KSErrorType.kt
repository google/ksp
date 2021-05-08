/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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


package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.synthetic.KSErrorTypeClassDeclaration

object KSErrorType : KSType {
    override val annotations: Sequence<KSAnnotation> = emptySequence()

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

    override val isMarkedNullable: Boolean = false

    override fun replace(arguments: List<KSTypeArgument>): KSType {
        return this
    }

    override fun starProjection(): KSType {
        return this
    }

    override fun toString(): String {
        return "<ERROR TYPE>"
    }

    override val isFunctionType: Boolean = false

    override val isSuspendFunctionType: Boolean = false
}
