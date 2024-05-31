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

import com.google.devtools.ksp.symbol.*

class KSErrorType(
    private val hint: String? = null,
) : KSType {
    override val declaration: KSDeclaration
        get() = KSErrorTypeClassDeclaration(this)

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

    override fun toString(): String = hint?.let { "<ERROR TYPE: $it>" } ?: "<ERROR TYPE>"

    override fun hashCode() = hint.hashCode()

    override fun equals(other: Any?): Boolean {
        return this === other || other is KSErrorType && other.hint == hint
    }

    companion object {
        // As KSTypeImpl can also be `isError`, this function returns a KSType
        // TODO: Make return exclusively KSErrorType
        fun fromReferenceBestEffort(reference: KSTypeReference?): KSType {
            return when (val type = reference?.resolve()) {
                is KSErrorType -> type
                null -> KSErrorType(hint = reference?.element?.toString())
                else -> {
                    type.takeIf { it.isError } ?: KSErrorType(hint = type.toString())
                }
            }
        }
    }
}
