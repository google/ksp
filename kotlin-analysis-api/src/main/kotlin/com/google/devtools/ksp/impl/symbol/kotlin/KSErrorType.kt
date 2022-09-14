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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Nullability

object KSErrorType : KSType {
    override val declaration: KSDeclaration
        get() = KSErrorTypeClassDeclaration

    override val nullability: Nullability
        get() = Nullability.NULLABLE

    override val arguments: List<KSTypeArgument>
        get() = emptyList()

    override val annotations: Sequence<KSAnnotation>
        get() = emptySequence()

    override fun isAssignableFrom(that: KSType): Boolean = false

    override fun isMutabilityFlexible(): Boolean = false

    override fun isCovarianceFlexible(): Boolean = false

    override fun replace(arguments: List<KSTypeArgument>): KSType = this

    override fun starProjection(): KSType = this

    override fun makeNullable(): KSType = this

    override fun makeNotNullable(): KSType = this

    override val isMarkedNullable: Boolean = false

    override val isError: Boolean = true

    override val isFunctionType: Boolean = false

    override val isSuspendFunctionType: Boolean = false

    override fun toString(): String {
        return "<ERROR TYPE>"
    }
}
