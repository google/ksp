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


package com.google.devtools.ksp.symbol

/**
 * Represents a type in Kotlin's type system.
 *
 * Generally, a type is comprised of a declaration (e.g., class), corresponding type arguments, and other details like nullability.
 * KSType is useful when doing type checking, finding the declaration, and so on. Some of the information,
 * such as type annotations and type arguments, are often available in the corresponding type reference without resolution.
 */
interface KSType {
    /**
     * The declaration that generates this type.
     */
    val declaration: KSDeclaration

    /**
     * A type can be nullable, not nullable, or context-specific in the case of platform types.
     */
    val nullability: Nullability

    /**
     * Type arguments to the type.
     */
    val arguments: List<KSTypeArgument>

    /**
     * Type annotations to the type.
     */
    val annotations: List<KSAnnotation>

    /**
     * Check whether this type is assign-compatible from another type.
     *
     * @param: that the other type being checked.
     */
    fun isAssignableFrom(that: KSType): Boolean

    /**
     * True if the type is a collection and can be both mutable and immutable, depending on the context.
     */
    fun isMutabilityFlexible(): Boolean

    /**
     * True if the type can be both invariant and covariant, depending on the context.
     */
    fun isCovarianceFlexible(): Boolean

    /**
     * Replace the type arguments
     *
     * @param arguemnts New type arguments
     * @return A type with the arguments replaced.
     */
    fun replace(arguments: List<KSTypeArgument>): KSType

    /**
     * Returns the star projection of the type.
     */
    fun starProjection(): KSType

    /**
     * Make the type nullable
     */
    fun makeNullable(): KSType

    /**
     * Make the type not nullable
     */
    fun makeNotNullable(): KSType

    /**
     * True if the type is an error type, which means the type can't be resolved by compiler.
     */
    val isError: Boolean
}