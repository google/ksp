/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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
 * A function definition
 *
 * Dispatch receiver can be obtained through [parentDeclaration].
 */
interface KSFunctionDeclaration : KSDeclaration, KSDeclarationContainer {
    /**
     * Kind of this function.
     */
    val functionKind: FunctionKind

    /**
     * Whether this function is abstract.
     */
    val isAbstract: Boolean

    /**
     * Extension receiver of this function
     * @see [https://kotlinlang.org/docs/reference/extensions.html#extension-functions]
     */
    val extensionReceiver: KSTypeReference?

    /**
     * Return type of this function.
     * Can be null if an error occurred during resolution.
     */
    val returnType: KSTypeReference?

    /**
     * [variable parameters][KSVariableParameter] of this function.
     */
    val parameters: List<KSVariableParameter>

    /**
     * Checks if this function overrides another function.
     * @param overridee the candidate overridden function being checked.
     * @return boolean value indicating whether this function overrides [overridee]
     * Calling [overrides] is expensive and should be avoided if possible.
     */
    fun overrides(overridee: KSFunctionDeclaration): Boolean

    /**
     * Find the original overridee of this function, if overriding.
     * @return [KSFunctionDeclaration] for the original function, if overriding, otherwise null.
     * Calling [findOverridee] is expensive and should be avoided if possible.
     */
    fun findOverridee(): KSFunctionDeclaration?
}