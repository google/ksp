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
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.error.ErrorTypeKind

class KSErrorType(
    val nameHint: String?,
) : KSType {
    constructor(name: String, message: String?) : this(
        nameHint = listOfNotNull(name, message).takeIf { it.isNotEmpty() }?.joinToString(" % ")
    )

    override val annotations: Sequence<KSAnnotation>
        get() = emptySequence()

    override val arguments: List<KSTypeArgument>
        get() = emptyList()

    override val declaration: KSDeclaration
        get() = KSErrorTypeClassDeclaration(this)

    override val isError: Boolean
        get() = true

    override val nullability: Nullability
        get() = Nullability.NULLABLE

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

    override val isMarkedNullable: Boolean
        get() = false

    override fun replace(arguments: List<KSTypeArgument>): KSType {
        return this
    }

    override fun starProjection(): KSType {
        return this
    }

    override fun toString(): String = nameHint?.let { "<ERROR TYPE: $it>" } ?: "<ERROR TYPE>"

    override val isFunctionType: Boolean
        get() = false

    override val isSuspendFunctionType: Boolean
        get() = false

    override fun hashCode() = nameHint.hashCode()

    override fun equals(other: Any?): Boolean {
        return this === other || other is KSErrorType && other.nameHint == nameHint
    }

    companion object {
        fun fromReferenceBestEffort(reference: KSTypeReference?): KSErrorType {
            return when (val type = reference?.resolve()) {
                is KSErrorType -> type
                null -> KSErrorType(reference?.element?.toString())
                else -> KSErrorType(type.toString())
            }
        }

        fun fromKtErrorType(ktType: KotlinType): KSErrorType {
            // Logic is in sync with `KotlinType.isError`
            val errorType: ErrorType = when (val unwrapped = ktType.unwrap()) {
                is ErrorType -> unwrapped
                is FlexibleType -> unwrapped.delegate as? ErrorType
                else -> null
            } ?: throw IllegalArgumentException("Not an error type: $ktType")

            val hint = when (errorType.kind) {
                // Handle "Unresolved types" group
                ErrorTypeKind.UNRESOLVED_TYPE,
                ErrorTypeKind.UNRESOLVED_CLASS_TYPE,
                ErrorTypeKind.UNRESOLVED_JAVA_CLASS,
                ErrorTypeKind.UNRESOLVED_DECLARATION,
                ErrorTypeKind.UNRESOLVED_KCLASS_CONSTANT_VALUE,
                ErrorTypeKind.UNRESOLVED_TYPE_ALIAS -> errorType.formatParams.first()

                // TODO: Handle more ErrorTypeKinds where it's possible to extract a name for the error type.
                else -> errorType.debugMessage
            }

            return KSErrorType(hint)
        }
    }
}
